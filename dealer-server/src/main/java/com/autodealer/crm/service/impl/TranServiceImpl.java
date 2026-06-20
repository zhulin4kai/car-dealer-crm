package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.enums.PaymentMethod;
import com.autodealer.crm.enums.PaymentType;
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

import java.math.BigDecimal;
import java.util.Date;
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
    private TTranHistoryMapper tranHistoryMapper;

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

        if (tranMapper.insertSelective(tTran) != 1 || tTran.getId() == null) {
            throw new RuntimeException("交易创建失败");
        }
        Integer tranId = tTran.getId();

        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                validateTransactionProduct(product);
                product.setTranId(tranId);
                product.setCreateBy(operatorId);
                product.setCreateTime(now);
                if (tranProductMapper.insertSelective(product) != 1) {
                    throw new RuntimeException("交易商品创建失败: " + product.getProductId());
                }

                int updateCount = productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new RuntimeException("产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
        }

        writeHistory(tranId, TranStage.QUOTATION, tTran.getMoney(),
                tTran.getExpectedDate(), tTran.getCreateBy());

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
            throw new RuntimeException("仅待报价阶段的交易可以修改");
        }

        tTran.setStage(null);
        tTran.setEditBy(currentUserProvider.getCurrentUserId());
        tTran.setEditTime(new Date());
        int rows = tranMapper.updateByPrimaryKeySelective(tTran);

        if (rows > 0) {
            clearTransactionCache(tTran.getId());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settleTransaction(Integer tranId, BigDecimal amount) {
        if (tranId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("交易 ID 和正数结算金额不能为空");
        }
        requireAccessibleTransaction(tranId);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = tranMapper.settleAtomic(tranId, amount, operatorId);
        if (rows != 1) {
            throw new RuntimeException("当前交易状态不允许结算");
        }
        TTran tran = tranMapper.selectByPrimaryKey(tranId);
        writeHistory(tranId, TranStage.PENDING, amount,
                tran != null ? tran.getExpectedDate() : null, operatorId);
        clearTransactionCache(tranId);
        return true;
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
            throw new RuntimeException("仅待报价阶段可以修改交易商品");
        }
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                if (productMapper.updateStock(
                        product.getProductId().longValue(), product.getQuantity()) != 1) {
                    throw new RuntimeException("恢复产品库存失败: " + product.getProductId());
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
            throw new RuntimeException("仅待报价阶段可以修改交易商品");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                validateTransactionProduct(product);
                product.setTranId(tranId);
                product.setCreateBy(operatorId);
                product.setCreateTime(new Date());
                if (tranProductMapper.insertSelective(product) != 1) {
                    throw new RuntimeException("交易商品创建失败: " + product.getProductId());
                }

                int updateCount = productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new RuntimeException("产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
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
        Date now = new Date();

        // 原子 CAS：仅 PENDING 阶段可审批
        int stageResult = tranMapper.updateStageAtomic(tranId,
                approved ? TranStage.APPROVED : TranStage.LOST,
                TranStage.PENDING, approveBy);
        if (stageResult == 0) {
            throw new RuntimeException("当前交易状态不允许审批操作");
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
            throw new RuntimeException("审批记录创建失败");
        }
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
            throw new RuntimeException("发票信息不完整");
        }

        TTran tran = requireAccessibleTransaction(invoice.getTranId());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        invoice.setCreateBy(operatorId);
        invoice.setEditBy(operatorId);
        if (invoice.getAmount() == null || tran.getMoney() == null) {
            throw new RuntimeException("发票金额和交易金额不能为空");
        }
        if (invoice.getAmount().compareTo(tran.getMoney()) != 0) {
            throw new RuntimeException("发票金额必须等于交易结算金额");
        }
        List<TTranInvoice> existingInvoices = tranInvoiceMapper.selectByTranId(invoice.getTranId());
        if (existingInvoices != null && !existingInvoices.isEmpty()) {
            throw new RuntimeException("该交易已开具发票，不可重复开票");
        }

        int stageResult = tranMapper.updateStageAtomic(invoice.getTranId(),
                TranStage.PAYMENT, TranStage.APPROVED, invoice.getCreateBy());
        if (stageResult != 1) {
            throw new RuntimeException("当前交易状态不允许创建发票");
        }

        Date now = new Date();
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setStatus("PENDING");
        invoice.setCreateTime(now);
        invoice.setEditTime(now);

        int result = tranInvoiceMapper.insertSelective(invoice);
        if (result != 1) {
            throw new RuntimeException("发票创建失败");
        }
        writeHistory(invoice.getTranId(), TranStage.PAYMENT,
                tran.getMoney(), tran.getExpectedDate(), invoice.getCreateBy());
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
            throw new RuntimeException("不支持的发票状态: " + status);
        }

        TTranInvoice currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (currentInvoice == null) {
            throw new RuntimeException("发票不存在");
        }
        requireAccessibleTransaction(currentInvoice.getTranId());
        Integer updateBy = currentUserProvider.getCurrentUserId();
        boolean validTransition = "PENDING".equals(currentInvoice.getStatus())
                || ("ISSUED".equals(currentInvoice.getStatus()) && "VOID".equals(status));
        if (!validTransition || currentInvoice.getStatus().equals(status)) {
            throw new RuntimeException("当前发票状态不允许此操作");
        }

        Date now = new Date();
        int result = tranInvoiceMapper.updateStatusIfCurrent(invoiceId, currentInvoice.getStatus(),
                status, "ISSUED".equals(status) ? now : currentInvoice.getIssueTime(), now, updateBy);
        if (result != 1) {
            throw new RuntimeException("发票状态已变更，请刷新后重试");
        }

        if ("VOID".equals(status)) {
            int stageRows = tranMapper.updateStageAtomic(currentInvoice.getTranId(),
                    TranStage.APPROVED, TranStage.PAYMENT, updateBy);
            if (stageRows != 1) {
                throw new RuntimeException("交易已进入后续状态，不允许作废发票");
            }
            TTran tran = tranMapper.selectByPrimaryKey(currentInvoice.getTranId());
            writeHistory(currentInvoice.getTranId(), TranStage.APPROVED,
                    tran != null ? tran.getMoney() : null,
                    tran != null ? tran.getExpectedDate() : null, updateBy);
        }

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
            throw new RuntimeException("单次批量删除最多支持" + Constants.MAX_BATCH_SIZE + "条记录");
        }

        List<Integer> lockedIds = ids.stream().distinct().sorted().toList();
        lockedIds.forEach(this::requireAccessibleTransaction);
        for (Integer id : lockedIds) {
            TTran transaction = tranMapper.selectByPrimaryKeyForUpdate(id);
            if (transaction == null) {
                throw new RuntimeException("交易不存在: " + id);
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
            throw new RuntimeException("只有待报价状态的交易才能删除: " + transaction.getId());
        }
    }

    private void deleteLockedTransaction(Integer id) {
        List<TTranProduct> products = tranProductMapper.selectByTranId(id);
        if (products != null) {
            for (TTranProduct product : products) {
                int rows = productMapper.updateStock(
                        product.getProductId().longValue(), product.getQuantity());
                if (rows != 1) {
                    throw new RuntimeException("恢复产品库存失败: " + product.getProductId());
                }
            }
        }
        tranProductMapper.deleteByTranId(id);
        tranRemarkMapper.deleteByTranId(id);
        tranInvoiceMapper.deleteByTranId(id);
        tranApproveMapper.deleteByTranId(id);
        paymentMapper.deleteByTranId(id);
        if (tranMapper.deleteByPrimaryKey(id) != 1) {
            throw new RuntimeException("交易删除失败: " + id);
        }
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
            throw new RuntimeException("当前交易状态不允许重新提交");
        }

        deductTransactionStock(tranId);

        // 清除旧的审批记录
        tranApproveMapper.deleteByTranId(tranId);

        writeHistory(tranId, TranStage.QUOTATION,
                tran.getMoney(), tran.getExpectedDate(), userId);
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
            throw new RuntimeException("仅待报价阶段的交易可以修改产品和金额");
        }

        tran.setEditBy(currentUserProvider.getCurrentUserId());
        tran.setEditTime(new Date());
        tran.setStage(null);
        int rows = tranMapper.updateByPrimaryKeySelective(tran);
        if (rows == 0) {
            return false;
        }

        deleteTransactionProducts(tran.getId());

        if (products != null && !products.isEmpty()) {
            addTransactionProducts(tran.getId(), products);
        }

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
        if (payment == null || payment.getTranId() == null || payment.getAmount() == null) {
            throw new RuntimeException("支付信息不完整");
        }
        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("支付金额必须大于0");
        }
        validatePaymentMethod(payment.getPaymentMethod());
        validatePaymentType(payment.getPaymentType());

        requireAccessibleTransaction(payment.getTranId());
        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(payment.getTranId());
        if (tran == null) {
            throw new RuntimeException("交易记录不存在");
        }
        payment.setCreateBy(currentUserProvider.getCurrentUserId());
        if (tran.getStage() != TranStage.PAYMENT) {
            throw new RuntimeException("当前交易状态不允许收款");
        }
        if (tran.getMoney() == null || tran.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("交易金额无效，无法收款");
        }

        List<TPayment> existingPayments = paymentMapper.selectByTranId(payment.getTranId());
        BigDecimal paidAmount = (existingPayments == null ? List.<TPayment>of() : existingPayments).stream()
                .filter(existing -> "COMPLETED".equals(existing.getPaymentStatus()))
                .filter(existing -> !PaymentType.REFUND.name().equals(existing.getPaymentType()))
                .map(TPayment::getAmount)
                .filter(amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAfterPayment = paidAmount.add(payment.getAmount());
        if (totalAfterPayment.compareTo(tran.getMoney()) > 0) {
            throw new RuntimeException("收款总额不能超过交易金额");
        }

        Date now = new Date();
        payment.setPaymentNo(generatePaymentNo());
        payment.setPaymentStatus("COMPLETED");
        payment.setPaymentTime(now);
        payment.setCreateTime(now);

        if (paymentMapper.insertSelective(payment) != 1) {
            throw new RuntimeException("收款记录创建失败");
        }

        if (totalAfterPayment.compareTo(tran.getMoney()) == 0) {
            int rows = tranMapper.updateStageToCompleted(payment.getTranId(), payment.getCreateBy());
            if (rows != 1) {
                throw new RuntimeException("交易完成状态更新失败");
            }
            writeHistory(payment.getTranId(), TranStage.COMPLETED,
                    tran.getMoney(), tran.getExpectedDate(), payment.getCreateBy());
        }

        clearTransactionCache(payment.getTranId());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPayment refundPayment(Integer paymentId) {
        TPayment original = paymentMapper.selectByPrimaryKey(paymentId);
        if (original == null) {
            throw new RuntimeException("支付记录不存在");
        }
        if (!"COMPLETED".equals(original.getPaymentStatus())) {
            throw new RuntimeException("只能对已到账的收款进行退款");
        }
        if (PaymentType.REFUND.name().equals(original.getPaymentType())) {
            throw new RuntimeException("退款记录不能再次退款");
        }

        TTran tran = requireAccessibleTransaction(original.getTranId());
        Integer userId = currentUserProvider.getCurrentUserId();
        List<TPayment> transactionPayments = paymentMapper.selectByTranId(original.getTranId());
        List<TPayment> completedPayments = (transactionPayments == null
                ? List.<TPayment>of() : transactionPayments).stream()
                .filter(payment -> "COMPLETED".equals(payment.getPaymentStatus()))
                .filter(payment -> !PaymentType.REFUND.name().equals(payment.getPaymentType()))
                .toList();
        if (tran.getStage() != TranStage.COMPLETED
                || completedPayments.size() != 1
                || !paymentId.equals(completedPayments.get(0).getId())
                || original.getAmount().compareTo(tran.getMoney()) != 0) {
            throw new RuntimeException("当前仅支持单笔全额支付的整单退款");
        }

        Date now = new Date();

        int stageRows = tranMapper.updateStageAtomic(original.getTranId(),
                TranStage.CANCELLED, TranStage.COMPLETED, userId);
        if (stageRows != 1) {
            throw new RuntimeException("交易已退款或状态不允许退款");
        }

        int marked = paymentMapper.markRefundedIfCompleted(paymentId, now, userId);
        if (marked != 1) {
            throw new RuntimeException("该收款已退款或状态已变更");
        }

        // 创建退款记录（负金额）
        TPayment refund = new TPayment();
        refund.setTranId(original.getTranId());
        refund.setAmount(original.getAmount().negate());
        refund.setPaymentMethod(original.getPaymentMethod());
        refund.setPaymentType(PaymentType.REFUND.name());
        refund.setPaymentStatus("COMPLETED");
        refund.setPaymentTime(now);
        refund.setPaymentNo(generatePaymentNo());
        refund.setCreateTime(now);
        refund.setCreateBy(userId);
        refund.setRemark("退款 - 交易取消，原收款ID: " + original.getId());
        if (paymentMapper.insertSelective(refund) != 1) {
            throw new RuntimeException("退款记录创建失败");
        }

        restoreTransactionStock(original.getTranId());

        writeHistory(original.getTranId(), TranStage.CANCELLED,
                tran.getMoney(), tran.getExpectedDate(), userId);

        clearTransactionCache(original.getTranId());
        return refund;
    }

    @Override
    public List<TPayment> getTransactionPayments(Integer tranId) {
        requireAccessibleTransaction(tranId);
        return paymentMapper.selectByTranId(tranId);
    }

    private void restoreTransactionStock(Integer tranId) {
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products == null) {
            return;
        }
        for (TTranProduct product : products) {
            if (productMapper.updateStock(
                    product.getProductId().longValue(), product.getQuantity()) != 1) {
                throw new RuntimeException("恢复产品库存失败: " + product.getProductId());
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
                throw new RuntimeException("产品库存不足: " + product.getProductId());
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
        Integer scopeUserId = currentUserProvider.getDataScopeUserId();
        return scopeUserId == null
                ? tranMapper.selectByPrimaryKey(tranId)
                : tranMapper.selectScopedById(tranId, scopeUserId);
    }

    private TTran requireAccessibleTransaction(Integer tranId) {
        TTran transaction = findAccessibleTransaction(tranId);
        if (transaction == null) {
            throw new RuntimeException("交易不存在或无权访问");
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
            throw new RuntimeException("交易历史记录创建失败");
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
