package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CancelTestDriveRequest;
import com.autodealer.crm.dto.CheckInTestDriveRequest;
import com.autodealer.crm.dto.CompleteTestDriveRequest;
import com.autodealer.crm.dto.CreateTestDriveRequest;
import com.autodealer.crm.dto.RescheduleTestDriveRequest;
import com.autodealer.crm.enums.OpportunityStage;
import com.autodealer.crm.enums.ProductVehicleStatus;
import com.autodealer.crm.enums.TestDriveStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.mapper.TOpportunityMapper;
import com.autodealer.crm.mapper.TProductVehicleMapper;
import com.autodealer.crm.mapper.TTestDriveMapper;
import com.autodealer.crm.mapper.TTestDriveStatusHistoryMapper;
import com.autodealer.crm.mapper.TTestDriveVehicleHoldMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TOpportunity;
import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.model.TTestDrive;
import com.autodealer.crm.model.TTestDriveStatusHistory;
import com.autodealer.crm.model.TTestDriveVehicleHold;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.TestDriveServiceImpl;
import com.autodealer.crm.service.impl.EmploymentResponsibilityGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestDriveServiceImplTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 7, 1, 11, 0);

    @Mock private TTestDriveMapper testDriveMapper;
    @Mock private TTestDriveVehicleHoldMapper vehicleHoldMapper;
    @Mock private TTestDriveStatusHistoryMapper historyMapper;
    @Mock private TCustomerMapper customerMapper;
    @Mock private TOpportunityMapper opportunityMapper;
    @Mock private TProductVehicleMapper vehicleMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @Mock private TAuthorizationGraphLockMapper graphLocks;
    @Mock private EmploymentResponsibilityGuard responsibilityGuard;
    @InjectMocks private TestDriveServiceImpl testDriveService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        lenient().when(graphLocks.lockByName("TEST_DRIVE_SCHEDULE_GUARD")).thenReturn("TEST_DRIVE_SCHEDULE_GUARD");
        lenient().when(customerMapper.selectScopedById(eq(10), nullable(Integer.class))).thenReturn(customer());
        lenient().when(vehicleMapper.selectByIdForUpdate(100L)).thenReturn(vehicle(100L, ProductVehicleStatus.AVAILABLE));
        lenient().when(historyMapper.insert(any())).thenReturn(1);
        lenient().when(vehicleHoldMapper.insert(any())).thenReturn(1);
    }

    @Test
    void createTestDrive_shouldCreateHoldAndHistoryWithoutChangingVehicleStatus() {
        CreateTestDriveRequest request = createRequest();
        when(opportunityMapper.selectById(200L)).thenReturn(opportunity());
        when(vehicleHoldMapper.countActiveConflict(eq(100L), eq(START), eq(END), isNull())).thenReturn(0);
        when(testDriveMapper.countOwnerScheduleConflict(eq(3), eq(START), eq(END), isNull())).thenReturn(0);
        when(testDriveMapper.insert(any())).thenAnswer(invocation -> {
            TTestDrive testDrive = invocation.getArgument(0);
            testDrive.setId(300L);
            return 1;
        });
        TTestDrive persisted = testDrive(300L, TestDriveStatus.SCHEDULED);
        when(testDriveMapper.selectById(300L)).thenReturn(persisted);

        TTestDrive result = testDriveService.createTestDrive(request);

        assertSame(persisted, result);
        ArgumentCaptor<TTestDrive> driveCaptor = ArgumentCaptor.forClass(TTestDrive.class);
        verify(testDriveMapper).insert(driveCaptor.capture());
        assertEquals(10, driveCaptor.getValue().getCustomerId());
        assertEquals(3, driveCaptor.getValue().getOwnerId());
        assertEquals(TestDriveStatus.SCHEDULED.name(), driveCaptor.getValue().getStatus());
        ArgumentCaptor<TTestDriveVehicleHold> holdCaptor = ArgumentCaptor.forClass(TTestDriveVehicleHold.class);
        verify(vehicleHoldMapper).insert(holdCaptor.capture());
        assertEquals(100L, holdCaptor.getValue().getVehicleId());
        assertEquals("ACTIVE", holdCaptor.getValue().getStatus());
        verify(vehicleMapper, never()).reserveIfAvailable(anyLong(), anyString(), anyString(), anyString(), anyLong(), any(), any(), anyInt());
        verify(auditRecorder).record(AuditActionEnum.TEST_DRIVE_CREATE, "300");
    }

    @Test void handoverOwnerCannotReceiveNewTestDrive(){
        when(opportunityMapper.selectById(200L)).thenReturn(opportunity());
        org.mockito.Mockito.doThrow(new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT)).when(responsibilityGuard).requireActiveOwner(3);
        BusinessException error=assertThrows(BusinessException.class,()->testDriveService.createTestDrive(createRequest()));
        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,error.getCodeEnum());verify(testDriveMapper,never()).insert(any());
        verify(vehicleHoldMapper,never()).insert(any());verify(historyMapper,never()).insert(any());verify(auditRecorder,never()).record(any(),anyString());
    }

    @Test
    void createTestDrive_vehicleTimeConflict_shouldRejectBeforeWrite() {
        when(opportunityMapper.selectById(200L)).thenReturn(opportunity());
        when(vehicleHoldMapper.countActiveConflict(eq(100L), eq(START), eq(END), isNull())).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> testDriveService.createTestDrive(createRequest()));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(testDriveMapper, never()).insert(any());
        verify(vehicleHoldMapper, never()).insert(any());
        verify(historyMapper, never()).insert(any());
    }

    @Test
    void createTestDrive_inaccessibleCustomer_shouldRejectBeforeVehicleLock() {
        when(customerMapper.selectScopedById(eq(10), nullable(Integer.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> testDriveService.createTestDrive(createRequest()));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(vehicleMapper, never()).selectByIdForUpdate(anyLong());
        verify(testDriveMapper, never()).insert(any());
    }

    @Test
    void reschedule_conflict_shouldRejectBeforeReleasingCurrentHold() {
        TTestDrive current = testDrive(300L, TestDriveStatus.SCHEDULED);
        when(testDriveMapper.selectByIdForUpdate(300L)).thenReturn(current);
        LocalDateTime newStart = LocalDateTime.of(2026, 7, 2, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 7, 2, 15, 0);
        when(vehicleHoldMapper.countActiveConflict(eq(100L), eq(newStart), eq(newEnd), eq(300L))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> testDriveService.reschedule(300L, rescheduleRequest(newStart, newEnd)));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(vehicleHoldMapper, never()).releaseActiveByTestDriveId(anyLong(), anyString(), any(), anyInt());
        verify(testDriveMapper, never()).updateScheduleIfCurrent(anyLong(), anyString(), anyString(), anyLong(), any(), any(), any(), anyInt());
    }

    @Test
    void cancel_scheduled_shouldReleaseHoldAndRecordReason() {
        TTestDrive current = testDrive(300L, TestDriveStatus.SCHEDULED);
        when(testDriveMapper.selectByIdForUpdate(300L)).thenReturn(current);
        when(testDriveMapper.updateCancelIfCurrent(eq(300L), eq("SCHEDULED"), eq("CANCELED"),
                eq("CUSTOMER_CANCEL"), eq("客户临时有事"), any(), eq(7))).thenReturn(1);
        TTestDrive canceled = testDrive(300L, TestDriveStatus.CANCELED);
        when(testDriveMapper.selectById(300L)).thenReturn(canceled);

        TTestDrive result = testDriveService.cancel(300L, cancelRequest("CUSTOMER_CANCEL", "客户临时有事"));

        assertSame(canceled, result);
        verify(vehicleHoldMapper).releaseActiveByTestDriveId(eq(300L), eq("客户临时有事"), any(), eq(7));
        verify(auditRecorder).record(AuditActionEnum.TEST_DRIVE_CANCEL, "300");
    }

    @Test
    void checkIn_canceledDrive_shouldReject() {
        when(testDriveMapper.selectByIdForUpdate(300L)).thenReturn(testDrive(300L, TestDriveStatus.CANCELED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> testDriveService.checkIn(300L, checkInRequest()));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(testDriveMapper, never()).updateCheckInIfCurrent(anyLong(), anyString(), any(), anyString(), any(), anyInt());
    }

    @Test
    void complete_withoutCheckIn_shouldRejectAndNotReleaseHold() {
        when(testDriveMapper.selectByIdForUpdate(300L)).thenReturn(testDrive(300L, TestDriveStatus.SCHEDULED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> testDriveService.complete(300L, completeRequest()));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(vehicleHoldMapper, never()).releaseActiveByTestDriveId(anyLong(), anyString(), any(), anyInt());
        verify(testDriveMapper, never()).updateCompleteIfCurrent(anyLong(), anyString(), any(), any(), any(),
                anyString(), anyString(), anyString(), any(), anyInt());
    }

    @Test
    void complete_afterCheckIn_shouldReleaseHoldAndNotCreateOrder() {
        TTestDrive current = testDrive(300L, TestDriveStatus.CHECKED_IN);
        current.setActualArriveTime(START.plusMinutes(5));
        when(testDriveMapper.selectByIdForUpdate(300L)).thenReturn(current);
        when(testDriveMapper.updateCompleteIfCurrent(eq(300L), eq("CHECKED_IN"), eq(START.plusMinutes(10)),
                eq(START.plusMinutes(50)), any(), eq("客户满意"), eq("动力满意，要求报价"),
                eq("进入报价流程"), any(), eq(7))).thenReturn(1);
        TTestDrive completed = testDrive(300L, TestDriveStatus.COMPLETED);
        when(testDriveMapper.selectById(300L)).thenReturn(completed);

        TTestDrive result = testDriveService.complete(300L, completeRequest());

        assertSame(completed, result);
        verify(vehicleHoldMapper).releaseActiveByTestDriveId(eq(300L), eq("试驾完成"), any(), eq(7));
        verify(auditRecorder).record(AuditActionEnum.TEST_DRIVE_COMPLETE, "300");
    }

    private CreateTestDriveRequest createRequest() {
        CreateTestDriveRequest request = new CreateTestDriveRequest();
        request.setCustomerId(10);
        request.setOpportunityId(200L);
        request.setVehicleId(100L);
        request.setPlannedStartTime(START);
        request.setPlannedEndTime(END);
        request.setContactName("王先生");
        request.setContactPhone("13800138000");
        request.setRemark("首次试驾");
        return request;
    }

    private RescheduleTestDriveRequest rescheduleRequest(LocalDateTime start, LocalDateTime end) {
        RescheduleTestDriveRequest request = new RescheduleTestDriveRequest();
        request.setVehicleId(100L);
        request.setPlannedStartTime(start);
        request.setPlannedEndTime(end);
        request.setReason("客户改期");
        return request;
    }

    private CancelTestDriveRequest cancelRequest(String type, String reason) {
        CancelTestDriveRequest request = new CancelTestDriveRequest();
        request.setCancelType(type);
        request.setReason(reason);
        return request;
    }

    private CheckInTestDriveRequest checkInRequest() {
        CheckInTestDriveRequest request = new CheckInTestDriveRequest();
        request.setArrivedAt(START.plusMinutes(5));
        request.setCustomerConfirmMethod("ONSITE_CONFIRM");
        return request;
    }

    private CompleteTestDriveRequest completeRequest() {
        CompleteTestDriveRequest request = new CompleteTestDriveRequest();
        request.setSafetyConfirmed(true);
        request.setActualStartTime(START.plusMinutes(10));
        request.setActualEndTime(START.plusMinutes(50));
        request.setResult("客户满意");
        request.setCustomerFeedback("动力满意，要求报价");
        request.setNextAction("进入报价流程");
        return request;
    }

    private TCustomer customer() {
        TCustomer customer = new TCustomer();
        customer.setId(10);
        customer.setOwnerId(3);
        return customer;
    }

    private TOpportunity opportunity() {
        TOpportunity opportunity = new TOpportunity();
        opportunity.setId(200L);
        opportunity.setCustomerId(10);
        opportunity.setOwnerId(3);
        opportunity.setStage(OpportunityStage.TEST_DRIVE_INVITED.name());
        return opportunity;
    }

    private TProductVehicle vehicle(Long id, ProductVehicleStatus status) {
        TProductVehicle vehicle = new TProductVehicle();
        vehicle.setId(id);
        vehicle.setProductId(1L);
        vehicle.setStatus(status.name());
        return vehicle;
    }

    private TTestDrive testDrive(Long id, TestDriveStatus status) {
        TTestDrive testDrive = new TTestDrive();
        testDrive.setId(id);
        testDrive.setCustomerId(10);
        testDrive.setOpportunityId(200L);
        testDrive.setVehicleId(100L);
        testDrive.setOwnerId(3);
        testDrive.setPlannedStartTime(START);
        testDrive.setPlannedEndTime(END);
        testDrive.setStatus(status.name());
        return testDrive;
    }
}
