package com.autodealer.crm.modules.commerce.quote.application.internal;

import com.autodealer.crm.modules.commerce.catalog.application.api.port.ProductCatalogDataPort;
import com.autodealer.crm.modules.sales.opportunity.application.api.port.OpportunityDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.CreateQuoteRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.CreateQuoteVersionRequest;
import com.autodealer.crm.modules.commerce.promotion.application.api.dto.PromotionProductLine;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.QuoteDetailResponse;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.QuoteItemRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.UpdateQuoteStatusRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.enums.OpportunityStage;
import com.autodealer.crm.modules.commerce.quote.application.api.enums.QuoteStatus;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.commerce.quote.persistence.mapper.TQuoteMapper;
import com.autodealer.crm.modules.commerce.quote.persistence.mapper.TQuoteStatusHistoryMapper;
import com.autodealer.crm.modules.commerce.quote.persistence.mapper.TQuoteVersionItemMapper;
import com.autodealer.crm.modules.commerce.quote.persistence.mapper.TQuoteVersionMapper;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotion;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuote;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteStatusHistory;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersion;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersionItem;
import com.autodealer.crm.modules.commerce.quote.application.api.query.QuoteQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.commerce.promotion.application.api.ProductPromotionService;
import com.autodealer.crm.modules.commerce.quote.application.api.QuoteService;
import com.autodealer.crm.shared.infrastructure.json.JSONUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class QuoteServiceImpl implements QuoteService {

    private static final DateTimeFormatter QUOTE_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<QuoteStatus> CUSTOMER_CONFIRMATION_TARGETS = EnumSet.of(
            QuoteStatus.ACCEPTED,
            QuoteStatus.REFUSED,
            QuoteStatus.EXPIRED
    );
    private static final Set<String> CONFIRMATION_METHODS = Set.of(
            "CUSTOMER_SIGNATURE",
            "CALL_RECORD",
            "WECHAT",
            "EMAIL",
            "SYSTEM_EXPIRE",
            "PROXY"
    );

    private final TQuoteMapper quoteMapper;
    private final TQuoteVersionMapper versionMapper;
    private final TQuoteVersionItemMapper itemMapper;
    private final TQuoteStatusHistoryMapper historyMapper;
    private final ProductCatalogDataPort productMapper;
    private final CustomerDataPort customerMapper;
    private final OpportunityDataPort opportunityMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final ProductPromotionService promotionService;

    public QuoteServiceImpl(TQuoteMapper quoteMapper,
                            TQuoteVersionMapper versionMapper,
                            TQuoteVersionItemMapper itemMapper,
                            TQuoteStatusHistoryMapper historyMapper,
                            ProductCatalogDataPort productMapper,
                            CustomerDataPort customerMapper,
                            OpportunityDataPort opportunityMapper,
                            CurrentUserProvider currentUserProvider,
                            OperationAuditRecorder auditRecorder,
                            ProductPromotionService promotionService) {
        this.quoteMapper = quoteMapper;
        this.versionMapper = versionMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
        this.productMapper = productMapper;
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
        this.promotionService = promotionService;
    }

    @Override
    public PageInfo<TQuote> getQuotePage(QuoteQuery query) {
        QuoteQuery actualQuery = query == null ? new QuoteQuery() : query;
        if (actualQuery.getStatus() != null && !actualQuery.getStatus().isBlank()) {
            QuoteStatus.parse(actualQuery.getStatus());
        }
        int page = actualQuery.getPage() == null ? 1 : actualQuery.getPage();
        int size = actualQuery.getSize() == null ? 10 : actualQuery.getSize();
        PageHelper.startPage(page, size);
        List<TQuote> quotes = quoteMapper.selectByQuery(actualQuery, currentUserProvider.getDataScopeUserId());
        return new PageInfo<>(quotes);
    }

    @Override
    public QuoteDetailResponse getQuoteDetail(Long id) {
        TQuote quote = requireAccessibleQuote(id);
        return buildDetail(quote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuoteDetailResponse createQuote(CreateQuoteRequest request) {
        requireAccessibleCustomer(request.getCustomerId());
        requireValidOpportunityLink(request.getOpportunityId(), request.getCustomerId());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        TQuote quote = new TQuote();
        quote.setQuoteNo(generateQuoteNo(now));
        quote.setCustomerId(request.getCustomerId());
        quote.setOpportunityId(request.getOpportunityId());
        quote.setStatus(QuoteStatus.DRAFT.name());
        quote.setRemark(request.getRemark());
        quote.setCreateTime(now);
        quote.setCreateBy(operatorId);
        quote.setUpdateTime(now);
        quote.setUpdateBy(operatorId);
        if (quoteMapper.insert(quote) != 1 || quote.getId() == null) {
            throw new BusinessException(CodeEnum.FAIL, "报价创建失败");
        }

        TQuoteVersion version = buildVersion(quote.getId(), 1, request.getValidUntil(), request.getRemark(),
                request.getItems(), now, operatorId);
        insertVersionWithItems(version, request.getItems(), now, operatorId);
        updateCurrentVersion(quote.getId(), version.getId(), now, operatorId);
        writeHistory(quote.getId(), null, QuoteStatus.DRAFT, "创建报价", null, now, operatorId);
        auditRecorder.record(AuditActionEnum.QUOTE_CREATE, String.valueOf(quote.getId()));
        auditRecorder.record(AuditActionEnum.QUOTE_VERSION_CREATE, String.valueOf(version.getId()));
        return buildDetail(requireAccessibleQuote(quote.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuoteDetailResponse createVersion(Long quoteId, CreateQuoteVersionRequest request) {
        TQuote quote = requireAccessibleQuote(quoteId);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        if (QuoteStatus.CONVERTED_TO_ORDER.name().equals(quote.getStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "已转订单报价不能再生成新版本");
        }

        TQuoteVersion version;
        if (QuoteStatus.DRAFT.name().equals(quote.getStatus()) && quote.getCurrentVersionId() != null) {
            TQuoteVersion current = versionMapper.selectById(quote.getCurrentVersionId());
            if (current == null) {
                throw new BusinessException(CodeEnum.NOT_FOUND, "当前报价版本不存在");
            }
            version = buildVersion(quoteId, current.getVersionNo(), request.getValidUntil(), request.getRemark(),
                    request.getItems(), now, operatorId);
            version.setId(current.getId());
            if (versionMapper.updateDraftVersion(version) != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "草稿报价版本更新失败");
            }
            itemMapper.deleteByVersionId(version.getId());
            insertItems(version.getId(), request.getItems(), now, operatorId);
        } else {
            int nextVersionNo = versionMapper.selectMaxVersionNo(quoteId) + 1;
            version = buildVersion(quoteId, nextVersionNo, request.getValidUntil(), request.getRemark(),
                    request.getItems(), now, operatorId);
            insertVersionWithItems(version, request.getItems(), now, operatorId);
            updateCurrentVersion(quoteId, version.getId(), now, operatorId);
            QuoteStatus previous = QuoteStatus.parse(quote.getStatus());
            if (previous != QuoteStatus.DRAFT) {
                updateQuoteStatus(quoteId, previous, QuoteStatus.DRAFT, "生成新报价版本", null, now, operatorId);
            }
        }

        auditRecorder.record(AuditActionEnum.QUOTE_VERSION_CREATE, String.valueOf(version.getId()));
        return buildDetail(requireAccessibleQuote(quoteId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TQuote transitionStatus(Long quoteId, UpdateQuoteStatusRequest request) {
        TQuote quote = requireAccessibleQuote(quoteId);
        QuoteStatus current = QuoteStatus.parse(request.getExpectedStatus());
        QuoteStatus target = QuoteStatus.parse(request.getTargetStatus());
        if (!quote.getStatus().equals(current.name())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "报价状态已变化，请刷新后重试");
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "报价状态不允许这样流转");
        }
        validateConfirmationEvidence(target, request);

        LocalDateTime now = LocalDateTime.now();
        Integer operatorId = currentUserProvider.getCurrentUserId();
        updateQuoteStatus(quoteId, current, target, request.getReason(), request, now, operatorId);
        auditRecorder.record(AuditActionEnum.QUOTE_STATUS_CHANGE, String.valueOf(quoteId));
        return requireAccessibleQuote(quoteId);
    }

    @Override
    public List<TQuoteVersion> getVersions(Long quoteId) {
        requireAccessibleQuote(quoteId);
        return versionMapper.selectByQuoteId(quoteId);
    }

    private QuoteDetailResponse buildDetail(TQuote quote) {
        QuoteDetailResponse response = new QuoteDetailResponse();
        response.setQuote(quote);
        if (quote.getCurrentVersionId() != null) {
            TQuoteVersion version = versionMapper.selectById(quote.getCurrentVersionId());
            response.setCurrentVersion(version);
            response.setItems(version == null ? List.of() : itemMapper.selectByVersionId(version.getId()));
        } else {
            response.setItems(List.of());
        }
        return response;
    }

    private void updateQuoteStatus(Long quoteId,
                                   QuoteStatus current,
                                   QuoteStatus target,
                                   String reason,
                                   UpdateQuoteStatusRequest confirmation,
                                   LocalDateTime now,
                                   Integer operatorId) {
        if (quoteMapper.updateStatusIfCurrent(quoteId, current.name(), target.name(), now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "报价状态更新失败");
        }
        writeHistory(quoteId, current, target, reason, confirmation, now, operatorId);
    }

    private void updateCurrentVersion(Long quoteId, Long versionId, LocalDateTime now, Integer operatorId) {
        if (quoteMapper.updateCurrentVersion(quoteId, versionId, now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "报价当前版本更新失败");
        }
    }

    private TQuoteVersion buildVersion(Long quoteId,
                                       Integer versionNo,
                                       LocalDateTime validUntil,
                                       String remark,
                                       List<QuoteItemRequest> items,
                                       LocalDateTime now,
                                       Integer operatorId) {
        TQuoteVersion version = new TQuoteVersion();
        version.setQuoteId(quoteId);
        version.setVersionNo(versionNo);
        version.setValidUntil(validUntil);
        version.setTotalAmount(calculateTotal(items));
        version.setRemark(remark);
        version.setCreateTime(now);
        version.setCreateBy(operatorId);
        return version;
    }

    private BigDecimal calculateTotal(List<QuoteItemRequest> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (QuoteItemRequest item : items) {
            TProduct product = requireOnSaleProduct(item.getProductId());
            BigDecimal lineAmount = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal promotionAmount = calculateQuotePromotionAmount(item, lineAmount);
            total = total.add(lineAmount.subtract(promotionAmount).max(BigDecimal.ZERO));
        }
        return total;
    }

    private void insertVersionWithItems(TQuoteVersion version,
                                        List<QuoteItemRequest> items,
                                        LocalDateTime now,
                                        Integer operatorId) {
        if (versionMapper.insert(version) != 1 || version.getId() == null) {
            throw new BusinessException(CodeEnum.FAIL, "报价版本创建失败");
        }
        insertItems(version.getId(), items, now, operatorId);
    }

    private void insertItems(Long versionId,
                             List<QuoteItemRequest> items,
                             LocalDateTime now,
                             Integer operatorId) {
        for (QuoteItemRequest requestItem : items) {
            TProduct product = requireOnSaleProduct(requestItem.getProductId());
            TQuoteVersionItem item = new TQuoteVersionItem();
            item.setQuoteVersionId(versionId);
            item.setProductId(product.getId());
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setProductSpecification(product.getSpecification());
            item.setGuidePrice(product.getPrice());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(requestItem.getQuantity());
            BigDecimal lineAmount = product.getPrice().multiply(BigDecimal.valueOf(requestItem.getQuantity()));
            item.setLineAmount(lineAmount);
            applyPromotionSnapshot(item, requestItem, lineAmount);
            item.setCreateTime(now);
            item.setCreateBy(operatorId);
            if (itemMapper.insert(item) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "报价商品快照创建失败");
            }
        }
    }

    private void applyPromotionSnapshot(TQuoteVersionItem item,
                                        QuoteItemRequest requestItem,
                                        BigDecimal lineAmount) {
        Long promotionId = requestItem.getPromotionId();
        if (promotionId == null) {
            item.setPromotionAmount(BigDecimal.ZERO);
            return;
        }
        TProductPromotion promotion = requireQuotePromotion(requestItem, lineAmount);
        BigDecimal promotionAmount = promotionService.calculateDiscount(List.of(
                new PromotionProductLine(requestItem.getProductId(), lineAmount
                        .divide(BigDecimal.valueOf(requestItem.getQuantity())), requestItem.getQuantity())
        ), promotion);
        item.setPromotionId(promotion.getId());
        item.setPromotionCode(promotion.getCode());
        item.setPromotionName(promotion.getName());
        item.setPromotionRuleSummary(promotion.getRuleSummary());
        item.setPromotionAmount(promotionAmount);
        item.setPromotionSnapshot(JSONUtils.toJSON(promotion));
    }

    private BigDecimal calculateQuotePromotionAmount(QuoteItemRequest requestItem, BigDecimal lineAmount) {
        if (requestItem.getPromotionId() == null) {
            return BigDecimal.ZERO;
        }
        TProductPromotion promotion = requireQuotePromotion(requestItem, lineAmount);
        return promotionService.calculateDiscount(List.of(
                new PromotionProductLine(requestItem.getProductId(), lineAmount
                        .divide(BigDecimal.valueOf(requestItem.getQuantity())), requestItem.getQuantity())
        ), promotion);
    }

    private TProductPromotion requireQuotePromotion(QuoteItemRequest requestItem, BigDecimal lineAmount) {
        if (requestItem.getQuantity() == null || requestItem.getQuantity() <= 0 || lineAmount == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "报价商品数量无效");
        }
        return promotionService.requireApplicablePromotion(
                requestItem.getPromotionId(), List.of(requestItem.getProductId()));
    }

    private TProduct requireOnSaleProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "产品ID不能为空");
        }
        TProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商品不存在");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "商品当前不可销售");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "商品价格无效");
        }
        return product;
    }

    private void writeHistory(Long quoteId,
                              QuoteStatus fromStatus,
                              QuoteStatus toStatus,
                              String reason,
                              UpdateQuoteStatusRequest confirmation,
                              LocalDateTime now,
                              Integer operatorId) {
        TQuoteStatusHistory history = new TQuoteStatusHistory();
        history.setQuoteId(quoteId);
        history.setFromStatus(fromStatus == null ? null : fromStatus.name());
        history.setToStatus(toStatus.name());
        history.setReason(reason);
        if (CUSTOMER_CONFIRMATION_TARGETS.contains(toStatus) && confirmation != null) {
            history.setConfirmedByName(trimToNull(confirmation.getConfirmedByName()));
            history.setConfirmedAt(confirmation.getConfirmedAt());
            history.setConfirmationMethod(trimToNull(confirmation.getConfirmationMethod()));
            history.setConfirmationEvidence(trimToNull(confirmation.getConfirmationEvidence()));
            history.setProxyConfirmReason(trimToNull(confirmation.getProxyConfirmReason()));
        }
        history.setCreateTime(now);
        history.setCreateBy(operatorId);
        if (historyMapper.insert(history) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "报价状态历史创建失败");
        }
    }

    private TQuote requireAccessibleQuote(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "报价ID不能为空");
        }
        TQuote quote = quoteMapper.selectById(id);
        if (quote == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "报价不存在");
        }
        requireAccessibleCustomer(quote.getCustomerId());
        return quote;
    }

    private void requireAccessibleCustomer(Integer customerId) {
        if (customerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户ID不能为空");
        }
        TCustomer customer = customerMapper.selectScopedById(customerId, currentUserProvider.getDataScopeUserId());
        if (customer == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "客户不存在或无权访问");
        }
    }

    private void requireValidOpportunityLink(Long opportunityId, Integer customerId) {
        if (opportunityId == null) {
            return;
        }
        TOpportunity opportunity = opportunityMapper.selectById(opportunityId);
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        if (opportunity == null || (dataScopeUserId != null && !dataScopeUserId.equals(opportunity.getOwnerId()))) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "商机不存在或无权访问");
        }
        if (!customerId.equals(opportunity.getCustomerId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "报价客户必须与商机客户一致");
        }
        OpportunityStage stage;
        try {
            stage = OpportunityStage.parse(opportunity.getStage());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
        if (stage.terminal()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态商机不能创建报价");
        }
    }

    private void validateConfirmationEvidence(QuoteStatus target, UpdateQuoteStatusRequest request) {
        if (!CUSTOMER_CONFIRMATION_TARGETS.contains(target)) {
            return;
        }
        String confirmedByName = trimToNull(request.getConfirmedByName());
        String method = trimToNull(request.getConfirmationMethod());
        String evidence = trimToNull(request.getConfirmationEvidence());
        if (confirmedByName == null || request.getConfirmedAt() == null || method == null || evidence == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户确认必须填写确认人、时间、方式和凭证");
        }
        if (!CONFIRMATION_METHODS.contains(method)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户确认方式编码不合法");
        }
        if ("PROXY".equals(method) && trimToNull(request.getProxyConfirmReason()) == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "代确认必须填写代确认原因");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateQuoteNo(LocalDateTime now) {
        return "BJ" + now.format(QUOTE_NO_FORMAT) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
