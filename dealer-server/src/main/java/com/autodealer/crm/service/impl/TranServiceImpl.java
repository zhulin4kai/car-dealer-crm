package com.autodealer.crm.service.impl;

import com.autodealer.crm.constant.Constants;
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
        return tranMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createTransaction(TTran tTran, List<TTranProduct> products) {
        Date now = new Date();
        tTran.setCreateTime(now);
        tTran.setTranNo(generateTranNo());

        tranMapper.insertSelective(tTran);
        Integer tranId = tTran.getId();

        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                product.setCreateTime(now);
                tranProductMapper.insertSelective(product);

                int updateCount = productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
                if (updateCount == 0) {
                    throw new RuntimeException("产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
                }
            }
        }

        clearTransactionCache(tranId);
        return tranId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransaction(TTran tTran) {
        if (tTran == null || tTran.getId() == null) {
            return false;
        }

        TTran existing = tranMapper.selectByPrimaryKey(tTran.getId());
        if (existing == null) {
            return false;
        }

        // 仅 QUOTATION 阶段可修改金额
        if (tTran.getMoney() != null && existing.getStage() != TranStage.QUOTATION) {
            throw new RuntimeException("仅待报价阶段的交易可以修改金额");
        }

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
    public boolean updateTransactionStage(Integer id, TranStage newStage) {
        TTran existing = tranMapper.selectByPrimaryKey(id);
        if (existing == null) {
            throw new RuntimeException("交易记录不存在");
        }

        int result = tranMapper.updateStageAtomic(id, newStage, existing.getStage(), existing.getEditBy());
        if (result > 0) {
            writeHistory(id, newStage.getLabel(), existing.getMoney(), existing.getExpectedDate(), existing.getEditBy());
            clearTransactionCache(id);
            return true;
        }
        throw new RuntimeException("阶段变更失败，当前状态不允许此操作");
    }

    @Override
    public boolean addTransactionRemark(TTranRemark remark) {
        if (remark == null || remark.getTranId() == null) {
            return false;
        }
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
        return tranRemarkMapper.selectByTranId(tranId);
    }

    @Override
    public List<TTranProduct> getTransactionProductDetails(Integer tranId) {
        return tranMapper.selectTranProductsByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTransactionProducts(Integer tranId) {
        List<TTranProduct> products = tranProductMapper.selectByTranId(tranId);
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                productMapper.updateStock(product.getProductId().longValue(), product.getQuantity());
            }
        }

        tranProductMapper.deleteByTranId(tranId);
        redisManager.delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTransactionProducts(Integer tranId, List<TTranProduct> products) {
        if (products != null && !products.isEmpty()) {
            for (TTranProduct product : products) {
                product.setTranId(tranId);
                tranProductMapper.insertSelective(product);

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
    public boolean approveTran(Integer tranId, Boolean approved, String comment, Integer approveBy) {
        Date now = new Date();

        // 原子 CAS：仅 PENDING 阶段可审批
        int stageResult = tranMapper.updateStageAtomic(tranId,
                approved ? TranStage.APPROVED : TranStage.LOST,
                TranStage.PENDING, approveBy);
        if (stageResult == 0) {
            throw new RuntimeException("当前交易状态不允许审批操作");
        }

        TTran tran = tranMapper.selectByPrimaryKey(tranId);
        writeHistory(tranId, (approved ? TranStage.APPROVED : TranStage.LOST).getLabel(),
                tran != null ? tran.getMoney() : null, null, approveBy);

        TTranApprove approve = new TTranApprove();
        approve.setTranId(tranId);
        approve.setApproveResult(approved);
        approve.setApproveComment(comment);
        approve.setApproveTime(now);
        approve.setApproveBy(approveBy);
        approve.setCreateTime(now);
        approve.setCreateBy(approveBy);

        tranApproveMapper.insertSelective(approve);
        clearTransactionCache(tranId);
        return true;
    }

    @Override
    public TTranApprove getTranApprove(Integer tranId) {
        return tranApproveMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTranInvoice(TTranInvoice invoice) {
        if (invoice == null || invoice.getTranId() == null) {
            throw new RuntimeException("发票信息不完整");
        }

        // 原子 CAS：仅 APPROVED 阶段可创建发票
        int stageResult = tranMapper.updateStageAtomic(invoice.getTranId(),
                TranStage.PAYMENT, TranStage.APPROVED, invoice.getCreateBy());
        if (stageResult == 0) {
            throw new RuntimeException("当前交易状态不允许创建发票");
        }

        List<TTranInvoice> existingInvoices = tranInvoiceMapper.selectByTranId(invoice.getTranId());
        if (existingInvoices != null && !existingInvoices.isEmpty()) {
            throw new RuntimeException("该交易已开具发票，不可重复开票");
        }

        TTran tran = tranMapper.selectByPrimaryKey(invoice.getTranId());
        if (tran == null) {
            throw new RuntimeException("交易记录不存在");
        }
        if (invoice.getAmount() == null || tran.getMoney() == null) {
            throw new RuntimeException("发票金额和交易金额不能为空");
        }
        if (invoice.getAmount().compareTo(tran.getMoney()) != 0) {
            throw new RuntimeException("发票金额必须等于交易结算金额");
        }

        Date now = new Date();
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setStatus("PENDING");
        invoice.setCreateTime(now);
        invoice.setEditTime(now);

        int result = tranInvoiceMapper.insertSelective(invoice);
        if (result > 0) {
            writeHistory(invoice.getTranId(), TranStage.PAYMENT.getLabel(),
                    tran != null ? tran.getMoney() : null, null, invoice.getCreateBy());
            clearTransactionCache(invoice.getTranId());
            return true;
        }
        return false;
    }

    @Override
    public List<TTranInvoice> getTranInvoices(Integer tranId) {
        return tranInvoiceMapper.selectByTranId(tranId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTranInvoiceStatus(Integer invoiceId, String status, Integer updateBy) {
        if (status == null || status.trim().isEmpty()) {
            throw new RuntimeException("发票状态不能为空");
        }

        Date now = new Date();
        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(invoiceId);
        invoice.setStatus(status);
        invoice.setEditTime(now);
        invoice.setEditBy(updateBy);

        if ("ISSUED".equals(status)) {
            invoice.setIssueTime(now);
        }

        int result = tranInvoiceMapper.updateByPrimaryKeySelective(invoice);
        if (result == 0) {
            return false;
        }

        TTranInvoice currentInvoice = tranInvoiceMapper.selectByPrimaryKey(invoiceId);
        if (currentInvoice == null) {
            return false;
        }

        if ("VOID".equals(status)) {
            // 发票作废：如果交易已进入 PAYMENT 阶段，回退到 APPROVED
            TTran currentTran = tranMapper.selectByPrimaryKey(currentInvoice.getTranId());
            if (currentTran != null && currentTran.getStage() == TranStage.PAYMENT) {
                tranMapper.updateStageAtomic(currentInvoice.getTranId(),
                        TranStage.APPROVED, TranStage.PAYMENT, updateBy);
            }
        }

        clearTransactionCache(currentInvoice.getTranId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTransaction(Integer id) {
        TTran transaction = tranMapper.selectByPrimaryKey(id);
        if (transaction == null) {
            return false;
        }

        if (transaction.getStage() != TranStage.QUOTATION) {
            throw new RuntimeException("只有待报价状态的交易才能删除");
        }

        // 恢复库存
        List<TTranProduct> tranProducts = tranProductMapper.selectByTranId(id);
        if (tranProducts != null && !tranProducts.isEmpty()) {
            for (TTranProduct product : tranProducts) {
                productMapper.updateStock(product.getProductId().longValue(), product.getQuantity());
            }
            tranProductMapper.deleteByTranId(id);
        }

        // 级联删除关联记录
        tranRemarkMapper.deleteByTranId(id);
        tranInvoiceMapper.deleteByTranId(id);
        tranApproveMapper.deleteByTranId(id);
        paymentMapper.deleteByTranId(id);

        int result = tranMapper.deleteByPrimaryKey(id);
        if (result > 0) {
            clearTransactionCache(id);
            return true;
        }
        return false;
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

        for (Integer id : ids) {
            TTran transaction = tranMapper.selectByPrimaryKey(id);
            if (transaction == null) {
                continue;
            }
            if (transaction.getStage() != TranStage.QUOTATION) {
                continue;
            }

            List<TTranProduct> tranProducts = tranProductMapper.selectByTranId(id);
            if (tranProducts != null && !tranProducts.isEmpty()) {
                for (TTranProduct product : tranProducts) {
                    productMapper.updateStock(product.getProductId().longValue(), product.getQuantity());
                }
                tranProductMapper.deleteByTranId(id);
            }

            tranRemarkMapper.deleteByTranId(id);
            tranInvoiceMapper.deleteByTranId(id);
            tranApproveMapper.deleteByTranId(id);
            clearTransactionCache(id);
        }

        int result = tranMapper.deleteByIds(ids);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resubmitTransaction(Integer tranId, Integer userId) {
        // 原子 CAS：仅 LOST 阶段可重新提交
        int stageResult = tranMapper.updateStageAtomic(tranId,
                TranStage.QUOTATION, TranStage.LOST, userId);
        if (stageResult == 0) {
            throw new RuntimeException("当前交易状态不允许重新提交");
        }

        // 清除旧的审批记录
        tranApproveMapper.deleteByTranId(tranId);

        writeHistory(tranId, TranStage.QUOTATION.getLabel(), null, null, userId);
        clearTransactionCache(tranId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTransactionWithProducts(TTran tran, List<TTranProduct> products) {
        if (tran == null || tran.getId() == null) {
            return false;
        }

        TTran existing = tranMapper.selectByPrimaryKey(tran.getId());
        if (existing == null) {
            return false;
        }

        // 仅 QUOTATION 阶段可修改产品和金额
        if (existing.getStage() != TranStage.QUOTATION) {
            throw new RuntimeException("仅待报价阶段的交易可以修改产品和金额");
        }

        tran.setEditTime(new Date());
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
        TTranInvoice invoice = tranInvoiceMapper.selectByPrimaryKey(id);
        if (invoice == null) {
            return false;
        }
        return updateTranInvoiceStatus(id, status, invoice.getEditBy());
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

        TTran tran = tranMapper.selectByPrimaryKey(payment.getTranId());
        if (tran == null) {
            throw new RuntimeException("交易记录不存在");
        }
        if (tran.getStage() != TranStage.PAYMENT) {
            throw new RuntimeException("当前交易状态不允许收款");
        }

        Date now = new Date();
        payment.setPaymentNo(generatePaymentNo());
        if (payment.getPaymentStatus() == null) {
            payment.setPaymentStatus("COMPLETED");
        }
        if (payment.getPaymentTime() == null && "COMPLETED".equals(payment.getPaymentStatus())) {
            payment.setPaymentTime(now);
        }
        payment.setCreateTime(now);

        paymentMapper.insertSelective(payment);

        // 检查是否已收齐：SUM(已到账金额) >= 交易金额
        if ("COMPLETED".equals(payment.getPaymentStatus())) {
            List<TPayment> allPayments = paymentMapper.selectByTranId(payment.getTranId());
            BigDecimal totalPaid = allPayments.stream()
                    .filter(p -> "COMPLETED".equals(p.getPaymentStatus()))
                    .map(TPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalPaid.compareTo(tran.getMoney()) >= 0) {
                int rows = tranMapper.updateStageToCompleted(payment.getTranId(), payment.getCreateBy());
                if (rows > 0) {
                    writeHistory(payment.getTranId(), "COMPLETED", tran.getMoney(), null, payment.getCreateBy());
                }
            }
        }

        clearTransactionCache(payment.getTranId());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPayment refundPayment(Integer paymentId, Integer userId) {
        TPayment original = paymentMapper.selectByPrimaryKey(paymentId);
        if (original == null) {
            throw new RuntimeException("支付记录不存在");
        }
        if (!"COMPLETED".equals(original.getPaymentStatus())) {
            throw new RuntimeException("只能对已到账的收款进行退款");
        }

        TTran tran = tranMapper.selectByPrimaryKey(original.getTranId());
        if (tran == null) {
            throw new RuntimeException("交易记录不存在");
        }

        Date now = new Date();

        // 创建退款记录（负金额）
        TPayment refund = new TPayment();
        refund.setTranId(original.getTranId());
        refund.setAmount(original.getAmount().negate());
        refund.setPaymentMethod(original.getPaymentMethod());
        refund.setPaymentType("REFUND");
        refund.setPaymentStatus("COMPLETED");
        refund.setPaymentTime(now);
        refund.setPaymentNo(generatePaymentNo());
        refund.setCreateTime(now);
        refund.setCreateBy(userId);
        refund.setRemark("退款 - 交易取消，原收款ID: " + original.getId());
        paymentMapper.insertSelective(refund);

        // 标记原收款为已退款
        original.setPaymentStatus("REFUNDED");
        original.setEditTime(now);
        original.setEditBy(userId);
        paymentMapper.updateByPrimaryKeySelective(original);

        // 恢复库存
        List<TTranProduct> products = tranProductMapper.selectByTranId(original.getTranId());
        if (products != null) {
            for (TTranProduct p : products) {
                productMapper.updateStock(p.getProductId().longValue(), p.getQuantity());
            }
        }

        // 回退到 PAYMENT 阶段（从 COMPLETED）
        tranMapper.updateStageAtomic(original.getTranId(), TranStage.PAYMENT, TranStage.COMPLETED, userId);
        writeHistory(original.getTranId(), "PAYMENT (退款)", tran.getMoney(), null, userId);

        clearTransactionCache(original.getTranId());
        return refund;
    }

    @Override
    public List<TPayment> getTransactionPayments(Integer tranId) {
        return paymentMapper.selectByTranId(tranId);
    }

    private void writeHistory(Integer tranId, String stage, BigDecimal money, Date expectedDate, Integer userId) {
        TTranHistory history = new TTranHistory();
        history.setTranId(tranId);
        history.setStage(stage);
        history.setMoney(money);
        history.setExpectedDate(expectedDate);
        history.setCreateTime(new Date());
        history.setCreateBy(userId);
        tranHistoryMapper.insert(history);
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
