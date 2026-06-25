package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.enums.PaymentMethod;
import com.autodealer.crm.enums.PaymentStatus;
import com.autodealer.crm.enums.PaymentType;
import com.autodealer.crm.enums.RefundRequestStatus;
import com.autodealer.crm.enums.RefundType;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.TranQuery;
import com.autodealer.crm.service.TranService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.dto.SettlementPreviewResponse;
import com.autodealer.crm.dto.SettleRequest;
import com.autodealer.crm.service.ProductPromotionService;
import com.autodealer.crm.util.JSONUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TranServiceImpl implements TranService {

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Resource
    private TTranMapper tranMapper;

    @Resource
    private TTranRemarkMapper tranRemarkMapper;

    @Resource
    private TTranProductMapper tranProductMapper;

    @Resource
    private TTranInvoiceMapper tranInvoiceMapper;

    @Resource
    private TTranApproveMapper tranApproveMapper;

    @Resource
    private TProductMapper productMapper;

    @Resource
    private RedisManager redisManager;

    @Resource
    private TPaymentMapper paymentMapper;

    @Resource
    private TRefundRequestMapper refundRequestMapper;

    @Resource
    private TTranHistoryMapper tranHistoryMapper;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Resource
    private ProductPromotionService promotionService;

    @Override
    public PageInfo<TTran> getTransactionList(TranQuery query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TTran> tTranList = tranMapper.selectByQuery(query);
        return new PageInfo<>(tTranList);
    }

    @Override
    public TTran getTransactionById(Integer id) {
        return findAccessibleTransaction(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createTransaction(TTran tTran, List<TTranProduct> products) {
        if (tTran == null) {
            throw new IllegalArgumentException("交易信息不能为空");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        tTran.setStage(TranStage.QUOTATION);
        tTran.setCreateBy(operatorId);
        tTran.setCreateTime(now);
        tTran.setTranNo(generateTranNo());

        // 阶段1：服务端验证产品并计算总金额（insert 前完成，确保 money 被持久化）
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                if (product == null || product.getProductId() == null
                        || product.getQuantity() == null || product.getQuantity() <= 0) {
                    throw new BusinessException(CodeEnum.PARAM_ERROR, "交易商品 ID 和正数数量不能为空");
                }
                TProduct dbProduct = productMapper.selectById(product.getProductId().longValue());
                if (dbProduct == null) {
                    throw new BusinessException(CodeEnum.NOT_FOUND,
                            "商品不存在: " + product.getProductId());
                }
                if (!"on_sale".equals(dbProduct.getStatus())) {
                    throw new BusinessException(CodeEnum.OPERATION_FAILED,
                            "商品 [" + dbProduct.getName() + "] 当前不可销售");
                }
                if (dbProduct.getPrice() == null
                        || dbProduct.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(CodeEnum.PARAM_ERROR,
                            "商品 [" + dbProduct.getName() + "] 价格无效");
                }
                // 使用数据库价格作为快照
                product.setPrice(dbProduct.getPrice());
                totalAmount = totalAmount.add(
                        dbProduct.getPrice().multiply(new BigDecimal(product.getQuantity())));
            }
        }
        // 金额在插入前设置，确保 insertSelective 持久化 money
        tTran.setMoney(totalAmount);

        if (tranMapper.insertSelective(tTran) != 1 || tTran.getId() == null) {
            throw new BusinessException(CodeEnum.FAIL, "交易创建失败");
        }
        Integer tranId = tTran.getId();

        // 阶段2：插入交易产品行项并扣减库存
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                product.setCreateBy(operatorId);
                product.setCreateTime(now);
                if (tranProductMapper.insertSelective(product) != 1) {
                    throw new BusinessException(CodeEnum.FAIL,
                            "交易商品创建失败: " + product.getProductId());
                }
                int updateCount = productMapper.updateStock(
                        product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new BusinessException(CodeEnum.OPERATION_FAILED,
                            "产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
        }

        writeHistory(tranId, TranStage.QUOTATION, tTran.getMoney(),
                tTran.getCreateBy() != null ? tTran.getExpectedDate() : null,
                tTran.getCreateBy());

        auditRecorder.record(AuditActionEnum.TRAN_CREATE, String.valueOf(tranId));
        clearTransactionCache(tranId);
        return tranId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransaction(TTran tTran) {
        if (tTran == null || tTran.getId() == null) {
            return false;
        }

        TTran existing = findAccessibleTransaction(tTran.getId());
        if (existing == null) {
            return false;
        }

        if (existing.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "仅待报价阶段的交易可以修改");
        }

        tTran.setStage(null);
        tTran.setEditBy(currentUserProvider.getCurrentUserId());
        tTran.setEditTime(new Date());
        int rows = tranMapper.updateByPrimaryKeySelective(tTran);

        if (rows > 0) {
            int versionRows = tranMapper.incrementVersion(
                    tTran.getId(), existing.getVersion() == null ? 0 : existing.getVersion(),
                    currentUserProvider.getCurrentUserId());
            if (versionRows != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已被其他用户修改");
            }
            clearTransactionCache(tTran.getId());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementPreviewResponse settleTransaction(Integer tranId, SettleRequest request) {
        if (request == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "结算请求不能为空");
        }
        SettlementPreviewResponse calculated = getSettlementPreview(tranId, request.getPromotionId());
        if (!calculated.getTransactionVersion().equals(request.getExpectedVersion())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已变更，请重新获取结算预览");
        }
        if (!calculated.getPricingFingerprint().equals(request.getPricingFingerprint())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "计价条件已变更，请重新获取结算预览");
        }
        if (calculated.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "结算金额必须大于0");
        }

        String promotionSnapshot = calculated.getPromotion() == null
                ? null : JSONUtils.toJSON(calculated.getPromotion());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = tranMapper.settleAtomic(
                tranId,
                calculated.getFinalAmount(),
                calculated.getOriginalAmount(),
                calculated.getDiscountAmount(),
                calculated.getPromotionId(),
                promotionSnapshot,
                request.getExpectedVersion(),
                operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态或版本已变更，请重新预览");
        }

        TTran tran = tranMapper.selectByPrimaryKey(tranId);
        writeHistory(tranId, TranStage.PENDING, calculated.getFinalAmount(),
                tran != null ? tran.getExpectedDate() : null, operatorId);
        auditRecorder.record(AuditActionEnum.TRAN_SETTLE, String.valueOf(tranId));
        clearTransactionCache(tranId);
        calculated.setTransactionVersion(request.getExpectedVersion() + 1);
        return calculated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementPreviewResponse getSettlementPreview(Integer tranId, Long promotionId) {
        if (tranId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易 ID 不能为空");
        }
        TTran tran = requireAccessibleTransaction(tranId);
        if (tran == null || tran.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不是待报价，无法预览结算");
        }
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products == null || products.isEmpty()) {
            throw new BusinessException(CodeEnum.TRAN_NO_PRODUCTS, "该交易没有产品信息");
        }
        BigDecimal originalAmount = calculateOriginalAmount(products);
        TProductPromotion promotion = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (promotionId != null) {
            promotion = promotionService.getPromotionById(promotionId);
            if (promotion == null) {
                throw new BusinessException(CodeEnum.NOT_FOUND, "促销不存在");
            }
            if (!isActivePromotionStatus(promotion.getStatus())) {
                throw new BusinessException(CodeEnum.FAIL, "促销状态不是进行中");
            }
            if (!isPromotionEffectiveNow(promotion)) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "促销不在有效期内");
            }
            discountAmount = calculatePromotionDiscount(products, promotion);
        }
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        SettlementPreviewResponse resp = new SettlementPreviewResponse();
        resp.setTranId(tranId);
        resp.setPromotionId(promotionId);
        resp.setOriginalAmount(originalAmount);
        resp.setDiscountAmount(discountAmount);
        resp.setFinalAmount(finalAmount);
        resp.setTransactionVersion(tran.getVersion() != null ? tran.getVersion() : 0);
        resp.setPricingFingerprint(buildPricingFingerprint(tranId, tran.getVersion(), products, promotion));
        if (promotion != null) {
            SettlementPreviewResponse.PromotionInfo info = new SettlementPreviewResponse.PromotionInfo();
            info.setId(promotion.getId());
            info.setName(promotion.getName());
            info.setType(promotion.getType());
            info.setDiscount(promotion.getDiscount());
            info.setProductId(promotion.getProductId());
            info.setStartTime(String.valueOf(promotion.getStartTime()));
            info.setEndTime(String.valueOf(promotion.getEndTime()));
            info.setUpdateTime(String.valueOf(promotion.getUpdateTime()));
            resp.setPromotion(info);
        }
        return resp;
    }

    @Override
    public List<TProductPromotion> getAvailablePromotions(Integer tranId) {
        if (tranId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易 ID 不能为空");
        }
        TTran tran = requireAccessibleTransaction(tranId);
        if (tran.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不是待报价，无法选择促销");
        }
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = products.stream()
                .map(TTranProduct::getProductId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        return promotionService.getAvailablePromotions(productIds);
    }

    private boolean isActivePromotionStatus(String status) {
        return "进行中".equals(status) || "ACTIVE".equals(status) || "active".equals(status);
    }

    private boolean isPromotionEffectiveNow(TProductPromotion promotion) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return promotion.getStartTime() != null && promotion.getEndTime() != null
                && !now.isBefore(promotion.getStartTime()) && !now.isAfter(promotion.getEndTime());
    }

    private BigDecimal calculateOriginalAmount(List<TTranProduct> products) {
        BigDecimal total = BigDecimal.ZERO;
        for (TTranProduct p : products) {
            validateSettlementProduct(p);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePromotionDiscount(List<TTranProduct> products, TProductPromotion promotion) {
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal promoDiscount = promotion.getDiscount();
        if (promotion.getProductId() == null || promoDiscount == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销商品和优惠值不能为空");
        }
        String type = promotion.getType();
        boolean matched = false;
        if ("PERCENTAGE".equals(type)) {
            if (promoDiscount.compareTo(BigDecimal.ZERO) <= 0 || promoDiscount.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "折扣必须在0到1之间");
            }
            for (TTranProduct p : products) {
                validateSettlementProduct(p);
                if (!promotion.getProductId().equals(p.getProductId())) continue;
                matched = true;
                BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity()));
                BigDecimal lineDiscount = lineTotal.multiply(BigDecimal.ONE.subtract(promoDiscount));
                discount = discount.add(lineDiscount);
            }
        } else if ("AMOUNT".equals(type)) {
            if (promoDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "优惠金额必须大于0");
            }
            for (TTranProduct p : products) {
                validateSettlementProduct(p);
                if (!promotion.getProductId().equals(p.getProductId())) continue;
                matched = true;
                BigDecimal lineDiscount = promoDiscount.multiply(BigDecimal.valueOf(p.getQuantity()));
                discount = discount.add(lineDiscount);
            }
        } else {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "不支持的促销类型");
        }
        if (!matched) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销不适用于当前交易商品");
        }
        BigDecimal originalMatched = BigDecimal.ZERO;
        for (TTranProduct p : products) {
            if (!promotion.getProductId().equals(p.getProductId())) continue;
            originalMatched = originalMatched.add(p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
        }
        if (discount.compareTo(originalMatched) > 0) {
            discount = originalMatched;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateSettlementProduct(TTranProduct product) {
        if (product == null || product.getProductId() == null
                || product.getPrice() == null || product.getQuantity() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易商品价格或数量不完整");
        }
        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0 || product.getQuantity() <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易商品价格和数量必须大于0");
        }
    }

    private String buildPricingFingerprint(Integer tranId, Integer version, List<TTranProduct> products, TProductPromotion promotion) {
        StringBuilder sb = new StringBuilder();
        sb.append("tran:").append(tranId).append("|v:").append(version == null ? 0 : version);
        if (products != null) {
            List<TTranProduct> orderedProducts = new ArrayList<>(products);
            orderedProducts.sort(Comparator
                    .comparing(TTranProduct::getId, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(TTranProduct::getProductId, Comparator.nullsLast(Long::compareTo)));
            for (TTranProduct p : orderedProducts) {
                sb.append("|p:").append(p.getProductId()).append("x").append(p.getQuantity()).append("@").append(p.getPrice());
            }
        }
        if (promotion != null) {
            sb.append("|promo:").append(promotion.getId())
              .append("|type:").append(promotion.getType())
              .append("|disc:").append(promotion.getDiscount())
              .append("|pid:").append(promotion.getProductId())
              .append("|s:").append(promotion.getStartTime())
              .append("|e:").append(promotion.getEndTime())
              .append("|u:").append(promotion.getUpdateTime());
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public boolean addTransactionRemark(TTranRemark remark) {
        if (remark == null || remark.getTranId() == null) {
            return false;
        }
        requireAccessibleTransaction(remark.getTranId());
        remark.setCreateBy(currentUserProvider.getCurrentUserId());
        remark.setCreateTime(new Date());
        int result = tranRemarkMapper.insert(remark);
        if (result > 0) {
            clearTransactionCache(remark.getTranId());
            return true;
        }
        return false;
    }

    @Override
    public List<TTranProduct> getTransactionProducts(Integer tranId) {
        requireAccessibleTransaction(tranId);
        String cacheKey = Constants.CACHE_KEY_TRAN_PRODUCTS + tranId;
        List<TTranProduct> products = redisManager.get(cacheKey);
        if (products != null) {
            return products;
        }

        products = tranProductMapper.selectByTranId(tranId);
        if (products != null) {
            redisManager.set(cacheKey, products, Constants.CACHE_EXPIRE_TIME);
        }
        return products;
    }

    @Override
    public List<TTranInvoice> getTransactionInvoices(Integer tranId) {
        requireAccessibleTransaction(tranId);
        String cacheKey = Constants.CACHE_KEY_TRAN_INVOICES + tranId;
        List<TTranInvoice> invoices = redisManager.get(cacheKey);
        if (invoices != null) {
            return invoices;
        }

        invoices = tranInvoiceMapper.selectByTranId(tranId);
        if (invoices != null) {
            redisManager.set(cacheKey, invoices, Constants.CACHE_EXPIRE_TIME);
        }
        return invoices;
    }

    @Override
    public List<TTranRemark> getTransactionRemarks(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return tranRemarkMapper.selectByTranId(tranId);
    }

    @Override
    public List<TTranProduct> getTransactionProductDetails(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return tranMapper.selectTranProductsByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTransactionProducts(Integer tranId) {
        TTran tran = requireAccessibleTransaction(tranId);
        if (tran.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "仅待报价阶段可以修改交易商品");
        }
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                if (productMapper.updateStock(
                        product.getProductId().longValue(), product.getQuantity()) != 1) {
                    throw new BusinessException(CodeEnum.FAIL, "恢复产品库存失败: " + product.getProductId());
                }
            }
        }

        tranProductMapper.deleteByTranId(tranId);
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTransactionProducts(Integer tranId, List<TTranProduct> products) {
        TTran tran = requireAccessibleTransaction(tranId);
        if (tran.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "仅待报价阶段可以修改交易商品");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                if (product == null || product.getProductId() == null
                        || product.getQuantity() == null || product.getQuantity() <= 0) {
                    throw new BusinessException(CodeEnum.PARAM_ERROR, "交易商品 ID 和正数数量不能为空");
                }
                // 从数据库查询商品价格，防止客户端篡改
                TProduct dbProduct = productMapper.selectById(product.getProductId().longValue());
                if (dbProduct == null) {
                    throw new BusinessException(CodeEnum.NOT_FOUND,
                            "商品不存在: " + product.getProductId());
                }
                if (!"on_sale".equals(dbProduct.getStatus())) {
                    throw new BusinessException(CodeEnum.OPERATION_FAILED,
                            "商品 [" + dbProduct.getName() + "] 当前不可销售");
                }
                if (dbProduct.getPrice() == null
                        || dbProduct.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(CodeEnum.PARAM_ERROR,
                            "商品 [" + dbProduct.getName() + "] 价格无效");
                }
                product.setPrice(dbProduct.getPrice());
                product.setTranId(tranId);
                product.setCreateBy(operatorId);
                product.setCreateTime(new Date());
                if (tranProductMapper.insertSelective(product) != 1) {
                    throw new BusinessException(CodeEnum.FAIL,
                            "交易商品创建失败: " + product.getProductId());
                }

                int updateCount = productMapper.updateStock(
                        product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new BusinessException(CodeEnum.OPERATION_FAILED,
                            "产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
        }

        // 重新计算交易总金额并持久化到 t_tran.money
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (products != null) {
            for (TTranProduct product : products) {
                if (product.getPrice() != null && product.getQuantity() != null) {
                    totalAmount = totalAmount.add(
                            product.getPrice().multiply(new BigDecimal(product.getQuantity())));
                }
            }
        }
        TTran moneyUpdate = new TTran();
        moneyUpdate.setId(tranId);
        moneyUpdate.setMoney(totalAmount);
        if (tranMapper.updateByPrimaryKeySelective(moneyUpdate) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "交易金额更新失败");
        }

        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveTran(Integer tranId, Boolean approved, String comment) {
        if (approved == null || comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("审批结果和审批意见不能为空");
        }
        TTran tran = requireAccessibleTransaction(tranId);
        Integer approveBy = currentUserProvider.getCurrentUserId();
        if (approveBy != null && approveBy.equals(tran.getCreateBy())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "交易创建人不能审批自己的交易");
        }
        Date now = new Date();

        // 原子 CAS：仅 PENDING 阶段可审批
        int stageResult = tranMapper.updateStageAtomic(tranId,
                approved ? TranStage.APPROVED : TranStage.LOST,
                TranStage.PENDING, approveBy);
        if (stageResult == 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许审批操作");
        }

        if (!approved) {
            restoreTransactionStock(tranId);
        }

        writeHistory(tranId, approved ? TranStage.APPROVED : TranStage.LOST,
                tran.getMoney(), tran.getExpectedDate(), approveBy);

        TTranApprove approve = new TTranApprove();
        approve.setTranId(tranId);
        approve.setApproveResult(approved);
        approve.setApproveComment(comment);
        approve.setApproveTime(now);
        approve.setApproveBy(approveBy);
        approve.setCreateTime(now);
        approve.setCreateBy(approveBy);

        if (tranApproveMapper.insertSelective(approve) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "审批记录创建失败");
        }
        auditRecorder.record(AuditActionEnum.TRAN_APPROVE, String.valueOf(tranId));
        clearTransactionCache(tranId);
        return true;
    }

    @Override
    public TTranApprove getTranApprove(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return tranApproveMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTranInvoice(TTranInvoice invoice) {
        if (invoice == null || invoice.getTranId() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "发票信息不完整");
        }

        TTran tran = requireAccessibleTransaction(invoice.getTranId());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        invoice.setCreateBy(operatorId);
        invoice.setEditBy(operatorId);
        if (invoice.getAmount() == null || tran.getMoney() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "发票金额和交易金额不能为空");
        }
        if (invoice.getAmount().compareTo(tran.getMoney()) != 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "发票金额必须等于交易结算金额");
        }
        List<TTranInvoice> existingInvoices = tranInvoiceMapper.selectByTranId(invoice.getTranId());
        if (existingInvoices != null && !existingInvoices.isEmpty()) {
            throw new BusinessException(CodeEnum.DUPLICATE, "该交易已开具发票，不可重复开票");
        }
        if (tran.getStage() != TranStage.APPROVED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许创建发票");
        }

        Date now = new Date();
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setStatus("PENDING");
        invoice.setCreateTime(now);
        invoice.setEditTime(now);

        int result = tranInvoiceMapper.insertSelective(invoice);
        if (result != 1) {
            throw new BusinessException(CodeEnum.FAIL, "发票创建失败");
        }
        auditRecorder.record(AuditActionEnum.INVOICE_CREATE, String.valueOf(invoice.getTranId()));
        clearTransactionCache(invoice.getTranId());
        return true;
    }

    @Override
    public List<TTranInvoice> getTranInvoices(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return tranInvoiceMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTranInvoiceStatus(Integer invoiceId, String status) {
        if (!"ISSUED".equals(status) && !"VOID".equals(status)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "不支持的发票状态: " + status);
        }

        TTranInvoice currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (currentInvoice == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "发票不存在");
        }
        requireAccessibleTransaction(currentInvoice.getTranId());
        Integer updateBy = currentUserProvider.getCurrentUserId();
        boolean validTransition = "PENDING".equals(currentInvoice.getStatus())
                || ("ISSUED".equals(currentInvoice.getStatus()) && "VOID".equals(status));
        if (!validTransition || currentInvoice.getStatus().equals(status)) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前发票状态不允许此操作");
        }

        Date now = new Date();
        int result = tranInvoiceMapper.updateStatusIfCurrent(invoiceId, currentInvoice.getStatus(),
                status, "ISSUED".equals(status) ? now : currentInvoice.getIssueTime(), now, updateBy);
        if (result != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "发票状态已变更，请刷新后重试");
        }

        if ("ISSUED".equals(status)) {
            int stageRows = tranMapper.updateStageAtomic(currentInvoice.getTranId(),
                    TranStage.PAYMENT, TranStage.APPROVED, updateBy);
            if (stageRows != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不允许进入待收款");
            }
            TTran tran = tranMapper.selectByPrimaryKey(currentInvoice.getTranId());
            writeHistory(currentInvoice.getTranId(), TranStage.PAYMENT,
                    tran != null ? tran.getMoney() : null,
                    tran != null ? tran.getExpectedDate() : null, updateBy);
        } else if ("VOID".equals(status)) {
            int stageRows = tranMapper.updateStageAtomic(currentInvoice.getTranId(),
                    TranStage.APPROVED, TranStage.PAYMENT, updateBy);
            if (stageRows != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已进入后续状态，不允许作废发票");
            }
            TTran tran = tranMapper.selectByPrimaryKey(currentInvoice.getTranId());
            writeHistory(currentInvoice.getTranId(), TranStage.APPROVED,
                    tran != null ? tran.getMoney() : null,
                    tran != null ? tran.getExpectedDate() : null, updateBy);
        }

        auditRecorder.record(AuditActionEnum.INVOICE_STATUS, String.valueOf(invoiceId));
        clearTransactionCache(currentInvoice.getTranId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTransaction(Integer id) {
        requireAccessibleTransaction(id);
        TTran transaction = tranMapper.selectByPrimaryKeyForUpdate(id);
        if (transaction == null) {
            return false;
        }
        requireDeletable(transaction);
        deleteLockedTransaction(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteTransactions(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (ids.size() > Constants.MAX_BATCH_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "单次批量删除最多支持" + Constants.MAX_BATCH_SIZE + "条记录");
        }

        List<Integer> lockedIds = ids.stream().distinct().sorted().toList();
        lockedIds.forEach(this::requireAccessibleTransaction);
        for (Integer id : lockedIds) {
            TTran transaction = tranMapper.selectByPrimaryKeyForUpdate(id);
            if (transaction == null) {
                throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在: " + id);
            }
            requireDeletable(transaction);
        }

        for (Integer id : lockedIds) {
            deleteLockedTransaction(id);
        }
        return true;
    }

    private void requireDeletable(TTran transaction) {
        if (transaction.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有待报价状态的交易才能删除: " + transaction.getId());
        }
    }

    private void deleteLockedTransaction(Integer id) {
        List<TTranProduct> products = tranProductMapper.selectByTranId(id);
        if (products != null) {
            for (TTranProduct product : products) {
                int rows = productMapper.updateStock(
                        product.getProductId().longValue(), product.getQuantity());
                if (rows != 1) {
                    throw new BusinessException(CodeEnum.FAIL, "恢复产品库存失败: " + product.getProductId());
                }
            }
        }
        tranProductMapper.deleteByTranId(id);
        tranRemarkMapper.deleteByTranId(id);
        tranInvoiceMapper.deleteByTranId(id);
        tranApproveMapper.deleteByTranId(id);
        refundRequestMapper.deleteByTranId(id);
        paymentMapper.deleteByTranId(id);
        if (tranMapper.deleteByPrimaryKey(id) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "交易删除失败: " + id);
        }
        auditRecorder.record(AuditActionEnum.TRAN_DELETE, String.valueOf(id));
        clearTransactionCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resubmitTransaction(Integer tranId) {
        TTran tran = requireAccessibleTransaction(tranId);
        Integer userId = currentUserProvider.getCurrentUserId();
        // 原子 CAS：仅 LOST 阶段可重新提交
        int stageResult = tranMapper.updateStageAtomic(tranId,
                TranStage.QUOTATION, TranStage.LOST, userId);
        if (stageResult == 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许重新提交");
        }

        deductTransactionStock(tranId);

        // 清除旧的审批记录
        tranApproveMapper.deleteByTranId(tranId);

        writeHistory(tranId, TranStage.QUOTATION,
                tran.getMoney(), tran.getExpectedDate(), userId);
        auditRecorder.record(AuditActionEnum.TRAN_RESUBMIT, String.valueOf(tranId));
        clearTransactionCache(tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransactionWithProducts(TTran tran, List<TTranProduct> products) {
        if (tran == null || tran.getId() == null) {
            return false;
        }

        TTran existing = findAccessibleTransaction(tran.getId());
        if (existing == null) {
            return false;
        }

        // 仅 QUOTATION 阶段可修改产品和金额
        if (existing.getStage() != TranStage.QUOTATION) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "仅待报价阶段的交易可以修改产品和金额");
        }

        tran.setEditBy(currentUserProvider.getCurrentUserId());
        tran.setEditTime(new Date());
        tran.setStage(null);
        // 不信任客户端提交的金额，由服务端从产品行项重新计算
        tran.setMoney(null);
        int rows = tranMapper.updateByPrimaryKeySelective(tran);
        if (rows == 0) {
            return false;
        }

        // products 为 null 表示本次不修改商品；非空列表表示完整替换。
        // 空列表既不能表达“不修改”，也不允许把交易变成无商品状态。
        if (products != null) {
            if (products.isEmpty()) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "交易商品列表不能为空");
            }
            deleteTransactionProducts(tran.getId());
            // addTransactionProducts 内部会重新计算总金额并更新 t_tran.money
            addTransactionProducts(tran.getId(), products);
        }

        int versionRows = tranMapper.incrementVersion(
                tran.getId(), existing.getVersion() == null ? 0 : existing.getVersion(),
                currentUserProvider.getCurrentUserId());
        if (versionRows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已被其他用户修改");
        }

        // 获取最新金额写入历史
        TTran refreshed = tranMapper.selectByPrimaryKey(tran.getId());
        writeHistory(tran.getId(), TranStage.QUOTATION,
                refreshed != null ? refreshed.getMoney() : BigDecimal.ZERO,
                refreshed != null ? refreshed.getExpectedDate() : existing.getExpectedDate(),
                currentUserProvider.getCurrentUserId());

        clearTransactionCache(tran.getId());
        return true;
    }

    @Override
    @Deprecated
    public boolean createInvoice(TTranInvoice invoice) {
        return createTranInvoice(invoice);
    }

    @Override
    @Deprecated
    public boolean updateInvoiceStatus(Integer id, String status) {
        return updateTranInvoiceStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPayment recordPayment(TPayment payment) {
        if (payment == null || payment.getTranId() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "支付信息不完整");
        }
        validatePaymentMethod(payment.getPaymentMethod());

        requireAccessibleTransaction(payment.getTranId());
        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(payment.getTranId());
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易记录不存在");
        }
        payment.setCreateBy(currentUserProvider.getCurrentUserId());
        if (tran.getStage() != TranStage.PAYMENT) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许收款");
        }
        if (tran.getMoney() == null || tran.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易金额无效，无法收款");
        }

        String transactionRef = normalizeBlank(payment.getTransactionRef());
        if (transactionRef != null) {
            TPayment sameRefPayment = paymentMapper.selectByTransactionRef(transactionRef);
            if (sameRefPayment != null) {
                if (payment.getTranId().equals(sameRefPayment.getTranId())
                        && !PaymentType.REFUND.name().equals(sameRefPayment.getPaymentType())) {
                    return sameRefPayment;
                }
                throw new BusinessException(CodeEnum.DUPLICATE, "支付参考号已存在");
            }
            payment.setTransactionRef(transactionRef);
        }

        List<TPayment> existingPayments = paymentMapper.selectByTranId(payment.getTranId());
        boolean hasPendingReceipt = normalizePayments(existingPayments).stream()
                .anyMatch(existing -> PaymentStatus.PENDING.name().equals(existing.getPaymentStatus())
                        && !PaymentType.REFUND.name().equals(existing.getPaymentType()));
        if (hasPendingReceipt) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "存在待确认收款，请先确认或退回后再登记");
        }
        BigDecimal paidAmount = calculateNetConfirmedPaid(existingPayments);
        BigDecimal remaining = tran.getMoney().subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已收齐，无需再次登记收款");
        }

        Date now = new Date();
        payment.setPaymentNo(generatePaymentNo());
        payment.setAmount(remaining);
        payment.setPaymentType(paidAmount.compareTo(BigDecimal.ZERO) > 0
                ? PaymentType.BALANCE.name() : PaymentType.FULL.name());
        payment.setPaymentStatus(PaymentStatus.PENDING.name());
        payment.setPaymentTime(null);
        payment.setCreateTime(now);

        if (paymentMapper.insertSelective(payment) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "收款记录创建失败");
        }

        auditRecorder.record(AuditActionEnum.PAYMENT_CREATE, String.valueOf(payment.getTranId()));
        clearTransactionCache(payment.getTranId());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPayment confirmPayment(Integer paymentId, Boolean approved, String comment) {
        if (approved == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "确认结果不能为空");
        }
        TPayment payment = paymentMapper.selectByPrimaryKeyForUpdate(paymentId);
        if (payment == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "支付记录不存在");
        }
        if (PaymentType.REFUND.name().equals(payment.getPaymentType())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款流水不能执行收款确认");
        }
        if (!PaymentStatus.PENDING.name().equals(payment.getPaymentStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只能确认待确认收款");
        }

        requireAccessibleTransaction(payment.getTranId());
        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(payment.getTranId());
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易记录不存在");
        }
        if (approved && tran.getStage() != TranStage.PAYMENT) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许确认收款");
        }
        Integer userId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        String newStatus = approved ? PaymentStatus.COMPLETED.name() : PaymentStatus.FAILED.name();
        String remark = mergeRemark(payment.getRemark(), approved ? comment : requireNonBlank(comment, "退回原因不能为空"));
        int updated = paymentMapper.updateStatusIfCurrent(paymentId, PaymentStatus.PENDING.name(), newStatus,
                approved ? now : null, remark, now, userId);
        if (updated != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "收款状态已变更，请刷新后重试");
        }

        payment.setPaymentStatus(newStatus);
        payment.setPaymentTime(approved ? now : null);
        payment.setRemark(remark);
        payment.setEditBy(userId);
        payment.setEditTime(now);

        if (approved) {
            List<TPayment> payments = mergePaymentSnapshot(paymentMapper.selectByTranId(payment.getTranId()), payment);
            BigDecimal confirmedAmount = calculateNetConfirmedPaid(payments);
            if (confirmedAmount.compareTo(tran.getMoney()) >= 0) {
                int stageRows = tranMapper.updateStageAtomic(payment.getTranId(),
                        TranStage.DELIVERY, TranStage.PAYMENT, userId);
                if (stageRows != 1) {
                    throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不允许进入待交付");
                }
                writeHistory(payment.getTranId(), TranStage.DELIVERY,
                        tran.getMoney(), tran.getExpectedDate(), userId);
            }
            auditRecorder.record(AuditActionEnum.PAYMENT_CONFIRM, String.valueOf(paymentId));
        } else {
            auditRecorder.record(AuditActionEnum.PAYMENT_REJECT, String.valueOf(paymentId));
        }

        clearTransactionCache(payment.getTranId());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPayment refundPayment(Integer paymentId) {
        throw new BusinessException(CodeEnum.OPERATION_FAILED, "退款必须先提交申请，并在审批通过后执行");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TRefundRequest createRefundRequest(Integer paymentId, TRefundRequest request) {
        if (request == null || request.getAmount() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "退款申请信息不完整");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "退款金额必须大于0");
        }
        String reason = requireNonBlank(request.getReason(), "退款原因不能为空");
        RefundType refundType = parseRefundType(request.getRefundType());

        TPayment original = paymentMapper.selectByPrimaryKeyForUpdate(paymentId);
        if (original == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "原收款记录不存在");
        }
        if (!PaymentStatus.COMPLETED.name().equals(original.getPaymentStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只能对已确认到账的收款申请退款");
        }
        if (PaymentType.REFUND.name().equals(original.getPaymentType())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款流水不能再次申请退款");
        }

        TTran tran = requireAccessibleTransaction(original.getTranId());
        if (tran.getStage() != TranStage.PAYMENT && tran.getStage() != TranStage.DELIVERY) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许申请退款");
        }

        BigDecimal refundable = calculateAvailableRefundAmount(original.getId(), original.getAmount(), true);
        if (request.getAmount().compareTo(refundable) > 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "退款金额不能超过可退金额");
        }

        Integer userId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        TRefundRequest refundRequest = new TRefundRequest();
        refundRequest.setTranId(original.getTranId());
        refundRequest.setOriginalPaymentId(original.getId());
        refundRequest.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        refundRequest.setRefundType(refundType.name());
        refundRequest.setReason(reason);
        refundRequest.setStatus(RefundRequestStatus.PENDING_APPROVAL.name());
        refundRequest.setRequestedBy(userId);
        refundRequest.setRequestedTime(now);
        refundRequest.setCreateBy(userId);
        refundRequest.setCreateTime(now);
        if (refundRequestMapper.insertSelective(refundRequest) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "退款申请创建失败");
        }
        auditRecorder.record(AuditActionEnum.PAYMENT_REFUND_REQUEST, String.valueOf(refundRequest.getId()));
        clearTransactionCache(original.getTranId());
        return refundRequest;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TRefundRequest approveRefundRequest(Integer requestId, Boolean approved, String comment) {
        if (approved == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "审批结果不能为空");
        }
        TRefundRequest request = refundRequestMapper.selectByPrimaryKeyForUpdate(requestId);
        if (request == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "退款申请不存在");
        }
        requireAccessibleTransaction(request.getTranId());
        String approveComment = approved ? normalizeBlank(comment) : requireNonBlank(comment, "驳回原因不能为空");
        String newStatus = approved
                ? RefundRequestStatus.APPROVED.name()
                : RefundRequestStatus.REJECTED.name();
        Integer userId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        int rows = refundRequestMapper.updateApprovalIfPending(requestId, newStatus, userId,
                now, approveComment, userId, now);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款申请状态已变更，请刷新后重试");
        }
        request.setStatus(newStatus);
        request.setApprovedBy(userId);
        request.setApprovedTime(now);
        request.setApproveComment(approveComment);
        request.setEditBy(userId);
        request.setEditTime(now);
        auditRecorder.record(AuditActionEnum.PAYMENT_REFUND_APPROVE, String.valueOf(requestId));
        clearTransactionCache(request.getTranId());
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPayment executeRefundRequest(Integer requestId, String transactionRef, String remark) {
        TRefundRequest request = refundRequestMapper.selectByPrimaryKeyForUpdate(requestId);
        if (request == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "退款申请不存在");
        }
        if (!RefundRequestStatus.APPROVED.name().equals(request.getStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有已审批退款申请可以执行");
        }

        TPayment original = paymentMapper.selectByPrimaryKeyForUpdate(request.getOriginalPaymentId());
        if (original == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "原收款记录不存在");
        }
        if (!PaymentStatus.COMPLETED.name().equals(original.getPaymentStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "原收款状态不允许执行退款");
        }

        requireAccessibleTransaction(request.getTranId());
        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(request.getTranId());
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易记录不存在");
        }
        if (tran.getStage() != TranStage.PAYMENT && tran.getStage() != TranStage.DELIVERY) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许执行退款");
        }

        BigDecimal refundable = calculateAvailableRefundAmount(original.getId(), original.getAmount(), false);
        if (request.getAmount().compareTo(refundable) > 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "退款金额不能超过当前可退金额");
        }

        String normalizedRef = normalizeBlank(transactionRef);
        if (normalizedRef != null) {
            TPayment sameRefPayment = paymentMapper.selectByTransactionRef(normalizedRef);
            if (sameRefPayment != null) {
                throw new BusinessException(CodeEnum.DUPLICATE, "退款参考号已存在");
            }
        }

        Integer userId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        TPayment refund = new TPayment();
        refund.setTranId(original.getTranId());
        refund.setAmount(request.getAmount().negate());
        refund.setPaymentMethod(original.getPaymentMethod());
        refund.setPaymentType(PaymentType.REFUND.name());
        refund.setPaymentStatus(PaymentStatus.COMPLETED.name());
        refund.setPaymentTime(now);
        refund.setPaymentNo(generatePaymentNo());
        refund.setTransactionRef(normalizedRef);
        refund.setCreateTime(now);
        refund.setCreateBy(userId);
        refund.setRemark(buildRefundRemark(request, remark));
        if (paymentMapper.insertSelective(refund) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "退款记录创建失败");
        }

        int executed = refundRequestMapper.markExecutedIfApproved(requestId, refund.getId(), userId, now, userId, now);
        if (executed != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款申请状态已变更，请刷新后重试");
        }

        BigDecimal remainingForOriginal = refundable.subtract(request.getAmount());
        if (remainingForOriginal.compareTo(BigDecimal.ZERO) == 0) {
            int marked = paymentMapper.markRefundedIfCompleted(original.getId(), now, userId);
            if (marked != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "原收款状态已变更，请刷新后重试");
            }
        }

        reconcileTransactionAfterRefund(tran, request, userId);

        auditRecorder.record(AuditActionEnum.PAYMENT_REFUND, String.valueOf(requestId));
        clearTransactionCache(original.getTranId());
        return refund;
    }

    @Override
    public List<TPayment> getTransactionPayments(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return paymentMapper.selectByTranId(tranId);
    }

    @Override
    public List<TRefundRequest> getTransactionRefundRequests(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return refundRequestMapper.selectByTranId(tranId);
    }

    private List<TPayment> normalizePayments(List<TPayment> payments) {
        return payments == null ? List.of() : payments;
    }

    private List<TPayment> mergePaymentSnapshot(List<TPayment> payments, TPayment current) {
        List<TPayment> merged = new ArrayList<>(normalizePayments(payments));
        if (current == null || current.getId() == null) {
            return merged;
        }
        for (int i = 0; i < merged.size(); i++) {
            if (current.getId().equals(merged.get(i).getId())) {
                merged.set(i, current);
                return merged;
            }
        }
        merged.add(current);
        return merged;
    }

    private BigDecimal calculateNetConfirmedPaid(List<TPayment> payments) {
        return normalizePayments(payments).stream()
                .filter(payment -> payment.getAmount() != null)
                .filter(payment -> PaymentStatus.COMPLETED.name().equals(payment.getPaymentStatus())
                        || (PaymentStatus.REFUNDED.name().equals(payment.getPaymentStatus())
                            && !PaymentType.REFUND.name().equals(payment.getPaymentType())))
                .map(TPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAvailableRefundAmount(Integer originalPaymentId,
                                                       BigDecimal originalAmount,
                                                       boolean includeOpenRequests) {
        BigDecimal executed = refundRequestMapper.sumExecutedAmountByOriginalPaymentId(originalPaymentId);
        BigDecimal open = includeOpenRequests
                ? refundRequestMapper.sumOpenAmountByOriginalPaymentId(originalPaymentId)
                : BigDecimal.ZERO;
        BigDecimal used = nullToZero(executed).add(nullToZero(open));
        return nullToZero(originalAmount).subtract(used).setScale(2, RoundingMode.HALF_UP);
    }

    private void reconcileTransactionAfterRefund(TTran tran, TRefundRequest request, Integer userId) {
        List<TPayment> payments = paymentMapper.selectByTranId(tran.getId());
        BigDecimal netPaid = calculateNetConfirmedPaid(payments);
        RefundType refundType = parseRefundType(request.getRefundType());
        if (refundType == RefundType.ORDER_CANCEL && netPaid.compareTo(BigDecimal.ZERO) <= 0) {
            int stageRows = tranMapper.updateStageAtomic(tran.getId(),
                    TranStage.CANCELLED, tran.getStage(), userId);
            if (stageRows != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不允许取消");
            }
            restoreTransactionStock(tran.getId());
            writeHistory(tran.getId(), TranStage.CANCELLED,
                    tran.getMoney(), tran.getExpectedDate(), userId);
            return;
        }

        if (refundType != RefundType.ORDER_CANCEL
                && tran.getStage() == TranStage.DELIVERY
                && netPaid.compareTo(tran.getMoney()) < 0) {
            int stageRows = tranMapper.updateStageAtomic(tran.getId(),
                    TranStage.PAYMENT, TranStage.DELIVERY, userId);
            if (stageRows != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不允许回退到待收款");
            }
            writeHistory(tran.getId(), TranStage.PAYMENT,
                    tran.getMoney(), tran.getExpectedDate(), userId);
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireNonBlank(String value, String message) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        return normalized;
    }

    private String mergeRemark(String originalRemark, String comment) {
        String normalizedComment = normalizeBlank(comment);
        if (normalizedComment == null) {
            return originalRemark;
        }
        String normalizedRemark = normalizeBlank(originalRemark);
        return normalizedRemark == null
                ? normalizedComment
                : normalizedRemark + "；" + normalizedComment;
    }

    private RefundType parseRefundType(String refundType) {
        try {
            return RefundType.valueOf(requireNonBlank(refundType, "退款类型不能为空"));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "不支持的退款类型: " + refundType);
        }
    }

    private String buildRefundRemark(TRefundRequest request, String executeRemark) {
        StringBuilder remark = new StringBuilder();
        remark.append("退款原因: ").append(request.getReason())
                .append("；原收款ID: ").append(request.getOriginalPaymentId());
        String normalizedExecuteRemark = normalizeBlank(executeRemark);
        if (normalizedExecuteRemark != null) {
            remark.append("；执行备注: ").append(normalizedExecuteRemark);
        }
        return remark.toString();
    }

    private void restoreTransactionStock(Integer tranId) {
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products == null) {
            return;
        }
        for (TTranProduct product : products) {
            if (productMapper.updateStock(
                    product.getProductId().longValue(), product.getQuantity()) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "恢复产品库存失败: " + product.getProductId());
            }
        }
    }

    private void deductTransactionStock(Integer tranId) {
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products == null) {
            return;
        }
        for (TTranProduct product : products) {
            if (productMapper.updateStock(
                    product.getProductId().longValue(), -product.getQuantity()) != 1) {
                throw new BusinessException(CodeEnum.OPERATION_FAILED, "产品库存不足: " + product.getProductId());
            }
        }
    }

    private void validateTransactionProduct(TTranProduct product) {
        if (product == null || product.getProductId() == null
                || product.getQuantity() == null || product.getQuantity() <= 0
                || product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("交易商品、正数数量和非负价格不能为空");
        }
    }

    private TTran findAccessibleTransaction(Integer tranId) {
        CurrentUserProvider.TransactionDataScope scope = currentUserProvider.getTransactionDataScope();
        return scope.isAll()
                ? tranMapper.selectByPrimaryKey(tranId)
                : tranMapper.selectScopedById(tranId, scope.getSelfUserId(),
                        scope.isApprovalScope(), scope.getFinanceStages());
    }

    private TTran requireAccessibleTransaction(Integer tranId) {
        TTran transaction = findAccessibleTransaction(tranId);
        if (transaction == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在或无权访问");
        }
        return transaction;
    }

    private void writeHistory(Integer tranId, TranStage stage, BigDecimal money, Date expectedDate, Integer userId) {
        TTranHistory history = new TTranHistory();
        history.setTranId(tranId);
        history.setStage(stage.name());
        history.setMoney(money);
        history.setExpectedDate(expectedDate);
        history.setCreateTime(new Date());
        history.setCreateBy(userId);
        if (tranHistoryMapper.insert(history) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "交易历史记录创建失败");
        }
    }

    private void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("支付方式不能为空");
        }
        try {
            PaymentMethod.valueOf(paymentMethod);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的支付方式: " + paymentMethod);
        }
    }

    private void validatePaymentType(String paymentType) {
        if (paymentType == null) {
            throw new IllegalArgumentException("支付类型不能为空");
        }
        PaymentType type;
        try {
            type = PaymentType.valueOf(paymentType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的支付类型: " + paymentType);
        }
        if (type == PaymentType.REFUND) {
            throw new IllegalArgumentException("普通收款不能使用退款类型");
        }
    }

    private String generatePaymentNo() {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String nanoStr = String.format("%010d", Math.abs(System.nanoTime() % 10000000000L));
        return "PAY" + dateStr + nanoStr;
    }

    private String generateTranNo() {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String nanoStr = String.format("%010d", Math.abs(System.nanoTime() % 10000000000L));
        return "TN" + dateStr + nanoStr;
    }

    private String generateInvoiceNo() {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String nanoStr = String.format("%010d", Math.abs(System.nanoTime() % 10000000000L));
        return "INV" + dateStr + nanoStr;
    }

    private void clearTransactionCache(Integer tranId) {
        redisManager.delete(Constants.CACHE_KEY_TRAN + tranId);
        redisManager.deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        redisManager.delete(Constants.CACHE_KEY_TRAN_INVOICES + tranId);
        redisManager.delete("cdrm:tran:payments:" + tranId);
    }
}
