package com.autodealer.crm.modules.sales.followup.application.internal;

import com.autodealer.crm.modules.sales.followup.application.api.model.FollowRelatedObjectContext;
import com.autodealer.crm.modules.identity.application.api.EmploymentResponsibilityGuard;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CorrectCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.VoidCommunicationRecordRequest;
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
import com.autodealer.crm.modules.sales.followup.application.api.query.CommunicationRecordQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.followup.application.api.CommunicationRecordService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CommunicationRecordServiceImpl implements CommunicationRecordService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TCommunicationRecordMapper communicationRecordMapper;
    private final TFollowTaskMapper followTaskMapper;
    private final FollowRelatedObjectResolver relatedObjectResolver;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final EmploymentResponsibilityGuard responsibilityGuard;

    public CommunicationRecordServiceImpl(TCommunicationRecordMapper communicationRecordMapper,
                                          TFollowTaskMapper followTaskMapper,
                                          FollowRelatedObjectResolver relatedObjectResolver,
                                          CurrentUserProvider currentUserProvider,
                                          OperationAuditRecorder auditRecorder,
                                          EmploymentResponsibilityGuard responsibilityGuard) {
        this.communicationRecordMapper = communicationRecordMapper;
        this.followTaskMapper = followTaskMapper;
        this.relatedObjectResolver = relatedObjectResolver;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
        this.responsibilityGuard = responsibilityGuard;
    }

    @Override
    public PageInfo<TCommunicationRecord> getCommunicationRecordPage(CommunicationRecordQuery query) {
        CommunicationRecordQuery safeQuery = query == null ? new CommunicationRecordQuery() : query;
        validateQuery(safeQuery);
        int page = safeQuery.getPage() == null || safeQuery.getPage() < 1 ? 1 : safeQuery.getPage();
        int size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 10 : safeQuery.getSize();
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        safeQuery.setDataScopeUserId(currentUserProvider.getDataScopeUserId());
        PageHelper.startPage(page, size);
        return new PageInfo<>(communicationRecordMapper.selectByQuery(safeQuery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCommunicationRecord createCommunicationRecord(CreateCommunicationRecordRequest request) {
        CommunicationMethod method = parseCommunicationMethod(request.getCommunicationMethod());
        FollowRelatedObjectContext relatedObject = relatedObjectResolver.requireAccessible(
                request.getRelatedObjectType(), request.getRelatedObjectId());
        TFollowTask task = resolveLinkedTask(request.getFollowTaskId(), relatedObject);
        Integer ownerId = task == null ? relatedObject.ownerId() : task.getOwnerId();
        relatedObjectResolver.validateAssignableOwner(ownerId);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        TCommunicationRecord record = buildRecord(task == null ? null : task.getId(), null, relatedObject,
                ownerId, method.name(), request.getCommunicationTime() == null ? now : request.getCommunicationTime(),
                request.getSummary(), request.getCustomerFeedback(), request.getNextAction(),
                request.getNextFollowTime(), operatorId, now);
        insertRecord(record);
        relatedObjectResolver.updateRecentFollowFact(relatedObject.type(), relatedObject.id(),
                record.getCommunicationTime(), record.getSummary(), record.getNextFollowTime(), operatorId);
        createNextTaskIfRequested(relatedObject, ownerId, request.getCreateNextTask(), request.getNextTaskType(),
                request.getNextTaskTitle(), request.getNextTaskPriority(), request.getNextTaskDueTime(),
                request.getNextTaskRemindTime(), operatorId, now);
        auditRecorder.record(AuditActionEnum.COMMUNICATION_RECORD_CREATE, record.getId().toString());
        return requireAccessibleRecord(record.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCommunicationRecord correctCommunicationRecord(Long id, CorrectCommunicationRecordRequest request) {
        TCommunicationRecord current = requireAccessibleRecordForUpdate(id);
        CommunicationRecordStatus status = parseRecordStatus(current.getStatus());
        if (status != CommunicationRecordStatus.ACTIVE) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有有效沟通记录可以更正");
        }
        CommunicationMethod method = parseCommunicationMethod(request.getCommunicationMethod());
        Integer operatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int rows = communicationRecordMapper.markCorrected(id, CommunicationRecordStatus.ACTIVE.name(),
                normalizeRequired(request.getCorrectionReason(), "更正原因不能为空"), now, operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "沟通记录状态已变化，请刷新后重试");
        }
        FollowRelatedObjectContext relatedObject = relatedObjectResolver.requireAccessible(
                current.getRelatedObjectType(), current.getRelatedObjectId());
        TCommunicationRecord corrected = buildRecord(current.getFollowTaskId(), current.getId(), relatedObject,
                current.getOwnerId(), method.name(),
                request.getCommunicationTime() == null ? current.getCommunicationTime() : request.getCommunicationTime(),
                request.getSummary(), request.getCustomerFeedback(), request.getNextAction(),
                request.getNextFollowTime(), operatorId, now);
        insertRecord(corrected);
        relatedObjectResolver.updateRecentFollowFact(relatedObject.type(), relatedObject.id(),
                corrected.getCommunicationTime(), corrected.getSummary(), corrected.getNextFollowTime(), operatorId);
        auditRecorder.record(AuditActionEnum.COMMUNICATION_RECORD_CORRECT, id.toString());
        return requireAccessibleRecord(corrected.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCommunicationRecord voidCommunicationRecord(Long id, VoidCommunicationRecordRequest request) {
        TCommunicationRecord current = requireAccessibleRecordForUpdate(id);
        CommunicationRecordStatus status = parseRecordStatus(current.getStatus());
        if (status == CommunicationRecordStatus.VOIDED) {
            return current;
        }
        if (status != CommunicationRecordStatus.ACTIVE) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "只有有效沟通记录可以作废");
        }
        Integer operatorId = currentUserProvider.getCurrentUserId();
        int rows = communicationRecordMapper.voidIfActive(id, CommunicationRecordStatus.ACTIVE.name(),
                normalizeRequired(request.getReason(), "作废原因不能为空"), LocalDateTime.now(), operatorId);
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "沟通记录状态已变化，请刷新后重试");
        }
        auditRecorder.record(AuditActionEnum.COMMUNICATION_RECORD_VOID, id.toString());
        return requireAccessibleRecord(id);
    }

    private TFollowTask resolveLinkedTask(Long followTaskId, FollowRelatedObjectContext relatedObject) {
        if (followTaskId == null) {
            return null;
        }
        TFollowTask task = followTaskMapper.selectById(followTaskId);
        if (!isAccessibleTask(task)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "跟进任务不存在或无权访问");
        }
        if (!task.getRelatedObjectType().equals(relatedObject.type().name())
                || !task.getRelatedObjectId().equals(relatedObject.id())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "沟通记录关联对象必须与跟进任务一致");
        }
        return task;
    }

    private TCommunicationRecord requireAccessibleRecord(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "沟通记录ID不能为空");
        }
        TCommunicationRecord record = communicationRecordMapper.selectById(id);
        if (!isAccessibleRecord(record)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "沟通记录不存在或无权访问");
        }
        return record;
    }

    private TCommunicationRecord requireAccessibleRecordForUpdate(Long id) {
        if (id == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "沟通记录ID不能为空");
        }
        TCommunicationRecord record = communicationRecordMapper.selectByIdForUpdate(id);
        if (!isAccessibleRecord(record)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "沟通记录不存在或无权访问");
        }
        return record;
    }

    private boolean isAccessibleRecord(TCommunicationRecord record) {
        if (record == null) {
            return false;
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        return dataScopeUserId == null || dataScopeUserId.equals(record.getOwnerId());
    }

    private boolean isAccessibleTask(TFollowTask task) {
        if (task == null) {
            return false;
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        return dataScopeUserId == null || dataScopeUserId.equals(task.getOwnerId());
    }

    private TCommunicationRecord buildRecord(Long followTaskId,
                                             Long parentRecordId,
                                             FollowRelatedObjectContext relatedObject,
                                             Integer ownerId,
                                             String method,
                                             LocalDateTime communicationTime,
                                             String summary,
                                             String customerFeedback,
                                             String nextAction,
                                             LocalDateTime nextFollowTime,
                                             Integer operatorId,
                                             LocalDateTime now) {
        TCommunicationRecord record = new TCommunicationRecord();
        record.setFollowTaskId(followTaskId);
        record.setParentRecordId(parentRecordId);
        record.setRelatedObjectType(relatedObject.type().name());
        record.setRelatedObjectId(relatedObject.id());
        record.setOwnerId(ownerId);
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

    private void createNextTaskIfRequested(FollowRelatedObjectContext relatedObject,
                                           Integer ownerId,
                                           Boolean createNextTask,
                                           String taskType,
                                           String title,
                                           String priority,
                                           LocalDateTime dueTime,
                                           LocalDateTime remindTime,
                                           Integer operatorId,
                                           LocalDateTime now) {
        if (!Boolean.TRUE.equals(createNextTask)) {
            return;
        }
        responsibilityGuard.requireActiveOwner(ownerId);
        if (dueTime == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "生成下一步任务时计划时间不能为空");
        }
        if (remindTime != null && remindTime.isAfter(dueTime)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "提醒时间不能晚于计划时间");
        }
        TFollowTask task = new TFollowTask();
        task.setTitle(StringUtils.hasText(title) ? title.trim() : "下一步跟进");
        task.setTaskType(parseTaskType(StringUtils.hasText(taskType) ? taskType : FollowTaskType.PHONE_FOLLOW_UP.name()).name());
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
        int rows = followTaskMapper.insert(task);
        if (rows != 1 || task.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "创建下一步跟进任务失败");
        }
        auditRecorder.record(AuditActionEnum.FOLLOW_TASK_CREATE, task.getId().toString());
    }

    private void insertRecord(TCommunicationRecord record) {
        int rows = communicationRecordMapper.insert(record);
        if (rows != 1 || record.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "写入沟通记录失败");
        }
    }

    private void validateQuery(CommunicationRecordQuery query) {
        if (StringUtils.hasText(query.getStatus())) {
            parseRecordStatus(query.getStatus());
        }
        if (StringUtils.hasText(query.getRelatedObjectType())) {
            parseObjectType(query.getRelatedObjectType());
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

    private CommunicationMethod parseCommunicationMethod(String value) {
        try {
            return CommunicationMethod.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }

    private CommunicationRecordStatus parseRecordStatus(String value) {
        try {
            return CommunicationRecordStatus.parse(value);
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
}
