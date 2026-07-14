package com.autodealer.crm.modules.fulfillment.transaction.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.enums.DeliveryStatus;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.fulfillment.delivery.persistence.mapper.TDeliveryMapper;
import com.autodealer.crm.modules.fulfillment.payment.persistence.mapper.TPaymentMapper;
import com.autodealer.crm.modules.commerce.inventory.persistence.mapper.TProductStockRecordMapper;
import com.autodealer.crm.modules.fulfillment.payment.persistence.mapper.TRefundRequestMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranHistoryMapper;
import com.autodealer.crm.modules.fulfillment.invoice.persistence.mapper.TTranInvoiceMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranMapper;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDelivery;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TPayment;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductStockRecord;
import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TRefundRequest;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranHistory;
import com.autodealer.crm.modules.fulfillment.invoice.application.api.model.TTranInvoice;
import com.autodealer.crm.modules.fulfillment.transaction.application.internal.TransactionCompletionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCompletionServiceImplTest {

    @Mock private TTranMapper tranMapper;
    @Mock private TPaymentMapper paymentMapper;
    @Mock private TTranInvoiceMapper invoiceMapper;
    @Mock private TDeliveryMapper deliveryMapper;
    @Mock private TProductStockRecordMapper stockRecordMapper;
    @Mock private TRefundRequestMapper refundRequestMapper;
    @Mock private TTranHistoryMapper tranHistoryMapper;
    @Mock private OperationAuditRecorder auditRecorder;
    @Mock private RedisManager redisManager;
    @InjectMocks private TransactionCompletionServiceImpl completionService;

    @BeforeEach
    void setUp() {
        lenient().when(tranHistoryMapper.insert(any())).thenReturn(1);
    }

    @Test
    void tryComplete_missingInvoice_shouldNotComplete() {
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran(TranStage.DELIVERY));
        when(refundRequestMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(paymentMapper.selectByTranId(1)).thenReturn(List.of(payment("COMPLETED", "FULL", BigDecimal.valueOf(100000))));
        when(invoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        boolean completed = completionService.tryComplete(1, 7);

        assertFalse(completed);
        verify(tranMapper, never()).updateStageAtomic(1, TranStage.COMPLETED, TranStage.DELIVERY, 7);
        verify(tranHistoryMapper, never()).insert(any());
    }

    @Test
    void tryComplete_missingDelivery_shouldNotComplete() {
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran(TranStage.DELIVERY));
        when(refundRequestMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(paymentMapper.selectByTranId(1)).thenReturn(List.of(payment("COMPLETED", "FULL", BigDecimal.valueOf(100000))));
        when(invoiceMapper.selectByTranId(1)).thenReturn(List.of(invoice("ISSUED", BigDecimal.valueOf(100000))));
        when(deliveryMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        boolean completed = completionService.tryComplete(1, 7);

        assertFalse(completed);
        verify(tranMapper, never()).updateStageAtomic(1, TranStage.COMPLETED, TranStage.DELIVERY, 7);
    }

    @Test
    void tryComplete_openRefund_shouldNotComplete() {
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran(TranStage.DELIVERY));
        when(refundRequestMapper.selectByTranId(1)).thenReturn(List.of(refund("PENDING_APPROVAL")));

        boolean completed = completionService.tryComplete(1, 7);

        assertFalse(completed);
        verify(tranMapper, never()).updateStageAtomic(1, TranStage.COMPLETED, TranStage.DELIVERY, 7);
    }

    @Test
    void tryComplete_allConditionsSatisfied_shouldCompleteOnceAndWriteHistory() {
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran(TranStage.DELIVERY));
        when(refundRequestMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(paymentMapper.selectByTranId(1)).thenReturn(List.of(payment("COMPLETED", "FULL", BigDecimal.valueOf(100000))));
        when(invoiceMapper.selectByTranId(1)).thenReturn(List.of(invoice("ISSUED", BigDecimal.valueOf(100000))));
        TDelivery delivery = delivery(10L, DeliveryStatus.COMPLETED);
        when(deliveryMapper.selectByTranId(1)).thenReturn(List.of(delivery));
        when(stockRecordMapper.selectOutboundByDelivery(10L)).thenReturn(outbound(delivery));
        when(tranMapper.updateStageAtomic(1, TranStage.COMPLETED, TranStage.DELIVERY, 7)).thenReturn(1);

        boolean completed = completionService.tryComplete(1, 7);

        assertTrue(completed);
        ArgumentCaptor<TTranHistory> historyCaptor = ArgumentCaptor.forClass(TTranHistory.class);
        verify(tranHistoryMapper).insert(historyCaptor.capture());
        assertEquals("COMPLETED", historyCaptor.getValue().getStage());
        assertEquals("完成条件聚合满足", historyCaptor.getValue().getReason());
        verify(auditRecorder).record(AuditActionEnum.TRAN_COMPLETE, "1");
    }

    @Test
    void tryComplete_completedTransaction_shouldReturnTrueWithoutDuplicateHistory() {
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran(TranStage.COMPLETED));

        boolean completed = completionService.tryComplete(1, 7);

        assertTrue(completed);
        verify(tranMapper, never()).updateStageAtomic(1, TranStage.COMPLETED, TranStage.DELIVERY, 7);
        verify(tranHistoryMapper, never()).insert(any());
    }

    private TTran tran(TranStage stage) {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(stage);
        tran.setMoney(BigDecimal.valueOf(100000));
        tran.setExpectedDate(new Date());
        return tran;
    }

    private TPayment payment(String status, String type, BigDecimal amount) {
        TPayment payment = new TPayment();
        payment.setTranId(1);
        payment.setPaymentStatus(status);
        payment.setPaymentType(type);
        payment.setAmount(amount);
        return payment;
    }

    private TTranInvoice invoice(String status, BigDecimal amount) {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setStatus(status);
        invoice.setAmount(amount);
        return invoice;
    }

    private TDelivery delivery(Long id, DeliveryStatus status) {
        TDelivery delivery = new TDelivery();
        delivery.setId(id);
        delivery.setTranId(1);
        delivery.setVehicleId(100L);
        delivery.setStatus(status.name());
        return delivery;
    }

    private TProductStockRecord outbound(TDelivery delivery) {
        TProductStockRecord record = new TProductStockRecord();
        record.setVehicleId(delivery.getVehicleId());
        record.setType("OUTBOUND");
        record.setSourceType("DELIVERY");
        record.setSourceId(delivery.getId());
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    private TRefundRequest refund(String status) {
        TRefundRequest request = new TRefundRequest();
        request.setTranId(1);
        request.setStatus(status);
        return request;
    }
}
