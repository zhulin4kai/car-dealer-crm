package com.autodealer.crm.modules.sales.testdrive.application.internal;

import com.autodealer.crm.modules.identity.application.api.port.AuthorizationGraphLockDataPort;
import com.autodealer.crm.modules.commerce.inventory.application.api.port.VehicleInventoryDataPort;
import com.autodealer.crm.modules.sales.opportunity.application.api.port.OpportunityDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CancelTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CheckInTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CompleteTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.CreateTestDriveRequest;
import com.autodealer.crm.modules.sales.testdrive.application.api.dto.RescheduleTestDriveRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.enums.OpportunityStage;
import com.autodealer.crm.modules.commerce.inventory.application.api.enums.ProductVehicleStatus;
import com.autodealer.crm.modules.sales.testdrive.application.api.enums.TestDriveStatus;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.testdrive.persistence.mapper.TTestDriveMapper;
import com.autodealer.crm.modules.sales.testdrive.persistence.mapper.TTestDriveStatusHistoryMapper;
import com.autodealer.crm.modules.sales.testdrive.persistence.mapper.TTestDriveVehicleHoldMapper;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductVehicle;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDrive;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDriveStatusHistory;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDriveVehicleHold;
import com.autodealer.crm.modules.sales.testdrive.application.api.query.TestDriveQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.testdrive.application.api.TestDriveService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TestDriveServiceImpl implements TestDriveService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> CANCEL_TYPES = Set.of("CUSTOMER_CANCEL", "STORE_CANCEL", "VEHICLE_UNAVAILABLE", "OTHER");

    private final TTestDriveMapper testDriveMapper;
    private final TTestDriveVehicleHoldMapper vehicleHoldMapper;
    private final TTestDriveStatusHistoryMapper historyMapper;
    private final CustomerDataPort customerMapper;
    private final OpportunityDataPort opportunityMapper;
    private final VehicleInventoryDataPort vehicleMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final EmploymentResponsibilityGuard responsibilityGuard;
    private final AuthorizationGraphLockDataPort graphLocks;

    public TestDriveServiceImpl(TTestDriveMapper testDriveMapper,
                                TTestDriveVehicleHoldMapper vehicleHoldMapper,
                                TTestDriveStatusHistoryMapper historyMapper,
                                CustomerDataPort customerMapper,
                                OpportunityDataPort opportunityMapper,
                                VehicleInventoryDataPort vehicleMapper,
                                CurrentUserProvider currentUserProvider,
                                OperationAuditRecorder auditRecorder,
                                EmploymentResponsibilityGuard responsibilityGuard,
                                AuthorizationGraphLockDataPort graphLocks) {
        this.testDriveMapper = testDriveMapper;
        this.vehicleHoldMapper = vehicleHoldMapper;
        this.historyMapper = historyMapper;
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.vehicleMapper = vehicleMapper;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
        this.responsibilityGuard = responsibilityGuard;
        this.graphLocks = graphLocks;
    }

    @Override
    public PageInfo<TTestDrive> getTestDrivePage(TestDriveQuery query) {
        TestDriveQuery safeQuery = query == null ? new TestDriveQuery() : query;
        if (StringUtils.hasText(safeQuery.getStatus())) {
            parseStatus(safeQuery.getStatus());
        }
        int page = safeQuery.getPage() == null || safeQuery.getPage() < 1 ? 1 : safeQuery.getPage();
        int size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 10 : safeQuery.getSize();
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        safeQuery.setDataScopeUserId(currentUserProvider.getDataScopeUserId());
        PageHelper.startPage(page, size);
        return new PageInfo<>(testDriveMapper.selectByQuery(safeQuery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTestDrive createTestDrive(CreateTestDriveRequest request) {
        validateTimeRange(request.getPlannedStartTime(), request.getPlannedEndTime());
        lockScheduleGraph();
        TCustomer customer = requireAccessibleCustomer(request.getCustomerId());
        TOpportunity opportunity = requireValidOpportunity(request.getOpportunityId(), customer.getId());
        TProductVehicle vehicle = requireAvailableVehicleForUpdate(request.getVehicleId());
        Integer ownerId = customer.getOwnerId() == null ? currentUserProvider.getCurrentUserId() : customer.getOwnerId();
        responsibilityGuard.requireActiveOwner(ownerId);
        validateConflicts(request.getVehicleId(), ownerId, request.getPlannedStartTime(), request.getPlannedEndTime(), null);

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        TTestDrive testDrive = new TTestDrive();
        testDrive.setTestDriveNo(generateTestDriveNo(now));
        testDrive.setCustomerId(customer.getId());
        testDrive.setOpportunityId(opportunity == null ? null : opportunity.getId());
        testDrive.setVehicleId(vehicle.getId());
        testDrive.setOwnerId(ownerId);
        testDrive.setPlannedStartTime(request.getPlannedStartTime());
        testDrive.setPlannedEndTime(request.getPlannedEndTime());
        testDrive.setStatus(TestDriveStatus.SCHEDULED.name());
        testDrive.setContactName(normalizeRequired(request.getContactName(), "客户联系人不能为空"));
        testDrive.setContactPhone(normalizeRequired(request.getContactPhone(), "客户联系电话不能为空"));
        testDrive.setRemark(normalizeNullable(request.getRemark()));
        testDrive.setRescheduleCount(0);
        testDrive.setVersion(0);
        testDrive.setCreateTime(now);
        testDrive.setCreateBy(operatorId);
        testDrive.setUpdateTime(now);
        testDrive.setUpdateBy(operatorId);
        if (testDriveMapper.insert(testDrive) != 1 || testDrive.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "创建试驾预约失败");
        }
        insertHold(testDrive.getId(), vehicle.getId(), request.getPlannedStartTime(), request.getPlannedEndTime(), now, operatorId);
        insertHistory(testDrive.getId(), null, TestDriveStatus.SCHEDULED.name(), "CREATE", "创建试驾预约",
                null, null, request.getPlannedStartTime(), request.getPlannedEndTime(), operatorId, now);
        auditRecorder.record(AuditActionEnum.TEST_DRIVE_CREATE, testDrive.getId().toString());
        return requireAccessibleTestDrive(testDrive.getId());
    }

    @Override
    public TTestDrive getTestDrive(Long id) {
        return requireAccessibleTestDrive(id);
    }

    @Override
    public List<TTestDriveStatusHistory> getHistory(Long id) {
        requireAccessibleTestDrive(id);
        return historyMapper.selectByTestDriveId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTestDrive reschedule(Long id, RescheduleTestDriveRequest request) {
        lockScheduleGraph();
        TTestDrive current = requireAccessibleTestDriveForUpdate(id);
        TestDriveStatus status = parseStatus(current.getStatus());
        if (!status.canReschedule()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前试驾状态不能改期");
        }
        LocalDateTime newStart = request.getPlannedStartTime();
        LocalDateTime newEnd = request.getPlannedEndTime();
        validateTimeRange(newStart, newEnd);
        Long targetVehicleId = request.getVehicleId() == null ? current.getVehicleId() : request.getVehicleId();
        TProductVehicle vehicle = requireAvailableVehicleForUpdate(targetVehicleId);
        validateConflicts(vehicle.getId(), current.getOwnerId(), newStart, newEnd, current.getId());

        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        String reason = normalizeRequired(request.getReason(), "改期原因不能为空");
        vehicleHoldMapper.releaseActiveByTestDriveId(current.getId(), "改期: " + reason, now, operatorId);
        insertHold(current.getId(), vehicle.getId(), newStart, newEnd, now, operatorId);
        int rows = testDriveMapper.updateScheduleIfCurrent(current.getId(), current.getStatus(), TestDriveStatus.RESCHEDULED.name(),
                vehicle.getId(), newStart, newEnd, now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "试驾状态已变化，请刷新后重试");
        }
        insertHistory(current.getId(), current.getStatus(), TestDriveStatus.RESCHEDULED.name(), "RESCHEDULE", reason,
                current.getPlannedStartTime(), current.getPlannedEndTime(), newStart, newEnd, operatorId, now);
        auditRecorder.record(AuditActionEnum.TEST_DRIVE_RESCHEDULE, current.getId().toString());
        return requireAccessibleTestDrive(current.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTestDrive cancel(Long id, CancelTestDriveRequest request) {
        TTestDrive current = requireAccessibleTestDriveForUpdate(id);
        TestDriveStatus status = parseStatus(current.getStatus());
        if (status == TestDriveStatus.CANCELED) {
            return current;
        }
        if (!status.canCancel()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前试驾状态不能取消");
        }
        String cancelType = normalizeCancelType(request.getCancelType());
        String reason = normalizeRequired(request.getReason(), "取消原因不能为空");
        return closeAs(current, TestDriveStatus.CANCELED, "CANCEL", cancelType, reason,
                AuditActionEnum.TEST_DRIVE_CANCEL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTestDrive markNoShow(Long id, CancelTestDriveRequest request) {
        TTestDrive current = requireAccessibleTestDriveForUpdate(id);
        TestDriveStatus status = parseStatus(current.getStatus());
        if (status == TestDriveStatus.NO_SHOW) {
            return current;
        }
        if (status != TestDriveStatus.SCHEDULED && status != TestDriveStatus.RESCHEDULED) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前试驾状态不能标记爽约");
        }
        String reason = normalizeRequired(request.getReason(), "爽约原因不能为空");
        return closeAs(current, TestDriveStatus.NO_SHOW, "NO_SHOW", "NO_SHOW", reason,
                AuditActionEnum.TEST_DRIVE_NO_SHOW);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTestDrive checkIn(Long id, CheckInTestDriveRequest request) {
        TTestDrive current = requireAccessibleTestDriveForUpdate(id);
        TestDriveStatus status = parseStatus(current.getStatus());
        if (!status.canCheckIn()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前试驾状态不能签到");
        }
        LocalDateTime arrivedAt = request.getArrivedAt() == null ? LocalDateTime.now() : request.getArrivedAt();
        if (arrivedAt.isBefore(current.getPlannedStartTime().minusHours(2))) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "未到允许签到时间");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int rows = testDriveMapper.updateCheckInIfCurrent(current.getId(), current.getStatus(), arrivedAt,
                normalizeRequired(request.getCustomerConfirmMethod(), "客户确认方式不能为空"), now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "试驾状态已变化，请刷新后重试");
        }
        insertHistory(current.getId(), current.getStatus(), TestDriveStatus.CHECKED_IN.name(), "CHECK_IN", "客户到店签到",
                null, null, null, null, operatorId, now);
        auditRecorder.record(AuditActionEnum.TEST_DRIVE_CHECK_IN, current.getId().toString());
        return requireAccessibleTestDrive(current.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TTestDrive complete(Long id, CompleteTestDriveRequest request) {
        TTestDrive current = requireAccessibleTestDriveForUpdate(id);
        TestDriveStatus status = parseStatus(current.getStatus());
        if (status != TestDriveStatus.CHECKED_IN) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "试驾未签到，不能完成");
        }
        if (!Boolean.TRUE.equals(request.getSafetyConfirmed())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾完成前必须完成安全确认");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime actualStart = request.getActualStartTime() == null ? current.getActualArriveTime() : request.getActualStartTime();
        LocalDateTime actualEnd = request.getActualEndTime() == null ? now : request.getActualEndTime();
        validateTimeRange(actualStart, actualEnd);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        vehicleHoldMapper.releaseActiveByTestDriveId(current.getId(), "试驾完成", now, operatorId);
        int rows = testDriveMapper.updateCompleteIfCurrent(current.getId(), current.getStatus(), actualStart, actualEnd, now,
                normalizeRequired(request.getResult(), "试驾结果不能为空"),
                normalizeRequired(request.getCustomerFeedback(), "客户反馈不能为空"),
                normalizeRequired(request.getNextAction(), "下一步动作不能为空"),
                now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "试驾状态已变化，请刷新后重试");
        }
        insertHistory(current.getId(), current.getStatus(), TestDriveStatus.COMPLETED.name(), "COMPLETE",
                request.getNextAction(), null, null, null, null, operatorId, now);
        auditRecorder.record(AuditActionEnum.TEST_DRIVE_COMPLETE, current.getId().toString());
        return requireAccessibleTestDrive(current.getId());
    }

    private TTestDrive closeAs(TTestDrive current,
                               TestDriveStatus target,
                               String actionType,
                               String cancelType,
                               String reason,
                               AuditActionEnum auditAction) {
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        vehicleHoldMapper.releaseActiveByTestDriveId(current.getId(), reason, now, operatorId);
        int rows = testDriveMapper.updateCancelIfCurrent(current.getId(), current.getStatus(), target.name(),
                cancelType, reason, now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "试驾状态已变化，请刷新后重试");
        }
        insertHistory(current.getId(), current.getStatus(), target.name(), actionType, reason,
                null, null, null, null, operatorId, now);
        auditRecorder.record(auditAction, current.getId().toString());
        return requireAccessibleTestDrive(current.getId());
    }

    private void validateConflicts(Long vehicleId,
                                   Integer ownerId,
                                   LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   Long excludeTestDriveId) {
        if (vehicleHoldMapper.countActiveConflict(vehicleId, startTime, endTime, excludeTestDriveId) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "该车辆当前时段已有试驾预约");
        }
        if (testDriveMapper.countOwnerScheduleConflict(ownerId, startTime, endTime, excludeTestDriveId) > 0) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "负责销售当前时段已有试驾安排");
        }
    }

    private void lockScheduleGraph() {
        String name = "TEST_DRIVE_SCHEDULE_GUARD";
        if (!name.equals(graphLocks.lockByName(name))) {
            throw new IllegalStateException("试驾排期串行化锁缺失");
        }
    }

    private void insertHold(Long testDriveId,
                            Long vehicleId,
                            LocalDateTime startTime,
                            LocalDateTime endTime,
                            LocalDateTime now,
                            Integer operatorId) {
        TTestDriveVehicleHold hold = new TTestDriveVehicleHold();
        hold.setTestDriveId(testDriveId);
        hold.setVehicleId(vehicleId);
        hold.setStartTime(startTime);
        hold.setEndTime(endTime);
        hold.setStatus("ACTIVE");
        hold.setCreateTime(now);
        hold.setCreateBy(operatorId);
        hold.setUpdateTime(now);
        hold.setUpdateBy(operatorId);
        if (vehicleHoldMapper.insert(hold) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "试驾车辆时间占用失败");
        }
    }

    private void insertHistory(Long testDriveId,
                               String fromStatus,
                               String toStatus,
                               String actionType,
                               String reason,
                               LocalDateTime oldStart,
                               LocalDateTime oldEnd,
                               LocalDateTime newStart,
                               LocalDateTime newEnd,
                               Integer operatorId,
                               LocalDateTime now) {
        TTestDriveStatusHistory history = new TTestDriveStatusHistory();
        history.setTestDriveId(testDriveId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setActionType(actionType);
        history.setReason(reason);
        history.setOldStartTime(oldStart);
        history.setOldEndTime(oldEnd);
        history.setNewStartTime(newStart);
        history.setNewEndTime(newEnd);
        history.setOperateBy(operatorId);
        history.setOperateTime(now);
        if (historyMapper.insert(history) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "试驾状态历史写入失败");
        }
    }

    private TCustomer requireAccessibleCustomer(Integer customerId) {
        if (customerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "客户ID不能为空");
        }
        TCustomer customer = customerMapper.selectScopedById(customerId, currentUserProvider.getDataScopeUserId());
        if (customer == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "客户不存在或无权访问");
        }
        return customer;
    }

    private TOpportunity requireValidOpportunity(Long opportunityId, Integer customerId) {
        if (opportunityId == null) {
            return null;
        }
        TOpportunity opportunity = opportunityMapper.selectById(opportunityId);
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        if (opportunity == null || (dataScopeUserId != null && !dataScopeUserId.equals(opportunity.getOwnerId()))) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "商机不存在或无权访问");
        }
        if (!customerId.equals(opportunity.getCustomerId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾商机必须属于同一客户");
        }
        OpportunityStage stage = parseOpportunityStage(opportunity.getStage());
        if (stage.terminal()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态商机不能预约试驾");
        }
        return opportunity;
    }

    private TProductVehicle requireAvailableVehicleForUpdate(Long vehicleId) {
        if (vehicleId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾车辆不能为空");
        }
        TProductVehicle vehicle = vehicleMapper.selectByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "库存车辆不存在");
        }
        ProductVehicleStatus status = parseVehicleStatus(vehicle.getStatus());
        if (status != ProductVehicleStatus.AVAILABLE) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "当前车辆不可安排试驾");
        }
        return vehicle;
    }

    private TTestDrive requireAccessibleTestDrive(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾ID不能为空");
        }
        TTestDrive testDrive = testDriveMapper.selectById(id);
        if (!isAccessible(testDrive)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "试驾不存在或无权访问");
        }
        return testDrive;
    }

    private TTestDrive requireAccessibleTestDriveForUpdate(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾ID不能为空");
        }
        TTestDrive testDrive = testDriveMapper.selectByIdForUpdate(id);
        if (!isAccessible(testDrive)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "试驾不存在或无权访问");
        }
        return testDrive;
    }

    private boolean isAccessible(TTestDrive testDrive) {
        if (testDrive == null) {
            return false;
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        return dataScopeUserId == null || dataScopeUserId.equals(testDrive.getOwnerId());
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾开始和结束时间不能为空");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "试驾结束时间必须晚于开始时间");
        }
    }

    private TestDriveStatus parseStatus(String value) {
        try {
            return TestDriveStatus.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private ProductVehicleStatus parseVehicleStatus(String value) {
        try {
            return ProductVehicleStatus.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private OpportunityStage parseOpportunityStage(String value) {
        try {
            return OpportunityStage.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private String normalizeCancelType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!CANCEL_TYPES.contains(normalized)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "取消类型编码不合法");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateTestDriveNo(LocalDateTime now) {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "TD" + now.format(NO_DATE_FORMATTER) + suffix;
    }
}
