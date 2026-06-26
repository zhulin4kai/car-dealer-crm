package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CreateProductVehicleRequest;
import com.autodealer.crm.dto.ReleaseProductVehicleRequest;
import com.autodealer.crm.dto.ReserveProductVehicleRequest;
import com.autodealer.crm.enums.ProductVehicleStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TProductVehicleMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.ProductVehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVehicleServiceImplTest {

    @Mock private TProductVehicleMapper vehicleMapper;
    @Mock private TProductMapper productMapper;
    @Mock private TProductStockRecordMapper stockRecordMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @InjectMocks private ProductVehicleServiceImpl productVehicleService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(9);
    }

    @Test
    void inboundVehicle_duplicateVin_shouldRejectBeforeInsert() {
        CreateProductVehicleRequest request = createInboundRequest();
        when(productMapper.selectById(1L)).thenReturn(new TProduct());
        when(vehicleMapper.selectByVin("VIN001")).thenReturn(vehicle(10L, ProductVehicleStatus.AVAILABLE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productVehicleService.inboundVehicle(request));

        assertEquals(CodeEnum.DUPLICATE, ex.getCodeEnum());
        verify(vehicleMapper, never()).insert(any());
        verify(productMapper, never()).updateStock(anyLong(), any());
    }

    @Test
    void inboundVehicle_success_shouldCreateVehicleStockRecordAndSummaryStock() {
        CreateProductVehicleRequest request = createInboundRequest();
        when(productMapper.selectById(1L)).thenReturn(new TProduct());
        when(vehicleMapper.selectByVin("VIN001")).thenReturn(null);
        when(vehicleMapper.insert(any())).thenAnswer(invocation -> {
            TProductVehicle vehicle = invocation.getArgument(0);
            vehicle.setId(100L);
            return 1;
        });
        when(productMapper.updateStock(1L, 1)).thenReturn(1);
        when(stockRecordMapper.insert(any())).thenReturn(1);
        TProductVehicle inserted = vehicle(100L, ProductVehicleStatus.AVAILABLE);
        when(vehicleMapper.selectById(100L)).thenReturn(inserted);

        TProductVehicle result = productVehicleService.inboundVehicle(request);

        assertSame(inserted, result);
        ArgumentCaptor<TProductStockRecord> recordCaptor = ArgumentCaptor.forClass(TProductStockRecord.class);
        verify(stockRecordMapper).insert(recordCaptor.capture());
        assertEquals("INBOUND", recordCaptor.getValue().getType());
        assertEquals(100L, recordCaptor.getValue().getVehicleId());
        assertEquals("AVAILABLE", recordCaptor.getValue().getAfterStatus());
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_STOCK_IN, "100");
    }

    @Test
    void reserveVehicle_duplicateSource_shouldReturnExistingReservation() {
        ReserveProductVehicleRequest request = createReserveRequest();
        TProductVehicle existing = vehicle(99L, ProductVehicleStatus.ORDER_RESERVED);
        when(vehicleMapper.selectActiveBySource("ORDER", 500L)).thenReturn(existing);

        TProductVehicle result = productVehicleService.reserveVehicle(100L, request);

        assertSame(existing, result);
        verify(vehicleMapper, never()).selectByIdForUpdate(anyLong());
        verify(productMapper, never()).updateStock(anyLong(), any());
    }

    @Test
    void reserveVehicle_casFailure_shouldRejectBeforeStockChange() {
        ReserveProductVehicleRequest request = createReserveRequest();
        when(vehicleMapper.selectActiveBySource("ORDER", 500L)).thenReturn(null);
        when(vehicleMapper.selectByIdForUpdate(100L)).thenReturn(vehicle(100L, ProductVehicleStatus.AVAILABLE));
        when(vehicleMapper.reserveIfAvailable(eq(100L), eq("ORDER_RESERVED"), anyString(),
                eq("ORDER"), eq(500L), any(), any(), eq(9))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productVehicleService.reserveVehicle(100L, request));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(productMapper, never()).updateStock(anyLong(), any());
        verify(stockRecordMapper, never()).insert(any());
    }

    @Test
    void reserveVehicle_success_shouldWriteReserveRecord() {
        ReserveProductVehicleRequest request = createReserveRequest();
        when(vehicleMapper.selectActiveBySource("ORDER", 500L)).thenReturn(null);
        when(vehicleMapper.selectByIdForUpdate(100L)).thenReturn(vehicle(100L, ProductVehicleStatus.AVAILABLE));
        when(vehicleMapper.reserveIfAvailable(eq(100L), eq("ORDER_RESERVED"), anyString(),
                eq("ORDER"), eq(500L), any(), any(), eq(9))).thenReturn(1);
        when(productMapper.updateStock(1L, -1)).thenReturn(1);
        when(stockRecordMapper.insert(any())).thenReturn(1);
        TProductVehicle reserved = vehicle(100L, ProductVehicleStatus.ORDER_RESERVED);
        when(vehicleMapper.selectById(100L)).thenReturn(reserved);

        TProductVehicle result = productVehicleService.reserveVehicle(100L, request);

        assertSame(reserved, result);
        ArgumentCaptor<TProductStockRecord> recordCaptor = ArgumentCaptor.forClass(TProductStockRecord.class);
        verify(stockRecordMapper).insert(recordCaptor.capture());
        assertEquals("RESERVE", recordCaptor.getValue().getType());
        assertEquals(-1, recordCaptor.getValue().getQuantity());
        assertEquals("ORDER", recordCaptor.getValue().getSourceType());
        assertEquals("AVAILABLE", recordCaptor.getValue().getBeforeStatus());
        assertEquals("ORDER_RESERVED", recordCaptor.getValue().getAfterStatus());
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_STOCK_RESERVE, "100");
    }

    @Test
    void releaseVehicle_withoutOriginalReserveRecord_shouldReject() {
        ReleaseProductVehicleRequest request = createReleaseRequest();
        when(stockRecordMapper.selectById(700L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productVehicleService.releaseVehicle(100L, request));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(vehicleMapper, never()).selectByIdForUpdate(anyLong());
    }

    @Test
    void releaseVehicle_duplicateRelease_shouldReturnCurrentVehicleWithoutStockChange() {
        ReleaseProductVehicleRequest request = createReleaseRequest();
        when(stockRecordMapper.selectById(700L)).thenReturn(reserveRecord());
        when(stockRecordMapper.selectReleaseByRelatedRecordId(700L)).thenReturn(new TProductStockRecord());
        TProductVehicle current = vehicle(100L, ProductVehicleStatus.AVAILABLE);
        when(vehicleMapper.selectById(100L)).thenReturn(current);

        TProductVehicle result = productVehicleService.releaseVehicle(100L, request);

        assertSame(current, result);
        verify(productMapper, never()).updateStock(anyLong(), any());
        verify(stockRecordMapper, never()).insert(any());
    }

    @Test
    void releaseVehicle_success_shouldWriteReleaseRecordAndRestoreStock() {
        ReleaseProductVehicleRequest request = createReleaseRequest();
        when(stockRecordMapper.selectById(700L)).thenReturn(reserveRecord());
        when(stockRecordMapper.selectReleaseByRelatedRecordId(700L)).thenReturn(null);
        when(vehicleMapper.selectByIdForUpdate(100L)).thenReturn(vehicle(100L, ProductVehicleStatus.ORDER_RESERVED));
        when(vehicleMapper.releaseIfCurrent(eq(100L), eq("ORDER_RESERVED"), any(), eq(9))).thenReturn(1);
        when(productMapper.updateStock(1L, 1)).thenReturn(1);
        when(stockRecordMapper.insert(any())).thenReturn(1);
        TProductVehicle released = vehicle(100L, ProductVehicleStatus.AVAILABLE);
        when(vehicleMapper.selectById(100L)).thenReturn(released);

        TProductVehicle result = productVehicleService.releaseVehicle(100L, request);

        assertSame(released, result);
        ArgumentCaptor<TProductStockRecord> recordCaptor = ArgumentCaptor.forClass(TProductStockRecord.class);
        verify(stockRecordMapper).insert(recordCaptor.capture());
        assertEquals("RELEASE", recordCaptor.getValue().getType());
        assertEquals(1, recordCaptor.getValue().getQuantity());
        assertEquals(700L, recordCaptor.getValue().getRelatedRecordId());
        assertEquals("ORDER_RESERVED", recordCaptor.getValue().getBeforeStatus());
        assertEquals("AVAILABLE", recordCaptor.getValue().getAfterStatus());
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_STOCK_RELEASE, "100");
    }

    private CreateProductVehicleRequest createInboundRequest() {
        CreateProductVehicleRequest request = new CreateProductVehicleRequest();
        request.setProductId(1L);
        request.setVin("VIN001");
        request.setColor("黑色");
        request.setConfiguration("高配");
        request.setLocation("A-01");
        request.setRemark("厂家到店");
        return request;
    }

    private ReserveProductVehicleRequest createReserveRequest() {
        ReserveProductVehicleRequest request = new ReserveProductVehicleRequest();
        request.setHoldType("ORDER");
        request.setSourceType("ORDER");
        request.setSourceId(500L);
        request.setRemark("订单成立占用");
        return request;
    }

    private ReleaseProductVehicleRequest createReleaseRequest() {
        ReleaseProductVehicleRequest request = new ReleaseProductVehicleRequest();
        request.setReserveRecordId(700L);
        request.setReason("订单取消释放");
        return request;
    }

    private TProductVehicle vehicle(Long id, ProductVehicleStatus status) {
        TProductVehicle vehicle = new TProductVehicle();
        vehicle.setId(id);
        vehicle.setProductId(1L);
        vehicle.setStatus(status.name());
        return vehicle;
    }

    private TProductStockRecord reserveRecord() {
        TProductStockRecord record = new TProductStockRecord();
        record.setId(700L);
        record.setProductId(1L);
        record.setVehicleId(100L);
        record.setQuantity(-1);
        record.setType("RESERVE");
        record.setSourceType("ORDER");
        record.setSourceId(500L);
        record.setBeforeStatus("AVAILABLE");
        record.setAfterStatus("ORDER_RESERVED");
        return record;
    }
}
