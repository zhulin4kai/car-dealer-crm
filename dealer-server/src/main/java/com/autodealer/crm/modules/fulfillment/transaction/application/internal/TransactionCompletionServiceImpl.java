package com.autodealer.crm.modules.fulfillment.transaction.application.internal;

import com.autodealer.crm.modules.fulfillment.delivery.application.api.port.DeliveryDataPort;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.port.InvoiceDataPort;
import com.autodealer.crm.modules.fulfillment.payment.application.api.port.RefundDataPort;
import com.autodealer.crm.modules.fulfillment.payment.application.api.port.PaymentDataPort;
import com.autodealer.crm.modules.commerce.inventory.application.api.port.StockRecordDataPort;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.enums.DeliveryStatus;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.PaymentStatus;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.PaymentType;
import com.autodealer.crm.modules.fulfillment.payment.application.api.enums.RefundRequestStatus;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranHistoryMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranMapper;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDelivery;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TPayment;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductStockRecord;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TRefundRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranHistory;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.model.TTranInvoice;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.TransactionCompletionService;
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
    private final PaymentDataPort paymentMapper;
    private final InvoiceDataPort invoiceMapper;
    private final DeliveryDataPort deliveryMapper;
    private final StockRecordDataPort stockRecordMapper;
    private final RefundDataPort refundRequestMapper;
    private final TTranHistoryMapper tranHistoryMapper;
    private final OperationAuditRecorder auditRecorder;
    private final RedisManager redisManager;

    public TransactionCompletionServiceImpl(TTranMapper tranMapper,
                                            PaymentDataPort paymentMapper,
                                            InvoiceDataPort invoiceMapper,
                                            DeliveryDataPort deliveryMapper,
                                            StockRecordDataPort stockRecordMapper,
                                            RefundDataPort refundRequestMapper,
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
