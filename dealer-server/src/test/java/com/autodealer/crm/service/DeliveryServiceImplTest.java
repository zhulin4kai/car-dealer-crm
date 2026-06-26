package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CreateDeliveryRequest;
import com.autodealer.crm.dto.SignDeliveryRequest;
import com.autodealer.crm.enums.DeliveryStatus;
import com.autodealer.crm.enums.ProductVehicleStatus;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TDeliveryCheckItemMapper;
import com.autodealer.crm.mapper.TDeliveryMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TProductVehicleMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.model.TDeliveryCheckItem;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.DeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock private TDeliveryMapper deliveryMapper;
    @Mock private TDeliveryCheckItemMapper checkItemMapper;
    @Mock private TTranMapper tranMapper;
    @Mock private TProductVehicleMapper vehicleMapper;
    @Mock private TProductStockRecordMapper stockRecordMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @InjectMocks private DeliveryServiceImpl deliveryService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        lenient().when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.all());
    }

    @Test
    void createDelivery_success_shouldCreateDeliveryAndDefaultChecklist() {
        CreateDeliveryRequest request = createDeliveryRequest();
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran(1, TranStage.DELIVERY));
        when(deliveryMapper.selectActiveByTranId(1)).thenReturn(null);
        when(vehicleMapper.selectByIdForUpdate(100L)).thenReturn(vehicle());
        when(deliveryMapper.insert(any())).thenAnswer(invocation -> {
            TDelivery delivery = invocation.getArgument(0);
            delivery.setId(10L);
            return 1;
        });
        when(checkItemMapper.insert(any())).thenReturn(1);
        TDelivery created = delivery(10L, DeliveryStatus.PENDING_PREPARE);
        when(deliveryMapper.selectById(10L)).thenReturn(created);

        TDelivery result = deliveryService.createDelivery(request);

        assertSame(created, result);
        ArgumentCaptor<TDeliveryCheckItem> itemCaptor = ArgumentCaptor.forClass(TDeliveryCheckItem.class);
        verify(checkItemMapper, org.mockito.Mockito.times(5)).insert(itemCaptor.capture());
        List<String> codes = itemCaptor.getAllValues().stream()
                .map(TDeliveryCheckItem::getItemCode)
                .toList();
        assertEquals(List.of("VEHICLE_READY", "DOCUMENTS_READY", "PAYMENT_READY",
                "INVOICE_READY", "CUSTOMER_APPOINTMENT"), codes);
        verify(auditRecorder).record(AuditActionEnum.DELIVERY_CREATE, "10");
    }

    @Test
    void signDelivery_incompleteChecklist_shouldRejectBeforeOutbound() {
        when(deliveryMapper.selectByIdForUpdate(10L)).thenReturn(delivery(10L, DeliveryStatus.PREPARING));
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran(1, TranStage.DELIVERY));
        when(checkItemMapper.countIncompleteByDeliveryId(10L)).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deliveryService.signDelivery(10L, signRequest()));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(vehicleMapper, never()).outboundIfCurrent(anyLong(), anyString(), any(), any());
        verify(stockRecordMapper, never()).insert(any());
        verify(deliveryMapper, never()).signIfCurrent(anyLong(), anyString(), any(), anyString(),
                any(), anyString(), anyString(), any(), any());
    }

    @Test
    void signDelivery_success_shouldOutboundVehicleAndNotCompleteTransaction() {
        SignDeliveryRequest request = signRequest();
        when(deliveryMapper.selectByIdForUpdate(10L)).thenReturn(delivery(10L, DeliveryStatus.PREPARING));
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran(1, TranStage.DELIVERY));
        when(checkItemMapper.countIncompleteByDeliveryId(10L)).thenReturn(0);
        when(vehicleMapper.selectByIdForUpdate(100L)).thenReturn(vehicle());
        when(stockRecordMapper.selectLatestReserveByVehicle(100L, "ORDER", 1L)).thenReturn(reserveRecord());
        when(stockRecordMapper.selectReleaseByRelatedRecordId(700L)).thenReturn(null);
        when(vehicleMapper.outboundIfCurrent(eq(100L), eq("ORDER_RESERVED"), any(), eq(7))).thenReturn(1);
        when(stockRecordMapper.insert(any())).thenReturn(1);
        when(deliveryMapper.signIfCurrent(eq(10L), eq("PREPARING"), eq(request.getSignedAt()),
                eq("王先生"), eq(request.getSignedAt()), eq("PAPER"), eq("delivery-001.pdf"),
                any(), eq(7))).thenReturn(1);
        TDelivery completed = delivery(10L, DeliveryStatus.COMPLETED);
        when(deliveryMapper.selectById(10L)).thenReturn(completed);

        TDelivery result = deliveryService.signDelivery(10L, request);

        assertSame(completed, result);
        ArgumentCaptor<TProductStockRecord> recordCaptor = ArgumentCaptor.forClass(TProductStockRecord.class);
        verify(stockRecordMapper).insert(recordCaptor.capture());
        TProductStockRecord record = recordCaptor.getValue();
        assertEquals("OUTBOUND", record.getType());
        assertEquals("DELIVERY", record.getSourceType());
        assertEquals(10L, record.getSourceId());
        assertEquals(700L, record.getRelatedRecordId());
        assertEquals("ORDER_RESERVED", record.getBeforeStatus());
        assertEquals("OUTBOUND", record.getAfterStatus());
        verify(tranMapper, never()).updateStageAtomic(any(), any(), any(), any());
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_STOCK_OUT, "100");
        verify(auditRecorder).record(AuditActionEnum.DELIVERY_COMPLETE, "10");
    }

    @Test
    void signDelivery_completed_shouldReturnCurrentWithoutDuplicateOutbound() {
        TDelivery completed = delivery(10L, DeliveryStatus.COMPLETED);
        when(deliveryMapper.selectByIdForUpdate(10L)).thenReturn(completed);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran(1, TranStage.DELIVERY));
        when(deliveryMapper.selectById(10L)).thenReturn(completed);

        TDelivery result = deliveryService.signDelivery(10L, signRequest());

        assertSame(completed, result);
        verify(vehicleMapper, never()).outboundIfCurrent(anyLong(), anyString(), any(), any());
        verify(stockRecordMapper, never()).insert(any());
    }

    private CreateDeliveryRequest createDeliveryRequest() {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setTranId(1);
        request.setVehicleId(100L);
        request.setPlannedDeliveryTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        return request;
    }

    private SignDeliveryRequest signRequest() {
        SignDeliveryRequest request = new SignDeliveryRequest();
        request.setSignerName("王先生");
        request.setSignedAt(LocalDateTime.of(2026, 7, 1, 15, 30));
        request.setSignMethod("PAPER");
        request.setSignEvidence("delivery-001.pdf");
        return request;
    }

    private TTran tran(Integer id, TranStage stage) {
        TTran tran = new TTran();
        tran.setId(id);
        tran.setCustomerId(20);
        tran.setStage(stage);
        return tran;
    }

    private TDelivery delivery(Long id, DeliveryStatus status) {
        TDelivery delivery = new TDelivery();
        delivery.setId(id);
        delivery.setTranId(1);
        delivery.setCustomerId(20);
        delivery.setVehicleId(100L);
        delivery.setStatus(status.name());
        return delivery;
    }

    private TProductVehicle vehicle() {
        TProductVehicle vehicle = new TProductVehicle();
        vehicle.setId(100L);
        vehicle.setProductId(5L);
        vehicle.setStatus(ProductVehicleStatus.ORDER_RESERVED.name());
        vehicle.setSourceType("ORDER");
        vehicle.setSourceId(1L);
        return vehicle;
    }

    private TProductStockRecord reserveRecord() {
        TProductStockRecord record = new TProductStockRecord();
        record.setId(700L);
        record.setProductId(5L);
        record.setVehicleId(100L);
        record.setType("RESERVE");
        record.setSourceType("ORDER");
        record.setSourceId(1L);
        record.setBeforeStatus("AVAILABLE");
        record.setAfterStatus("ORDER_RESERVED");
        return record;
    }
}
