package com.autodealer.crm.modules.sales.followup.application.internal;

import com.autodealer.crm.modules.sales.followup.application.api.model.FollowRelatedObjectContext;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CancelFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CompleteFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.PostponeFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.enums.CommunicationMethod;
import com.autodealer.crm.modules.sales.followup.application.api.enums.CommunicationRecordStatus;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowRelatedObjectType;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowTaskPriority;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowTaskStatus;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowTaskType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.followup.persistence.mapper.TCommunicationRecordMapper;
import com.autodealer.crm.modules.sales.followup.persistence.mapper.TFollowTaskMapper;
import com.autodealer.crm.modules.sales.followup.application.api.model.TCommunicationRecord;
import com.autodealer.crm.modules.sales.followup.application.api.model.TFollowTask;
import com.autodealer.crm.modules.sales.followup.application.api.query.FollowTaskQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.followup.application.api.FollowTaskService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class FollowTaskServiceImpl implements FollowTaskService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TFollowTaskMapper followTaskMapper;
    private final TCommunicationRecordMapper communicationRecordMapper;
    private final FollowRelatedObjectResolver relatedObjectResolver;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final EmploymentResponsibilityGuard responsibilityGuard;

    public FollowTaskServiceImpl(TFollowTaskMapper followTaskMapper,
                                 TCommunicationRecordMapper communicationRecordMapper,
                                 FollowRelatedObjectResolver relatedObjectResolver,
                                 CurrentUserProvider currentUserProvider,
                                 OperationAuditRecorder auditRecorder,
                                 EmploymentResponsibilityGuard responsibilityGuard) {
        this.followTaskMapper = followTaskMapper;
        this.communicationRecordMapper = communicationRecordMapper;
        this.relatedObjectResolver = relatedObjectResolver;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
        this.responsibilityGuard = responsibilityGuard;
    }

    @Override
    public PageInfo<TFollowTask> getFollowTaskPage(FollowTaskQuery query) {
        return getFollowTaskPage(query, true);
    }

    @Override
    public PageInfo<TFollowTask> getFollowTaskPageReadOnly(FollowTaskQuery query) {
        return getFollowTaskPage(query, false);
    }

    private PageInfo<TFollowTask> getFollowTaskPage(FollowTaskQuery query, boolean markOverdueBeforeQuery) {
        FollowTaskQuery safeQuery = query == null ? new FollowTaskQuery() : query;
        validateQuery(safeQuery);
        int page = safeQuery.getPage() == null || safeQuery.getPage() < 1 ? 1 : safeQuery.getPage();
        int size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 10 : safeQuery.getSize();
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        safeQuery.setDataScopeUserId(currentUserProvider.getDataScopeUserId());
        if (markOverdueBeforeQuery) {
            followTaskMapper.markOverdue(LocalDateTime.now(), safeQuery.getDataScopeUserId());
        }
        PageHelper.startPage(page, size);
        return new PageInfo<>(followTaskMapper.selectByQuery(safeQuery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFollowTask createFollowTask(CreateFollowTaskRequest request) {
        FollowRelatedObjectContext relatedObject = relatedObjectResolver.requireAccessible(
                request.getRelatedObjectType(), request.getRelatedObjectId());
        relatedObjectResolver.validateAssignableOwner(request.getOwnerId());
        responsibilityGuard.requireActiveOwner(request.getOwnerId());
        TFollowTask task = buildTask(request.getTitle(), request.getTaskType(), relatedObject,
                request.getOwnerId(), request.getPriority(), request.getDueTime(), request.getRemindTime(),
                currentUserProvider.getCurrentUserId(), LocalDateTime.now());
        insertTask(task);
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_CREATE, task.getId().toString());
        return requireAccessibleTask(task.getId());
    }

    @Override
    public TFollowTask getFollowTask(Long id) {
        return requireAccessibleTask(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFollowTask startFollowTask(Long id) {
        TFollowTask current = requireAccessibleTaskForUpdate(id);
        FollowTaskStatus status = parseStatus(current.getStatus());
        if (!status.processable()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态跟进任务不能开始处理");
        }
        if (status == FollowTaskStatus.IN_PROGRESS) {
            return current;
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = followTaskMapper.updateStatusIfCurrent(id, current.getStatus(),
                FollowTaskStatus.IN_PROGRESS.name(), LocalDateTime.now(), operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "跟进任务状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_START, id.toString());
        return requireAccessibleTask(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFollowTask postponeFollowTask(Long id, PostponeFollowTaskRequest request) {
        TFollowTask current = requireAccessibleTaskForUpdate(id);
        FollowTaskStatus status = parseStatus(current.getStatus());
        if (!status.processable()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态跟进任务不能延期");
        }
        LocalDateTime newDueTime = request.getNewDueTime();
        if (newDueTime == null || !newDueTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "新的计划时间必须晚于当前时间");
        }
        validateRemindTime(request.getRemindTime(), newDueTime);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime originalDueTime = current.getOriginalDueTime() == null
                ? current.getDueTime() : current.getOriginalDueTime();
        int rows = followTaskMapper.postponeIfCurrent(id, current.getStatus(), newDueTime, request.getRemindTime(),
                normalizeRequired(request.getReason(), "延期原因不能为空"), originalDueTime,
                LocalDateTime.now(), operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "跟进任务状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_POSTPONE, id.toString());
        return requireAccessibleTask(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFollowTask cancelFollowTask(Long id, CancelFollowTaskRequest request) {
        TFollowTask current = requireAccessibleTaskForUpdate(id);
        FollowTaskStatus status = parseStatus(current.getStatus());
        if (status == FollowTaskStatus.CANCELLED) {
            return current;
        }
        if (!status.processable()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态跟进任务不能取消");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = followTaskMapper.cancelIfCurrent(id, current.getStatus(),
                normalizeRequired(request.getReason(), "取消原因不能为空"), LocalDateTime.now(), operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "跟进任务状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_CANCEL, id.toString());
        return requireAccessibleTask(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFollowTask completeFollowTask(Long id, CompleteFollowTaskRequest request) {
        TFollowTask current = requireAccessibleTaskForUpdate(id);
        FollowTaskStatus status = parseStatus(current.getStatus());
        if (!status.processable()) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "终态跟进任务不能重复完成");
        }
        CommunicationMethod method = parseCommunicationMethod(request.getCommunicationMethod());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        TCommunicationRecord record = buildCommunicationRecord(current, method.name(),
                request.getCommunicationTime() == null ? now : request.getCommunicationTime(),
                request.getSummary(), request.getCustomerFeedback(), request.getNextAction(),
                request.getNextFollowTime(), operatorId, now);
        insertCommunicationRecord(record);
        int rows = followTaskMapper.completeIfCurrent(id, current.getStatus(),
                normalizeRequired(request.getResult(), "完成结果不能为空"), record.getId(),
                now, operatorId, now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "跟进任务状态已变化，请刷新后重试");
        }
        FollowRelatedObjectType type = parseObjectType(current.getRelatedObjectType());
        relatedObjectResolver.updateRecentFollowFact(type, current.getRelatedObjectId(),
                record.getCommunicationTime(), record.getSummary(), record.getNextFollowTime(), operatorId);
        createNextTaskIfRequested(current, request, operatorId, now);
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_COMPLETE, id.toString());
        return requireAccessibleTask(id);
    }

    TFollowTask requireAccessibleTask(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "跟进任务ID不能为空");
        }
        TFollowTask task = followTaskMapper.selectById(id);
        if (!isAccessible(task)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "跟进任务不存在或无权访问");
        }
        return task;
    }

    TFollowTask requireAccessibleTaskForUpdate(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "跟进任务ID不能为空");
        }
        TFollowTask task = followTaskMapper.selectByIdForUpdate(id);
        if (!isAccessible(task)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "跟进任务不存在或无权访问");
        }
        return task;
    }

    private boolean isAccessible(TFollowTask task) {
        if (task == null) {
            return false;
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        return dataScopeUserId == null || dataScopeUserId.equals(task.getOwnerId());
    }

    private void validateQuery(FollowTaskQuery query) {
        if (StringUtils.hasText(query.getStatus())) {
            parseStatus(query.getStatus());
        }
        if (StringUtils.hasText(query.getTaskType())) {
            parseTaskType(query.getTaskType());
        }
        if (StringUtils.hasText(query.getRelatedObjectType())) {
            parseObjectType(query.getRelatedObjectType());
        }
    }

    private TFollowTask buildTask(String title,
                                  String taskType,
                                  FollowRelatedObjectContext relatedObject,
                                  Integer ownerId,
                                  String priority,
                                  LocalDateTime dueTime,
                                  LocalDateTime remindTime,
                                  Integer operatorId,
                                  LocalDateTime now) {
        if (dueTime == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "计划时间不能为空");
        }
        validateRemindTime(remindTime, dueTime);
        TFollowTask task = new TFollowTask();
        task.setTitle(normalizeRequired(title, "任务标题不能为空"));
        task.setTaskType(parseTaskType(taskType).name());
        task.setRelatedObjectType(relatedObject.type().name());
        task.setRelatedObjectId(relatedObject.id());
        task.setOwnerId(ownerId);
        task.setPriority(FollowTaskPriority.parseOrDefault(priority).name());
        task.setDueTime(dueTime);
        task.setRemindTime(remindTime);
        task.setStatus(FollowTaskStatus.PENDING.name());
        task.setPostponeCount(0);
        task.setVersion(0);
        task.setCreateTime(now);
        task.setCreateBy(operatorId);
        task.setUpdateTime(now);
        task.setUpdateBy(operatorId);
        return task;
    }

    private TCommunicationRecord buildCommunicationRecord(TFollowTask task,
                                                          String method,
                                                          LocalDateTime communicationTime,
                                                          String summary,
                                                          String customerFeedback,
                                                          String nextAction,
                                                          LocalDateTime nextFollowTime,
                                                          Integer operatorId,
                                                          LocalDateTime now) {
        TCommunicationRecord record = new TCommunicationRecord();
        record.setFollowTaskId(task.getId());
        record.setRelatedObjectType(task.getRelatedObjectType());
        record.setRelatedObjectId(task.getRelatedObjectId());
        record.setOwnerId(task.getOwnerId());
        record.setCommunicationMethod(method);
        record.setCommunicationTime(communicationTime);
        record.setSummary(normalizeRequired(summary, "沟通摘要不能为空"));
        record.setCustomerFeedback(normalizeNullable(customerFeedback));
        record.setNextAction(normalizeNullable(nextAction));
        record.setNextFollowTime(nextFollowTime);
        record.setStatus(CommunicationRecordStatus.ACTIVE.name());
        record.setVersion(0);
        record.setCreateTime(now);
        record.setCreateBy(operatorId);
        record.setUpdateTime(now);
        record.setUpdateBy(operatorId);
        return record;
    }

    private void createNextTaskIfRequested(TFollowTask current,
                                           CompleteFollowTaskRequest request,
                                           Integer operatorId,
                                           LocalDateTime now) {
        if (!Boolean.TRUE.equals(request.getCreateNextTask())) {
            return;
        }
        if (request.getNextTaskDueTime() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "生成下一步任务时计划时间不能为空");
        }
        FollowRelatedObjectContext relatedObject = relatedObjectResolver.requireAccessible(
                current.getRelatedObjectType(), current.getRelatedObjectId());
        TFollowTask nextTask = buildTask(
                StringUtils.hasText(request.getNextTaskTitle()) ? request.getNextTaskTitle() : "下一步跟进",
                StringUtils.hasText(request.getNextTaskType()) ? request.getNextTaskType() : current.getTaskType(),
                relatedObject,
                current.getOwnerId(),
                request.getNextTaskPriority(),
                request.getNextTaskDueTime(),
                request.getNextTaskRemindTime(),
                operatorId,
                now
        );
        insertTask(nextTask);
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_CREATE, nextTask.getId().toString());
    }

    private void insertTask(TFollowTask task) {
        int rows = followTaskMapper.insert(task);
        if (rows != 1 || task.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "创建跟进任务失败");
        }
    }

    private void insertCommunicationRecord(TCommunicationRecord record) {
        int rows = communicationRecordMapper.insert(record);
        if (rows != 1 || record.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "写入沟通记录失败");
        }
    }

    private void validateRemindTime(LocalDateTime remindTime, LocalDateTime dueTime) {
        if (remindTime != null && dueTime != null && remindTime.isAfter(dueTime)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "提醒时间不能晚于计划时间");
        }
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

    private FollowTaskStatus parseStatus(String value) {
        try {
            return FollowTaskStatus.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private FollowTaskType parseTaskType(String value) {
        try {
            return FollowTaskType.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private FollowRelatedObjectType parseObjectType(String value) {
        try {
            return FollowRelatedObjectType.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private CommunicationMethod parseCommunicationMethod(String value) {
        try {
            return CommunicationMethod.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }
}
