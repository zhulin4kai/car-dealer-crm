package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.PromotionProductLine;
import com.autodealer.crm.enums.ProductPromotionStatus;
import com.autodealer.crm.enums.ProductPromotionType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductPromotionMapper;
import com.autodealer.crm.mapper.TProductPromotionUsageMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TProductPromotion;
import com.autodealer.crm.model.TProductPromotionUsage;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.ProductPromotionService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProductPromotionServiceImpl implements ProductPromotionService {

    private static final String ALL_SCOPE = "ALL";
    private static final Set<ProductPromotionStatus> MUTABLE_STATUSES = Set.of(
            ProductPromotionStatus.DRAFT,
            ProductPromotionStatus.PENDING_EFFECTIVE,
            ProductPromotionStatus.PAUSED
    );

    @Autowired
    private TProductPromotionMapper promotionMapper;

    @Autowired
    private TProductPromotionUsageMapper promotionUsageMapper;

    @Autowired
    private TProductMapper productMapper;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private OperationAuditRecorder auditRecorder;

    @Override
    public PageInfo<TProductPromotion> getPromotionList(Integer pageNum, Integer pageSize) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        if (size > 100) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        PageHelper.startPage(page, size);
        List<TProductPromotion> promotions = promotionMapper.selectList((page - 1) * size, size);
        return new PageInfo<>(promotions);
    }

    @Override
    public List<TProductPromotion> getAvailablePromotions(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctProductIds = productIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (distinctProductIds.isEmpty()) {
            return List.of();
        }
        return promotionMapper.selectAvailableByProductIds(
                distinctProductIds, LocalDateTime.now(), ALL_SCOPE, ALL_SCOPE, ALL_SCOPE);
    }

    @Override
    public TProductPromotion getPromotionById(Long id) {
        return promotionMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPromotion(TProductPromotion promotion) {
        validateForSave(promotion, null);
        LocalDateTime now = LocalDateTime.now();
        promotion.setStatus(ProductPromotionStatus.DRAFT.name());
        applyDefaults(promotion);
        promotion.setCreateTime(now);
        promotion.setUpdateTime(now);
        try {
            if (promotionMapper.insert(promotion) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "促销创建失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(CodeEnum.DUPLICATE, "促销编码已存在", ex);
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_PROMOTION_CREATE, String.valueOf(promotion.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePromotion(TProductPromotion promotion) {
        if (promotion == null || promotion.getId() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销ID不能为空");
        }
        TProductPromotion existing = requirePromotion(promotion.getId());
        ProductPromotionStatus existingStatus = ProductPromotionStatus.parse(existing.getStatus());
        if (!MUTABLE_STATUSES.contains(existingStatus)) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前促销状态不允许编辑规则");
        }
        validateForSave(promotion, existing);
        applyDefaults(promotion);
        promotion.setStatus(existing.getStatus());
        promotion.setUsedBudget(nullToZero(existing.getUsedBudget()));
        promotion.setUsedCount(existing.getUsedCount() == null ? 0 : existing.getUsedCount());
        promotion.setPauseReason(existing.getPauseReason());
        promotion.setEndReason(existing.getEndReason());
        promotion.setVoidReason(existing.getVoidReason());
        promotion.setUpdateTime(LocalDateTime.now());
        try {
            if (promotionMapper.update(promotion) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "促销更新失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(CodeEnum.DUPLICATE, "促销编码已存在", ex);
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_PROMOTION_UPDATE, String.valueOf(promotion.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePromotion(Long id) {
        TProductPromotion existing = requirePromotion(id);
        ProductPromotionStatus status = ProductPromotionStatus.parse(existing.getStatus());
        if (status != ProductPromotionStatus.DRAFT) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有草稿促销允许删除");
        }
        if (promotionMapper.countPromotionReferences(id) > 0) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "促销已被报价或订单引用，不能删除");
        }
        if (promotionMapper.deleteById(id) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "促销删除失败");
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_PROMOTION_DELETE, String.valueOf(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductPromotion publishPromotion(Long id) {
        TProductPromotion promotion = requirePromotion(id);
        assertTimeRangeUsable(promotion);
        ProductPromotionStatus target = LocalDateTime.now().isBefore(promotion.getStartTime())
                ? ProductPromotionStatus.PENDING_EFFECTIVE
                : ProductPromotionStatus.ACTIVE;
        return changeStatus(id, List.of(ProductPromotionStatus.DRAFT), target, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductPromotion activatePromotion(Long id) {
        TProductPromotion promotion = requirePromotion(id);
        assertEffectiveNow(promotion);
        assertBudgetNotExhausted(promotion);
        return changeStatus(id,
                List.of(ProductPromotionStatus.PENDING_EFFECTIVE, ProductPromotionStatus.PAUSED),
                ProductPromotionStatus.ACTIVE, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductPromotion pausePromotion(Long id, String reason) {
        String normalizedReason = requireReason(reason);
        return changeStatus(id,
                List.of(ProductPromotionStatus.PENDING_EFFECTIVE, ProductPromotionStatus.ACTIVE),
                ProductPromotionStatus.PAUSED, normalizedReason, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductPromotion endPromotion(Long id, String reason) {
        String normalizedReason = requireReason(reason);
        return changeStatus(id,
                List.of(ProductPromotionStatus.DRAFT, ProductPromotionStatus.PENDING_EFFECTIVE,
                        ProductPromotionStatus.ACTIVE, ProductPromotionStatus.PAUSED,
                        ProductPromotionStatus.EXHAUSTED),
                ProductPromotionStatus.ENDED, null, normalizedReason, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TProductPromotion voidPromotion(Long id, String reason) {
        String normalizedReason = requireReason(reason);
        return changeStatus(id,
                List.of(ProductPromotionStatus.DRAFT, ProductPromotionStatus.PENDING_EFFECTIVE,
                        ProductPromotionStatus.ACTIVE, ProductPromotionStatus.PAUSED,
                        ProductPromotionStatus.EXHAUSTED),
                ProductPromotionStatus.VOIDED, null, null, normalizedReason);
    }

    @Override
    public TProductPromotion requireApplicablePromotion(Long promotionId, List<Long> productIds) {
        if (promotionId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销ID不能为空");
        }
        TProductPromotion promotion = requirePromotion(promotionId);
        if (!ProductPromotionStatus.parse(promotion.getStatus()).usableForQuote()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销当前不可用于报价");
        }
        assertEffectiveNow(promotion);
        assertBudgetNotExhausted(promotion);
        List<Long> actualProductIds = productIds == null ? List.of() : productIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (!actualProductIds.contains(promotion.getProductId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销不适用于当前商品");
        }
        assertDefaultContextScope(promotion.getApplicableStore(), "促销门店范围不适用于当前报价上下文");
        assertDefaultContextScope(promotion.getCustomerType(), "促销客户范围不适用于当前报价上下文");
        assertDefaultContextScope(promotion.getApplicableChannel(), "促销渠道不适用于当前报价上下文");
        return promotion;
    }

    @Override
    public BigDecimal calculateDiscount(List<PromotionProductLine> lines, TProductPromotion promotion) {
        if (promotion == null) {
            return BigDecimal.ZERO;
        }
        ProductPromotionType type = ProductPromotionType.parse(promotion.getType());
        BigDecimal discountValue = promotion.getDiscount();
        if (discountValue == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销优惠值不能为空");
        }
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal matchedAmount = BigDecimal.ZERO;
        boolean matched = false;
        for (PromotionProductLine line : lines == null ? List.<PromotionProductLine>of() : lines) {
            validateLine(line);
            if (!promotion.getProductId().equals(line.getProductId())) {
                continue;
            }
            matched = true;
            BigDecimal lineAmount = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            matchedAmount = matchedAmount.add(lineAmount);
            if (type == ProductPromotionType.PERCENTAGE) {
                discountAmount = discountAmount.add(lineAmount.multiply(BigDecimal.ONE.subtract(discountValue)));
            } else if (type.monetary()) {
                discountAmount = discountAmount.add(discountValue.multiply(BigDecimal.valueOf(line.getQuantity())));
            }
        }
        if (!matched) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销不适用于当前商品");
        }
        if (type.benefitOnly()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (discountAmount.compareTo(matchedAmount) > 0) {
            discountAmount = matchedAmount;
        }
        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveUsage(Long promotionId, BigDecimal discountAmount, String sourceType, Long sourceId) {
        if (promotionId == null) {
            return;
        }
        BigDecimal actualDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount.max(BigDecimal.ZERO);
        int updated = promotionMapper.consumeBudgetAtomic(promotionId, actualDiscount, LocalDateTime.now());
        if (updated != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销预算或名额不足");
        }
        TProductPromotionUsage usage = new TProductPromotionUsage();
        usage.setPromotionId(promotionId);
        usage.setSourceType(normalizeScope(sourceType));
        usage.setSourceId(sourceId);
        usage.setDiscountAmount(actualDiscount.setScale(2, RoundingMode.HALF_UP));
        usage.setCreateTime(LocalDateTime.now());
        usage.setCreateBy(currentUserProvider.getCurrentUserId());
        try {
            if (promotionUsageMapper.insert(usage) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "促销使用流水写入失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(CodeEnum.DUPLICATE, "促销使用流水已存在", ex);
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_PROMOTION_USE, String.valueOf(promotionId));
    }

    private TProductPromotion changeStatus(Long id,
                                           List<ProductPromotionStatus> expectedStatuses,
                                           ProductPromotionStatus newStatus,
                                           String pauseReason,
                                           String endReason,
                                           String voidReason) {
        List<String> expected = expectedStatuses.stream().map(Enum::name).toList();
        int rows = promotionMapper.updateStatusAtomic(id, expected, newStatus.name(),
                pauseReason, endReason, voidReason, LocalDateTime.now());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_PROMOTION_STATUS_CHANGE, String.valueOf(id));
        return requirePromotion(id);
    }

    private void validateForSave(TProductPromotion promotion, TProductPromotion existing) {
        if (promotion == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销信息不能为空");
        }
        if (promotion.getProductId() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销商品不能为空");
        }
        TProduct product = productMapper.selectById(promotion.getProductId());
        if (product == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商品不存在");
        }
        promotion.setCode(requireText(promotion.getCode(), "促销编码不能为空"));
        TProductPromotion sameCode = promotionMapper.selectByCode(promotion.getCode());
        Long currentId = existing == null ? null : existing.getId();
        if (sameCode != null && (currentId == null || !currentId.equals(sameCode.getId()))) {
            throw new BusinessException(CodeEnum.DUPLICATE, "促销编码已存在");
        }
        promotion.setName(requireText(promotion.getName(), "促销名称不能为空"));
        promotion.setType(ProductPromotionType.parse(promotion.getType()).name());
        validateDiscount(ProductPromotionType.parse(promotion.getType()), promotion.getDiscount());
        promotion.setRuleSummary(requireText(promotion.getRuleSummary(), "促销规则摘要不能为空"));
        validateTimeRange(promotion);
        validateLimits(promotion, existing);
    }

    private void applyDefaults(TProductPromotion promotion) {
        promotion.setApplicableStore(normalizeScope(promotion.getApplicableStore()));
        promotion.setCustomerType(normalizeScope(promotion.getCustomerType()));
        promotion.setApplicableChannel(normalizeScope(promotion.getApplicableChannel()));
        promotion.setInventoryScope(normalizeScope(promotion.getInventoryScope()));
        promotion.setStackable(Boolean.TRUE.equals(promotion.getStackable()));
        promotion.setPriority(promotion.getPriority() == null ? 0 : promotion.getPriority());
        promotion.setUsedBudget(nullToZero(promotion.getUsedBudget()));
        promotion.setUsedCount(promotion.getUsedCount() == null ? 0 : promotion.getUsedCount());
    }

    private void validateDiscount(ProductPromotionType type, BigDecimal discount) {
        if (discount == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销优惠值不能为空");
        }
        if (type == ProductPromotionType.PERCENTAGE) {
            if (discount.compareTo(BigDecimal.ZERO) <= 0 || discount.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "百分比折扣必须大于0且小于1");
            }
            return;
        }
        if (type.monetary() && discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "金额类促销优惠必须大于0");
        }
        if (type.benefitOnly() && discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "权益类促销成本不能为负数");
        }
    }

    private void validateTimeRange(TProductPromotion promotion) {
        if (promotion.getStartTime() == null || promotion.getEndTime() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销有效期不能为空");
        }
        if (!promotion.getEndTime().isAfter(promotion.getStartTime())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销结束时间必须晚于开始时间");
        }
    }

    private void validateLimits(TProductPromotion promotion, TProductPromotion existing) {
        if (promotion.getBudgetLimit() != null && promotion.getBudgetLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销预算上限必须大于0");
        }
        if (promotion.getUsageLimit() != null && promotion.getUsageLimit() <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销使用名额必须大于0");
        }
        if (existing != null) {
            BigDecimal usedBudget = nullToZero(existing.getUsedBudget());
            int usedCount = existing.getUsedCount() == null ? 0 : existing.getUsedCount();
            if (promotion.getBudgetLimit() != null && promotion.getBudgetLimit().compareTo(usedBudget) < 0) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "预算上限不能小于已使用预算");
            }
            if (promotion.getUsageLimit() != null && promotion.getUsageLimit() < usedCount) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "使用名额不能小于已使用次数");
            }
        }
    }

    private void assertTimeRangeUsable(TProductPromotion promotion) {
        validateTimeRange(promotion);
        if (LocalDateTime.now().isAfter(promotion.getEndTime())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销已过有效期，不能发布");
        }
    }

    private void assertEffectiveNow(TProductPromotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartTime() == null || promotion.getEndTime() == null
                || now.isBefore(promotion.getStartTime()) || now.isAfter(promotion.getEndTime())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销不在有效期内");
        }
    }

    private void assertBudgetNotExhausted(TProductPromotion promotion) {
        if (promotion.getBudgetLimit() != null
                && nullToZero(promotion.getUsedBudget()).compareTo(promotion.getBudgetLimit()) >= 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销预算已用尽");
        }
        if (promotion.getUsageLimit() != null
                && promotion.getUsedCount() != null
                && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销名额已用尽");
        }
    }

    private void assertDefaultContextScope(String scope, String message) {
        if (scope != null && !scope.isBlank() && !ALL_SCOPE.equals(scope)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
    }

    private void validateLine(PromotionProductLine line) {
        if (line == null || line.getProductId() == null || line.getUnitPrice() == null || line.getQuantity() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销计算商品信息不完整");
        }
        if (line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0 || line.getQuantity() <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销计算商品价格和数量必须大于0");
        }
    }

    private TProductPromotion requirePromotion(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销ID不能为空");
        }
        TProductPromotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "促销不存在");
        }
        return promotion;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String requireReason(String reason) {
        return requireText(reason, "操作原因不能为空");
    }

    private String normalizeScope(String value) {
        if (value == null || value.isBlank()) {
            return ALL_SCOPE;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
