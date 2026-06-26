package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.enums.DeliveryStatus;
import com.autodealer.crm.enums.PaymentStatus;
import com.autodealer.crm.enums.PaymentType;
import com.autodealer.crm.enums.RefundRequestStatus;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TDeliveryMapper;
import com.autodealer.crm.mapper.TPaymentMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TRefundRequestMapper;
import com.autodealer.crm.mapper.TTranHistoryMapper;
import com.autodealer.crm.mapper.TTranInvoiceMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.model.TPayment;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.model.TRefundRequest;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.model.TTranHistory;
import com.autodealer.crm.model.TTranInvoice;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.TransactionCompletionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class TransactionCompletionServiceImpl implements TransactionCompletionService {

    private static final Set<String> OPEN_REFUND_STATUSES = Set.of(
            RefundRequestStatus.PENDING_APPROVAL.name(),
            RefundRequestStatus.PENDING_EXECUTION.name(),
            RefundRequestStatus.EXECUTING.name()
    );

    private final TTranMapper tranMapper;
    private final TPaymentMapper paymentMapper;
    private final TTranInvoiceMapper invoiceMapper;
    private final TDeliveryMapper deliveryMapper;
    private final TProductStockRecordMapper stockRecordMapper;
    private final TRefundRequestMapper refundRequestMapper;
    private final TTranHistoryMapper tranHistoryMapper;
    private final OperationAuditRecorder auditRecorder;
    private final RedisManager redisManager;

    public TransactionCompletionServiceImpl(TTranMapper tranMapper,
                                            TPaymentMapper paymentMapper,
                                            TTranInvoiceMapper invoiceMapper,
                                            TDeliveryMapper deliveryMapper,
                                            TProductStockRecordMapper stockRecordMapper,
                                            TRefundRequestMapper refundRequestMapper,
                                            TTranHistoryMapper tranHistoryMapper,
                                            OperationAuditRecorder auditRecorder,
                                            RedisManager redisManager) {
        this.tranMapper = tranMapper;
        this.paymentMapper = paymentMapper;
        this.invoiceMapper = invoiceMapper;
        this.deliveryMapper = deliveryMapper;
        this.stockRecordMapper = stockRecordMapper;
        this.refundRequestMapper = refundRequestMapper;
        this.tranHistoryMapper = tranHistoryMapper;
        this.auditRecorder = auditRecorder;
        this.redisManager = redisManager;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryComplete(Integer tranId, Integer operatorId) {
        if (tranId == null || operatorId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "交易ID和操作人不能为空");
        }
        TTran tran = tranMapper.selectByPrimaryKeyForUpdate(tranId);
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        if (tran.getStage() == TranStage.COMPLETED) {
            return true;
        }
        if (tran.getStage() != TranStage.DELIVERY) {
            return false;
        }
        if (tran.getMoney() == null || tran.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (hasOpenRefund(tranId)) {
            return false;
        }
        if (!isPaymentSatisfied(tran)) {
            return false;
        }
        if (!isInvoiceSatisfied(tran)) {
            return false;
        }
        if (!isDeliverySatisfied(tranId)) {
            return false;
        }

        int updated = tranMapper.updateStageAtomic(tranId, TranStage.COMPLETED, TranStage.DELIVERY, operatorId);
        if (updated != 1) {
            TTran latest = tranMapper.selectByPrimaryKeyForUpdate(tranId);
            if (latest != null && latest.getStage() == TranStage.COMPLETED) {
                return true;
            }
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易完成状态已变更，请刷新后重试");
        }
        writeHistory(tran, operatorId);
        auditRecorder.record(AuditActionEnum.TRAN_COMPLETE, String.valueOf(tranId));
        clearTransactionCache(tranId);
        return true;
    }

    private boolean hasOpenRefund(Integer tranId) {
        return refundRequestMapper.selectByTranId(tranId).stream()
                .map(TRefundRequest::getStatus)
                .anyMatch(OPEN_REFUND_STATUSES::contains);
    }

    private boolean isPaymentSatisfied(TTran tran) {
        BigDecimal confirmedAmount = paymentMapper.selectByTranId(tran.getId()).stream()
                .filter(payment -> PaymentStatus.COMPLETED.name().equals(payment.getPaymentStatus()))
                .map(this::signedPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return confirmedAmount.compareTo(tran.getMoney()) >= 0;
    }

    private BigDecimal signedPaymentAmount(TPayment payment) {
        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        if (PaymentType.REFUND.name().equals(payment.getPaymentType()) && amount.compareTo(BigDecimal.ZERO) > 0) {
            return amount.negate();
        }
        return amount;
    }

    private boolean isInvoiceSatisfied(TTran tran) {
        BigDecimal issuedAmount = invoiceMapper.selectByTranId(tran.getId()).stream()
                .filter(invoice -> "ISSUED".equals(invoice.getStatus()))
                .map(TTranInvoice::getAmount)
                .map(amount -> amount == null ? BigDecimal.ZERO : amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return issuedAmount.compareTo(tran.getMoney()) >= 0;
    }

    private boolean isDeliverySatisfied(Integer tranId) {
        List<TDelivery> deliveries = deliveryMapper.selectByTranId(tranId);
        for (TDelivery delivery : deliveries) {
            if (!DeliveryStatus.COMPLETED.name().equals(delivery.getStatus())) {
                continue;
            }
            TProductStockRecord outbound = stockRecordMapper.selectOutboundByDelivery(delivery.getId());
            if (outbound != null) {
                return true;
            }
        }
        return false;
    }

    private void writeHistory(TTran tran, Integer operatorId) {
        TTranHistory history = new TTranHistory();
        history.setTranId(tran.getId());
        history.setStage(TranStage.COMPLETED.name());
        history.setMoney(tran.getMoney());
        history.setExpectedDate(tran.getExpectedDate());
        history.setReason("完成条件聚合满足");
        history.setCreateTime(new Date());
        history.setCreateBy(operatorId);
        if (tranHistoryMapper.insert(history) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "交易历史记录创建失败");
        }
    }

    private void clearTransactionCache(Integer tranId) {
        redisManager.delete(RedisKeys.transactionProducts(tranId));
        redisManager.delete(RedisKeys.transactionInvoices(tranId));
    }
}
