package com.autodealer.crm.modules.fulfillment.transaction.application.internal;

import com.autodealer.crm.modules.fulfillment.delivery.application.api.port.DeliveryDataPort;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.port.InvoiceDataPort;
import com.autodealer.crm.modules.fulfillment.payment.application.api.port.RefundDataPort;
import com.autodealer.crm.modules.fulfillment.payment.application.api.port.PaymentDataPort;
import com.autodealer.crm.modules.commerce.inventory.application.api.port.VehicleInventoryDataPort;
import com.autodealer.crm.modules.commerce.inventory.application.api.port.StockRecordDataPort;
import com.autodealer.crm.modules.commerce.catalog.application.api.port.ProductCatalogDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductStockRecord;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductVehicle;
import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotion;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDelivery;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.model.TTranInvoice;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TPayment;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TRefundRequest;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranApproveMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranHistoryMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranProductMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranRemarkMapper;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranApprove;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranHistory;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranProduct;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranRemark;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.PaymentMethod;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.PaymentStatus;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.PaymentType;
import com.autodealer.crm.modules.commerce.inventory.application.api.enums.ProductVehicleStatus;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.RefundRequestStatus;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.RefundType;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.query.TranQuery;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.TranService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.commerce.promotion.application.api.dto.PromotionProductLine;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.SettlementPreviewResponse;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.SettleRequest;
import com.autodealer.crm.modules.commerce.promotion.application.api.ProductPromotionService;
import com.autodealer.crm.shared.infrastructure.json.JSONUtils;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.TransactionCompletionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private InvoiceDataPort tranInvoiceMapper;

    @Resource
    private TTranApproveMapper tranApproveMapper;

    @Resource
    private ProductCatalogDataPort productMapper;

    @Resource
    private VehicleInventoryDataPort productVehicleMapper;

    @Resource
    private CustomerDataPort customerMapper;

    @Resource
    private RedisManager redisManager;

    @Resource
    private PaymentDataPort paymentMapper;

    @Resource
    private RefundDataPort refundRequestMapper;

    @Resource
    private DeliveryDataPort deliveryMapper;

    @Resource
    private StockRecordDataPort stockRecordMapper;

    @Resource
    private TTranHistoryMapper tranHistoryMapper;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Resource
    private ProductPromotionService promotionService;

    @Resource
    private TransactionCompletionService transactionCompletionService;

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
        requireAccessibleCustomer(tTran.getCustomerId());
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
                if (!"ON_SALE".equals(dbProduct.getStatus())) {
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
                fillProductSnapshot(product, dbProduct);
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

        // 阶段2：插入报价商品行项。报价不是订单，不能在此扣减或占用库存。
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                product.setCreateBy(operatorId);
                product.setCreateTime(now);
                if (tranProductMapper.insertSelective(product) != 1) {
                    throw new BusinessException(CodeEnum.FAIL,
                            "交易商品创建失败: " + product.getProductId());
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

        validateCustomerRebind(tTran.getCustomerId(), existing.getCustomerId());
        sanitizeTransactionUpdate(tTran);
        tTran.setEditBy(currentUserProvider.getCurrentUserId());
        tTran.setEditTime(new Date());
        int rows = updateTransactionScoped(tTran);

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
        if (calculated.getPromotionId() != null) {
            promotionService.reserveUsage(calculated.getPromotionId(),
                    calculated.getDiscountAmount(), "TRAN", tranId.longValue());
        }
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
            List<Long> productIds = products.stream()
                    .map(TTranProduct::getProductId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            promotion = promotionService.requireApplicablePromotion(promotionId, productIds);
            discountAmount = promotionService.calculateDiscount(toPromotionLines(products), promotion);
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
            info.setCode(promotion.getCode());
            info.setName(promotion.getName());
            info.setType(promotion.getType());
            info.setDiscount(promotion.getDiscount());
            info.setRuleSummary(promotion.getRuleSummary());
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

    private BigDecimal calculateOriginalAmount(List<TTranProduct> products) {
        BigDecimal total = BigDecimal.ZERO;
        for (TTranProduct p : products) {
            validateSettlementProduct(p);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
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

    private List<PromotionProductLine> toPromotionLines(List<TTranProduct> products) {
        return products.stream()
                .peek(this::validateSettlementProduct)
                .map(product -> new PromotionProductLine(
                        product.getProductId(), product.getPrice(), product.getQuantity()))
                .toList();
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
        String cacheKey = RedisKeys.transactionProducts(tranId);
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
        String cacheKey = RedisKeys.transactionInvoices(tranId);
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
        tranProductMapper.deleteByTranId(tranId);
        redisManager.delete(RedisKeys.transactionProducts(tranId));
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
                if (!"ON_SALE".equals(dbProduct.getStatus())) {
                    throw new BusinessException(CodeEnum.OPERATION_FAILED,
                            "商品 [" + dbProduct.getName() + "] 当前不可销售");
                }
                if (dbProduct.getPrice() == null
                        || dbProduct.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(CodeEnum.PARAM_ERROR,
                            "商品 [" + dbProduct.getName() + "] 价格无效");
                }
                product.setPrice(dbProduct.getPrice());
                fillProductSnapshot(product, dbProduct);
                product.setTranId(tranId);
                product.setCreateBy(operatorId);
                product.setCreateTime(new Date());
                if (tranProductMapper.insertSelective(product) != 1) {
                    throw new BusinessException(CodeEnum.FAIL,
                            "交易商品创建失败: " + product.getProductId());
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
        if (updateTransactionScoped(moneyUpdate) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "交易金额更新失败");
        }

        redisManager.delete(RedisKeys.transactionProducts(tranId));
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

        requireAccessibleTransaction(invoice.getTranId());
        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(invoice.getTranId());
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        invoice.setCreateBy(operatorId);
        invoice.setEditBy(operatorId);
        if (invoice.getAmount() == null || tran.getMoney() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "发票金额和交易金额不能为空");
        }
        if (invoice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "发票金额必须大于0");
        }
        List<TTranInvoice> existingInvoices = tranInvoiceMapper.selectByTranId(invoice.getTranId());
        BigDecimal availableAmount = calculateAvailableInvoiceAmount(tran, existingInvoices);
        if (invoice.getAmount().compareTo(availableAmount) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "发票金额不能超过当前可开票余额",
                    Map.of("availableAmount", availableAmount));
        }
        if (!canCreateInvoice(tran.getStage())) {
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
        List<TTranInvoice> invoices = tranInvoiceMapper.selectByTranId(tranId);
        if (currentUserProvider.isAdmin() || currentUserProvider.hasAuthority(PermissionCodes.TRAN_INVOICE_SENSITIVE)) {
            return invoices;
        }
        return normalizeInvoices(invoices).stream()
                .map(this::maskInvoiceSensitiveFields)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTranInvoiceStatus(Integer invoiceId, String status, String reason) {
        if (!"ISSUED".equals(status) && !"FAILED".equals(status) && !"VOIDED".equals(status)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "不支持的发票状态: " + status);
        }

        TTranInvoice currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (currentInvoice == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "发票不存在");
        }
        requireAccessibleTransaction(currentInvoice.getTranId());
        TTran lockedTran = tranMapper.selectByPrimaryKeyForUpdate(currentInvoice.getTranId());
        if (lockedTran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (currentInvoice == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "发票不存在");
        }
        Integer updateBy = currentUserProvider.getCurrentUserId();
        boolean validTransition = ("PENDING".equals(currentInvoice.getStatus())
                    && ("ISSUED".equals(status) || "FAILED".equals(status) || "VOIDED".equals(status)))
                || ("ISSUED".equals(currentInvoice.getStatus()) && "VOIDED".equals(status));
        if (!validTransition || currentInvoice.getStatus().equals(status)) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前发票状态不允许此操作");
        }
        String updateRemark = currentInvoice.getRemark();
        if ("FAILED".equals(status) || "VOIDED".equals(status)) {
            updateRemark = mergeRemark(currentInvoice.getRemark(), requireNonBlank(reason, "发票失败或作废原因不能为空"));
        }

        Date now = new Date();
        int result = tranInvoiceMapper.updateStatusIfCurrent(invoiceId, currentInvoice.getStatus(),
                status, "ISSUED".equals(status) ? now : currentInvoice.getIssueTime(), updateRemark, now, updateBy);
        if (result != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "发票状态已变更，请刷新后重试");
        }

        auditRecorder.record(AuditActionEnum.INVOICE_STATUS, String.valueOf(invoiceId));
        if ("ISSUED".equals(status)) {
            transactionCompletionService.tryComplete(currentInvoice.getTranId(), updateBy);
        }
        clearTransactionCache(currentInvoice.getTranId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTranInvoice redReverseInvoice(Integer invoiceId, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "红冲金额必须大于0");
        }
        String checkedReason = requireNonBlank(reason, "红冲原因不能为空");

        TTranInvoice originalInvoice = requireInvoiceForMutation(invoiceId);
        if (!"ISSUED".equals(originalInvoice.getStatus())
                && !"PARTIAL_RED_REVERSED".equals(originalInvoice.getStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前发票状态不允许红冲");
        }
        if (originalInvoice.getAmount() == null || originalInvoice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只能对正数原票发起红冲");
        }

        List<TTranInvoice> invoices = tranInvoiceMapper.selectByTranId(originalInvoice.getTranId());
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal reversedAmount = calculateCompletedRedReversalAmount(originalInvoice.getId(), invoices);
        BigDecimal remainingAmount = originalInvoice.getAmount().abs().subtract(reversedAmount).setScale(2, RoundingMode.HALF_UP);
        if (normalizedAmount.compareTo(remainingAmount) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "红冲金额不能超过原票剩余可红冲金额",
                    Map.of("availableAmount", remainingAmount));
        }

        Integer operatorId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        TTranInvoice redInvoice = copyInvoiceForRedReverse(originalInvoice, normalizedAmount, checkedReason, operatorId, now);
        int inserted = tranInvoiceMapper.insertSelective(redInvoice);
        if (inserted != 1) {
            throw new BusinessException(CodeEnum.FAIL, "红冲发票创建失败");
        }

        BigDecimal totalReversed = reversedAmount.add(normalizedAmount).setScale(2, RoundingMode.HALF_UP);
        String newOriginalStatus = totalReversed.compareTo(originalInvoice.getAmount().abs()) >= 0
                ? "RED_REVERSED" : "PARTIAL_RED_REVERSED";
        String updateRemark = mergeRemark(originalInvoice.getRemark(), "红冲原因：" + checkedReason);
        int updated = tranInvoiceMapper.updateStatusIfCurrent(originalInvoice.getId(), originalInvoice.getStatus(),
                newOriginalStatus, originalInvoice.getIssueTime(), updateRemark, now, operatorId);
        if (updated != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "发票状态已变更，请刷新后重试");
        }

        auditRecorder.record(AuditActionEnum.INVOICE_RED_REVERSE, String.valueOf(invoiceId));
        clearTransactionCache(originalInvoice.getTranId());
        return redInvoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTranInvoice reissueInvoice(Integer invoiceId, TTranInvoice invoice, String reason) {
        if (invoice == null || invoice.getAmount() == null || invoice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "重开发票金额必须大于0");
        }
        String checkedReason = requireNonBlank(reason, "重开原因不能为空");
        TTranInvoice sourceInvoice = requireInvoiceForMutation(invoiceId);
        if (!"VOIDED".equals(sourceInvoice.getStatus())
                && !"RED_REVERSED".equals(sourceInvoice.getStatus())
                && !"PARTIAL_RED_REVERSED".equals(sourceInvoice.getStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前发票状态不允许重开");
        }

        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(sourceInvoice.getTranId());
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        List<TTranInvoice> invoices = tranInvoiceMapper.selectByTranId(sourceInvoice.getTranId());
        BigDecimal availableAmount = calculateAvailableInvoiceAmount(tran, invoices);
        BigDecimal reissueAmount = invoice.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (reissueAmount.compareTo(availableAmount) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "重开发票金额不能超过当前可开票余额",
                    Map.of("availableAmount", availableAmount));
        }

        Integer operatorId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        TTranInvoice reissueInvoice = copyInvoiceForReissue(sourceInvoice, invoice, checkedReason, operatorId, now);
        int inserted = tranInvoiceMapper.insertSelective(reissueInvoice);
        if (inserted != 1) {
            throw new BusinessException(CodeEnum.FAIL, "重开发票创建失败");
        }

        auditRecorder.record(AuditActionEnum.INVOICE_REISSUE, String.valueOf(invoiceId));
        clearTransactionCache(sourceInvoice.getTranId());
        return reissueInvoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTransaction(Integer id, String reason) {
        return transitionTransactionTerminal(id, TranStage.CANCELLED, requireNonBlank(reason, "取消原因不能为空"),
                AuditActionEnum.TRAN_CANCEL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeTransaction(Integer id, String reason) {
        return transitionTransactionTerminal(id, TranStage.CLOSED, requireNonBlank(reason, "关闭原因不能为空"),
                AuditActionEnum.TRAN_CLOSE);
    }

    private boolean transitionTransactionTerminal(Integer id, TranStage targetStage,
                                                  String reason, AuditActionEnum action) {
        requireAccessibleTransaction(id);
        TTran transaction = tranMapper.selectByPrimaryKeyForUpdate(id);
        if (transaction == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        if (transaction.getStage() == targetStage) {
            return true;
        }
        if (transaction.getStage() == TranStage.COMPLETED
                || transaction.getStage() == TranStage.CANCELLED
                || transaction.getStage() == TranStage.CLOSED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易终态不允许变更");
        }
        validateTerminalTransitionBlockers(transaction, targetStage);
        Integer userId = currentUserProvider.getCurrentUserId();
        if (targetStage == TranStage.CANCELLED) {
            releaseReservedVehicleForTransactionCancel(transaction, reason, userId);
        }
        int rows = tranMapper.updateStageAtomic(id, targetStage, transaction.getStage(), userId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态已变更，请刷新后重试");
        }
        writeHistory(id, targetStage, transaction.getMoney(), transaction.getExpectedDate(), userId, reason);
        auditRecorder.record(action, String.valueOf(id));
        clearTransactionCache(id);
        return true;
    }

    private void releaseReservedVehicleForTransactionCancel(TTran transaction, String reason, Integer userId) {
        TProductVehicle vehicle = productVehicleMapper.selectActiveBySource("ORDER", transaction.getId().longValue());
        if (vehicle == null) {
            return;
        }
        ProductVehicleStatus currentStatus = ProductVehicleStatus.parse(vehicle.getStatus());
        if (currentStatus != ProductVehicleStatus.ORDER_RESERVED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易库存占用状态不允许取消释放");
        }
        TProductStockRecord reserveRecord = stockRecordMapper.selectLatestReserveByVehicle(
                vehicle.getId(), "ORDER", transaction.getId().longValue());
        if (reserveRecord == null) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "缺少原订单库存占用流水，不能取消交易");
        }
        if (stockRecordMapper.selectReleaseByRelatedRecordId(reserveRecord.getId()) != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (productVehicleMapper.releaseIfCurrent(vehicle.getId(), currentStatus.name(), now, userId) != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "库存车辆状态已变更，请刷新后重试");
        }
        if (productMapper.updateStock(vehicle.getProductId(), 1) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "商品库存汇总恢复失败");
        }
        TProductStockRecord releaseRecord = new TProductStockRecord();
        releaseRecord.setProductId(vehicle.getProductId());
        releaseRecord.setVehicleId(vehicle.getId());
        releaseRecord.setQuantity(1);
        releaseRecord.setType("RELEASE");
        releaseRecord.setSourceType(reserveRecord.getSourceType());
        releaseRecord.setSourceId(reserveRecord.getSourceId());
        releaseRecord.setBeforeStatus(currentStatus.name());
        releaseRecord.setAfterStatus(ProductVehicleStatus.AVAILABLE.name());
        releaseRecord.setRelatedRecordId(reserveRecord.getId());
        releaseRecord.setRemark("取消交易：" + reason);
        releaseRecord.setCreateTime(now);
        releaseRecord.setCreateBy(userId);
        if (stockRecordMapper.insert(releaseRecord) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "交易取消释放流水创建失败");
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_RELEASE, String.valueOf(vehicle.getId()));
    }

    private void validateTerminalTransitionBlockers(TTran transaction, TranStage targetStage) {
        List<TPayment> payments = paymentMapper.selectByTranId(transaction.getId());
        BigDecimal netPaid = calculateNetConfirmedPaid(payments);
        boolean hasPendingReceipt = normalizePayments(payments).stream()
                .anyMatch(payment -> PaymentStatus.PENDING.name().equals(payment.getPaymentStatus()));
        if (netPaid.compareTo(BigDecimal.ZERO) > 0 || hasPendingReceipt) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易存在收款或待确认收款，请先完成退款或退回处理");
        }

        List<TRefundRequest> refunds = refundRequestMapper.selectByTranId(transaction.getId());
        boolean hasOpenRefund = normalizeRefundRequests(refunds).stream()
                .anyMatch(request -> RefundRequestStatus.PENDING_APPROVAL.name().equals(request.getStatus())
                        || RefundRequestStatus.PENDING_EXECUTION.name().equals(request.getStatus())
                        || RefundRequestStatus.EXECUTING.name().equals(request.getStatus()));
        if (hasOpenRefund) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易存在退款处理中事项");
        }

        List<TTranInvoice> invoices = tranInvoiceMapper.selectByTranId(transaction.getId());
        boolean hasBlockingInvoice = normalizeInvoices(invoices).stream()
                .anyMatch(invoice -> "PENDING".equals(invoice.getStatus())
                        || "ISSUING".equals(invoice.getStatus())
                        || "ISSUED".equals(invoice.getStatus())
                        || "PARTIAL_RED_REVERSED".equals(invoice.getStatus()));
        if (hasBlockingInvoice) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易存在未处理完成的发票事实");
        }

        List<TDelivery> deliveries = normalizeDeliveries(deliveryMapper.selectByTranId(transaction.getId()));
        boolean hasDeliveredVehicle = deliveries.stream()
                .anyMatch(delivery -> "COMPLETED".equals(delivery.getStatus())
                        || stockRecordMapper.selectOutboundByDelivery(delivery.getId()) != null);
        if (hasDeliveredVehicle) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已出库或已签收，请走售后纠错流程");
        }

        if (targetStage == TranStage.CLOSED && transaction.getStage() == TranStage.DELIVERY) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "待交付交易不能直接关闭，请按取消或交付异常流程处理");
        }
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

        validateCustomerRebind(tran.getCustomerId(), existing.getCustomerId());
        sanitizeTransactionUpdate(tran);
        tran.setEditBy(currentUserProvider.getCurrentUserId());
        tran.setEditTime(new Date());
        int rows = updateTransactionScoped(tran);
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
        if (!canRecordPayment(tran.getStage())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许收款");
        }
        if (tran.getMoney() == null || tran.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易金额无效，无法收款");
        }

        String transactionRef = normalizeBlank(payment.getTransactionRef());
        if (transactionRef == null && requiresExternalTransactionRef(payment.getPaymentMethod())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "该支付方式必须填写外部支付参考号");
        }
        payment.setTransactionRef(transactionRef);

        List<TPayment> existingPayments = paymentMapper.selectByTranId(payment.getTranId());
        BigDecimal paidAmount = calculateNetConfirmedPaid(existingPayments);
        BigDecimal remaining = tran.getMoney().subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易已收齐，无需再次登记收款");
        }

        String idempotencyKey = buildPaymentIdempotencyKey(
                payment.getTranId(), payment.getPaymentMethod(), transactionRef, remaining);
        payment.setIdempotencyKey(idempotencyKey);

        if (transactionRef != null) {
            TPayment sameRefPayment = paymentMapper.selectByTransactionRef(transactionRef);
            if (sameRefPayment != null) {
                return requireSamePaymentRequest(sameRefPayment, payment, remaining);
            }
        }

        TPayment sameIdempotencyPayment = findPaymentByIdempotency(existingPayments, idempotencyKey);
        if (sameIdempotencyPayment == null) {
            sameIdempotencyPayment = paymentMapper.selectByIdempotencyKey(idempotencyKey);
        }
        if (sameIdempotencyPayment != null) {
            return requireSamePaymentRequest(sameIdempotencyPayment, payment, remaining);
        }

        boolean hasPendingReceipt = normalizePayments(existingPayments).stream()
                .anyMatch(existing -> PaymentStatus.PENDING.name().equals(existing.getPaymentStatus())
                        && !PaymentType.REFUND.name().equals(existing.getPaymentType()));
        if (hasPendingReceipt) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "存在待确认收款，请先确认或退回后再登记");
        }

        Date now = new Date();
        payment.setPaymentNo(generatePaymentNo());
        payment.setAmount(remaining);
        payment.setPaymentType(paidAmount.compareTo(BigDecimal.ZERO) > 0
                ? PaymentType.BALANCE.name() : PaymentType.FULL.name());
        payment.setPaymentStatus(PaymentStatus.PENDING.name());
        payment.setPaymentTime(null);
        payment.setCreateTime(now);

        try {
            if (paymentMapper.insertSelective(payment) != 1) {
                throw new BusinessException(CodeEnum.FAIL, "收款记录创建失败");
            }
        } catch (DuplicateKeyException ex) {
            TPayment existing = paymentMapper.selectByIdempotencyKey(idempotencyKey);
            if (existing == null && transactionRef != null) {
                existing = paymentMapper.selectByTransactionRef(transactionRef);
            }
            if (existing != null) {
                return requireSamePaymentRequest(existing, payment, remaining);
            }
            throw new BusinessException(CodeEnum.DUPLICATE, "收款幂等键已存在", ex);
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
        if (approved && !canRecordPayment(tran.getStage())) {
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
                        TranStage.DELIVERY, tran.getStage(), userId);
                if (stageRows != 1) {
                    throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不允许进入待交付");
                }
                writeHistory(payment.getTranId(), TranStage.DELIVERY,
                        tran.getMoney(), tran.getExpectedDate(), userId);
            }
            transactionCompletionService.tryComplete(payment.getTranId(), userId);
            auditRecorder.record(AuditActionEnum.PAYMENT_CONFIRM, String.valueOf(paymentId));
        } else {
            auditRecorder.record(AuditActionEnum.PAYMENT_REJECT, String.valueOf(paymentId));
        }

        clearTransactionCache(payment.getTranId());
        return payment;
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
        if (!canProcessRefund(tran.getStage())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许申请退款");
        }

        BigDecimal refundable = calculateAvailableRefundAmount(original.getId(), original.getAmount(), true);
        if (request.getAmount().compareTo(refundable) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款金额不能超过可退金额",
                    Map.of("availableAmount", refundable));
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
        Integer userId = currentUserProvider.getCurrentUserId();
        if (approved && userId.equals(request.getRequestedBy())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "退款申请人不能审批自己的退款");
        }
        String approveComment = approved ? normalizeBlank(comment) : requireNonBlank(comment, "驳回原因不能为空");
        String newStatus = approved
                ? RefundRequestStatus.PENDING_EXECUTION.name()
                : RefundRequestStatus.REJECTED.name();
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
    public TRefundRequest executeRefundRequest(Integer requestId, String transactionRef, String remark,
                                               Boolean success, String failureReason) {
        TRefundRequest request = refundRequestMapper.selectByPrimaryKeyForUpdate(requestId);
        if (request == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "退款申请不存在");
        }
        if (RefundRequestStatus.COMPLETED.name().equals(request.getStatus())
                || RefundRequestStatus.FAILED.name().equals(request.getStatus())
                || RefundRequestStatus.REJECTED.name().equals(request.getStatus())
                || RefundRequestStatus.CANCELLED.name().equals(request.getStatus())) {
            return request;
        }
        if (!RefundRequestStatus.PENDING_EXECUTION.name().equals(request.getStatus())
                && !RefundRequestStatus.EXECUTING.name().equals(request.getStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有待执行退款申请可以执行");
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
        if (!canProcessRefund(tran.getStage())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前交易状态不允许执行退款");
        }

        BigDecimal refundable = calculateAvailableRefundAmount(original.getId(), original.getAmount(), false);
        if (request.getAmount().compareTo(refundable) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款金额不能超过当前可退金额",
                    Map.of("availableAmount", refundable));
        }

        String normalizedRef = normalizeBlank(transactionRef);
        String normalizedRemark = normalizeBlank(remark);
        if (normalizedRef != null) {
            TPayment sameRefPayment = paymentMapper.selectByTransactionRef(normalizedRef);
            if (sameRefPayment != null) {
                throw new BusinessException(CodeEnum.DUPLICATE, "退款参考号已存在");
            }
        }

        Integer userId = currentUserProvider.getCurrentUserId();
        Date now = new Date();
        boolean executionSuccess = success == null || success;
        if (!executionSuccess) {
            String requiredFailureReason = requireNonBlank(failureReason, "退款执行失败原因不能为空");
            int failed = refundRequestMapper.markFailedIfExecutable(requestId, userId, now,
                    requiredFailureReason, normalizedRef, normalizedRemark, userId, now);
            if (failed != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款申请状态已变更，请刷新后重试");
            }
            request.setStatus(RefundRequestStatus.FAILED.name());
            request.setExecutedBy(userId);
            request.setExecutedTime(now);
            request.setExecutionRef(normalizedRef);
            request.setExecutionRemark(normalizedRemark);
            request.setFailureReason(requiredFailureReason);
            request.setEditBy(userId);
            request.setEditTime(now);
            auditRecorder.record(AuditActionEnum.PAYMENT_REFUND_FAILED, String.valueOf(requestId));
            clearTransactionCache(original.getTranId());
            return request;
        }

        if (RefundRequestStatus.PENDING_EXECUTION.name().equals(request.getStatus())) {
            int executing = refundRequestMapper.markExecutingIfPendingExecution(requestId, userId, now,
                    normalizedRef, normalizedRemark, userId, now);
            if (executing != 1) {
                throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款申请状态已变更，请刷新后重试");
            }
            request.setStatus(RefundRequestStatus.EXECUTING.name());
            request.setExecutedBy(userId);
            request.setExecutionStartedTime(now);
            request.setExecutionRef(normalizedRef);
            request.setExecutionRemark(normalizedRemark);
            request.setEditBy(userId);
            request.setEditTime(now);
        }

        TPayment refund = new TPayment();
        refund.setTranId(original.getTranId());
        refund.setAmount(request.getAmount().negate());
        refund.setPaymentMethod(original.getPaymentMethod());
        refund.setPaymentType(PaymentType.REFUND.name());
        refund.setPaymentStatus(PaymentStatus.COMPLETED.name());
        refund.setPaymentTime(now);
        refund.setPaymentNo(generatePaymentNo());
        refund.setTransactionRef(normalizedRef);
        refund.setIdempotencyKey(buildRefundIdempotencyKey(requestId, original.getPaymentMethod(), normalizedRef));
        refund.setCreateTime(now);
        refund.setCreateBy(userId);
        refund.setRemark(buildRefundRemark(request, normalizedRemark));
        if (paymentMapper.insertSelective(refund) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "退款记录创建失败");
        }

        int executed = refundRequestMapper.markCompletedIfExecuting(requestId, refund.getId(), now, userId, now);
        if (executed != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "退款申请状态已变更，请刷新后重试");
        }
        request.setStatus(RefundRequestStatus.COMPLETED.name());
        request.setRefundPaymentId(refund.getId());
        request.setExecutedTime(now);
        request.setEditBy(userId);
        request.setEditTime(now);

        reconcileTransactionAfterRefund(tran, request, userId);

        auditRecorder.record(AuditActionEnum.PAYMENT_REFUND, String.valueOf(requestId));
        clearTransactionCache(original.getTranId());
        return request;
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

    private List<TRefundRequest> normalizeRefundRequests(List<TRefundRequest> requests) {
        return requests == null ? List.of() : requests;
    }

    private List<TDelivery> normalizeDeliveries(List<TDelivery> deliveries) {
        return deliveries == null ? List.of() : deliveries;
    }

    private boolean requiresExternalTransactionRef(String paymentMethod) {
        return PaymentMethod.BANK_TRANSFER.name().equals(paymentMethod)
                || PaymentMethod.WECHAT.name().equals(paymentMethod)
                || PaymentMethod.ALIPAY.name().equals(paymentMethod)
                || PaymentMethod.CHECK.name().equals(paymentMethod);
    }

    private boolean canRecordPayment(TranStage stage) {
        return stage == TranStage.APPROVED || stage == TranStage.PAYMENT;
    }

    private boolean canCreateInvoice(TranStage stage) {
        return stage == TranStage.APPROVED || stage == TranStage.PAYMENT || stage == TranStage.DELIVERY;
    }

    private boolean canProcessRefund(TranStage stage) {
        return stage == TranStage.PAYMENT
                || stage == TranStage.DELIVERY
                || stage == TranStage.CANCELLED;
    }

    private BigDecimal calculateAvailableInvoiceAmount(TTran tran, List<TTranInvoice> invoices) {
        BigDecimal usedAmount = normalizeInvoices(invoices).stream()
                .filter(invoice -> invoice.getAmount() != null)
                .filter(invoice -> !"VOIDED".equals(invoice.getStatus()))
                .filter(invoice -> !"FAILED".equals(invoice.getStatus()))
                .filter(invoice -> !"NOT_REQUIRED".equals(invoice.getStatus()))
                .map(TTranInvoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableAmount = nullToZero(tran.getMoney()).subtract(usedAmount).setScale(2, RoundingMode.HALF_UP);
        return availableAmount.max(BigDecimal.ZERO);
    }

    private List<TTranInvoice> normalizeInvoices(List<TTranInvoice> invoices) {
        return invoices == null ? Collections.emptyList() : invoices;
    }

    private TTranInvoice requireInvoiceForMutation(Integer invoiceId) {
        TTranInvoice invoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (invoice == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "发票不存在");
        }
        requireAccessibleTransaction(invoice.getTranId());
        TTran lockedTran = tranMapper.selectByPrimaryKeyForUpdate(invoice.getTranId());
        if (lockedTran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        invoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (invoice == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "发票不存在");
        }
        return invoice;
    }

    private BigDecimal calculateCompletedRedReversalAmount(Integer originalInvoiceId, List<TTranInvoice> invoices) {
        return normalizeInvoices(invoices).stream()
                .filter(invoice -> Objects.equals(invoice.getOriginalInvoiceId(), originalInvoiceId))
                .filter(invoice -> "RED_REVERSED".equals(invoice.getStatus()))
                .map(TTranInvoice::getAmount)
                .filter(Objects::nonNull)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private TTranInvoice copyInvoiceForRedReverse(TTranInvoice originalInvoice, BigDecimal amount,
                                                  String reason, Integer operatorId, Date now) {
        TTranInvoice redInvoice = new TTranInvoice();
        redInvoice.setTranId(originalInvoice.getTranId());
        redInvoice.setOriginalInvoiceId(originalInvoice.getId());
        redInvoice.setInvoiceNo(generateInvoiceNo());
        redInvoice.setType(originalInvoice.getType());
        redInvoice.setTitle(originalInvoice.getTitle());
        redInvoice.setTaxNumber(originalInvoice.getTaxNumber());
        redInvoice.setBankName(originalInvoice.getBankName());
        redInvoice.setBankAccount(originalInvoice.getBankAccount());
        redInvoice.setAddress(originalInvoice.getAddress());
        redInvoice.setPhone(originalInvoice.getPhone());
        redInvoice.setAmount(amount.negate().setScale(2, RoundingMode.HALF_UP));
        redInvoice.setStatus("RED_REVERSED");
        redInvoice.setRemark("红冲原因：" + reason);
        redInvoice.setIssueTime(now);
        redInvoice.setCreateTime(now);
        redInvoice.setCreateBy(operatorId);
        redInvoice.setEditTime(now);
        redInvoice.setEditBy(operatorId);
        return redInvoice;
    }

    private TTranInvoice copyInvoiceForReissue(TTranInvoice sourceInvoice, TTranInvoice request,
                                               String reason, Integer operatorId, Date now) {
        TTranInvoice reissueInvoice = new TTranInvoice();
        reissueInvoice.setTranId(sourceInvoice.getTranId());
        reissueInvoice.setOriginalInvoiceId(sourceInvoice.getId());
        reissueInvoice.setInvoiceNo(generateInvoiceNo());
        reissueInvoice.setType(firstNonBlank(request.getType(), sourceInvoice.getType()));
        reissueInvoice.setTitle(firstNonBlank(request.getTitle(), sourceInvoice.getTitle()));
        reissueInvoice.setTaxNumber(firstNonBlank(request.getTaxNumber(), sourceInvoice.getTaxNumber()));
        reissueInvoice.setBankName(firstNonBlank(request.getBankName(), sourceInvoice.getBankName()));
        reissueInvoice.setBankAccount(firstNonBlank(request.getBankAccount(), sourceInvoice.getBankAccount()));
        reissueInvoice.setAddress(firstNonBlank(request.getAddress(), sourceInvoice.getAddress()));
        reissueInvoice.setPhone(firstNonBlank(request.getPhone(), sourceInvoice.getPhone()));
        reissueInvoice.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        reissueInvoice.setStatus("PENDING");
        reissueInvoice.setRemark("重开原因：" + reason);
        reissueInvoice.setCreateTime(now);
        reissueInvoice.setCreateBy(operatorId);
        reissueInvoice.setEditTime(now);
        reissueInvoice.setEditBy(operatorId);
        return reissueInvoice;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }

    private TTranInvoice maskInvoiceSensitiveFields(TTranInvoice invoice) {
        TTranInvoice masked = new TTranInvoice();
        masked.setId(invoice.getId());
        masked.setTranId(invoice.getTranId());
        masked.setInvoiceNo(invoice.getInvoiceNo());
        masked.setType(invoice.getType());
        masked.setTitle(maskText(invoice.getTitle(), 2, 2));
        masked.setTaxNumber(maskText(invoice.getTaxNumber(), 4, 4));
        masked.setBankName(invoice.getBankName());
        masked.setBankAccount(maskText(invoice.getBankAccount(), 4, 4));
        masked.setAddress(maskText(invoice.getAddress(), 6, 0));
        masked.setPhone(maskText(invoice.getPhone(), 3, 4));
        masked.setOriginalInvoiceId(invoice.getOriginalInvoiceId());
        masked.setAmount(invoice.getAmount());
        masked.setStatus(invoice.getStatus());
        masked.setRemark(invoice.getRemark());
        masked.setIssueTime(invoice.getIssueTime());
        masked.setCreateTime(invoice.getCreateTime());
        masked.setCreateBy(invoice.getCreateBy());
        masked.setEditTime(invoice.getEditTime());
        masked.setEditBy(invoice.getEditBy());
        return masked;
    }

    private String maskText(String value, int prefixLength, int suffixLength) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= prefixLength + suffixLength) {
            return "*".repeat(trimmed.length());
        }
        String prefix = trimmed.substring(0, Math.max(prefixLength, 0));
        String suffix = suffixLength > 0 ? trimmed.substring(trimmed.length() - suffixLength) : "";
        return prefix + "****" + suffix;
    }

    private String buildPaymentIdempotencyKey(Integer tranId, String paymentMethod,
                                              String transactionRef, BigDecimal amount) {
        String normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (transactionRef != null) {
            return "PAYMENT:REF:" + paymentMethod + ":" + transactionRef;
        }
        return "PAYMENT:MANUAL:" + tranId + ":" + paymentMethod + ":" + normalizedAmount;
    }

    private String buildRefundIdempotencyKey(Integer requestId, String paymentMethod, String transactionRef) {
        if (transactionRef != null) {
            return "REFUND:REF:" + paymentMethod + ":" + transactionRef;
        }
        return "REFUND:REQUEST:" + requestId;
    }

    private TPayment findPaymentByIdempotency(List<TPayment> payments, String idempotencyKey) {
        return normalizePayments(payments).stream()
                .filter(payment -> idempotencyKey.equals(payment.getIdempotencyKey()))
                .findFirst()
                .orElse(null);
    }

    private TPayment requireSamePaymentRequest(TPayment existing, TPayment request, BigDecimal expectedAmount) {
        if (existing == null) {
            return null;
        }
        boolean sameRequest = request.getTranId().equals(existing.getTranId())
                && request.getPaymentMethod().equals(existing.getPaymentMethod())
                && existing.getAmount() != null
                && existing.getAmount().compareTo(expectedAmount) == 0
                && !PaymentType.REFUND.name().equals(existing.getPaymentType());
        if (!sameRequest) {
            throw new BusinessException(CodeEnum.DUPLICATE, "收款幂等键或支付参考号已被其他请求使用");
        }
        return existing;
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
                .filter(payment -> PaymentStatus.COMPLETED.name().equals(payment.getPaymentStatus()))
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

    private void validateTransactionProduct(TTranProduct product) {
        if (product == null || product.getProductId() == null
                || product.getQuantity() == null || product.getQuantity() <= 0
                || product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("交易商品、正数数量和非负价格不能为空");
        }
    }

    private void fillProductSnapshot(TTranProduct target, TProduct product) {
        target.setProductSku(product.getSku());
        target.setProductName(product.getName());
        target.setProductSpecification(product.getSpecification());
        target.setGuidePrice(product.getPrice());
    }

    private TTran findAccessibleTransaction(Integer tranId) {
        CurrentUserProvider.TransactionDataScope scope = currentUserProvider.getTransactionDataScope();
        return scope.isAll()
                ? tranMapper.selectByPrimaryKey(tranId)
                : tranMapper.selectScopedById(tranId, scope.getSelfUserId(),
                        scope.isApprovalScope(), scope.getFinanceStages());
    }

    private int updateTransactionScoped(TTran transaction) {
        CurrentUserProvider.TransactionDataScope scope = currentUserProvider.getTransactionDataScope();
        return tranMapper.updateScopedByPrimaryKeySelective(
                transaction, scope.isAll(), scope.getSelfUserId());
    }

    private TCustomer requireAccessibleCustomer(Integer customerId) {
        if (customerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户不能为空");
        }
        TCustomer customer = customerMapper.selectScopedById(customerId, currentUserProvider.getDataScopeUserId());
        if (customer == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权访问该客户");
        }
        return customer;
    }

    private void validateCustomerRebind(Integer requestedCustomerId, Integer existingCustomerId) {
        if (requestedCustomerId != null && !requestedCustomerId.equals(existingCustomerId)) {
            requireAccessibleCustomer(requestedCustomerId);
        }
    }

    private void sanitizeTransactionUpdate(TTran tran) {
        tran.setTranNo(null);
        tran.setCreateBy(null);
        tran.setCreateTime(null);
        tran.setStage(null);
        // 不信任客户端提交的金额，由服务端从产品行项重新计算。
        tran.setMoney(null);
    }

    private TTran requireAccessibleTransaction(Integer tranId) {
        TTran transaction = findAccessibleTransaction(tranId);
        if (transaction == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在或无权访问");
        }
        return transaction;
    }

    private void writeHistory(Integer tranId, TranStage stage, BigDecimal money, Date expectedDate, Integer userId) {
        writeHistory(tranId, stage, money, expectedDate, userId, null);
    }

    private void writeHistory(Integer tranId, TranStage stage, BigDecimal money, Date expectedDate,
                              Integer userId, String reason) {
        TTranHistory history = new TTranHistory();
        history.setTranId(tranId);
        history.setStage(stage.name());
        history.setReason(reason);
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
        redisManager.delete(RedisKeys.transactionProducts(tranId));
        redisManager.delete(RedisKeys.transactionInvoices(tranId));
    }
}
