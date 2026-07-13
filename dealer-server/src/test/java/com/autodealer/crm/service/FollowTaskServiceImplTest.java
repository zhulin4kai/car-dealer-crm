package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CompleteFollowTaskRequest;
import com.autodealer.crm.dto.CreateFollowTaskRequest;
import com.autodealer.crm.dto.PostponeFollowTaskRequest;
import com.autodealer.crm.enums.FollowRelatedObjectType;
import com.autodealer.crm.enums.FollowTaskStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TCommunicationRecordMapper;
import com.autodealer.crm.mapper.TFollowTaskMapper;
import com.autodealer.crm.model.TCommunicationRecord;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.query.FollowTaskQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.FollowRelatedObjectContext;
import com.autodealer.crm.service.impl.FollowRelatedObjectResolver;
import com.autodealer.crm.service.impl.FollowTaskServiceImpl;
import com.autodealer.crm.service.impl.EmploymentResponsibilityGuard;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowTaskServiceImplTest {

    private static final LocalDateTime DUE_TIME = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Mock private TFollowTaskMapper followTaskMapper;
    @Mock private TCommunicationRecordMapper communicationRecordMapper;
    @Mock private FollowRelatedObjectResolver relatedObjectResolver;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @Mock private EmploymentResponsibilityGuard responsibilityGuard;
    @InjectMocks private FollowTaskServiceImpl followTaskService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
    }

    @Test
    void createFollowTask_shouldUseServerUserAndValidateOwnerAndObject() {
        CreateFollowTaskRequest request = createRequest();
        FollowRelatedObjectContext context = new FollowRelatedObjectContext(
                FollowRelatedObjectType.CUSTOMER, 10L, 3, "王先生");
        when(relatedObjectResolver.requireAccessible("CUSTOMER", 10L)).thenReturn(context);
        when(followTaskMapper.insert(any())).thenAnswer(invocation -> {
            TFollowTask task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        });
        TFollowTask persisted = task(100L, FollowTaskStatus.PENDING);
        when(followTaskMapper.selectById(100L)).thenReturn(persisted);

        TFollowTask result = followTaskService.createFollowTask(request);

        assertSame(persisted, result);
        verify(relatedObjectResolver).validateAssignableOwner(3);
        ArgumentCaptor<TFollowTask> taskCaptor = ArgumentCaptor.forClass(TFollowTask.class);
        verify(followTaskMapper).insert(taskCaptor.capture());
        assertEquals("电话回访", taskCaptor.getValue().getTitle());
        assertEquals("PHONE_FOLLOW_UP", taskCaptor.getValue().getTaskType());
        assertEquals("CUSTOMER", taskCaptor.getValue().getRelatedObjectType());
        assertEquals(3, taskCaptor.getValue().getOwnerId());
        assertEquals(7, taskCaptor.getValue().getCreateBy());
        verify(auditRecorder).record(AuditActionEnum.FOLLOW_TASK_CREATE, "100");
    }

    @Test
    void createFollowTask_inaccessibleObject_shouldRejectBeforeInsert() {
        CreateFollowTaskRequest request = createRequest();
        when(relatedObjectResolver.requireAccessible("CUSTOMER", 10L))
                .thenThrow(new BusinessException(CodeEnum.ACCESS_DENIED, "客户不存在或无权访问"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> followTaskService.createFollowTask(request));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(followTaskMapper, never()).insert(any());
    }

    @Test
    void listFollowTasks_shouldMarkOverdueBeforeQuery() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(3);
        when(followTaskMapper.selectByQuery(any())).thenReturn(List.of(task(1L, FollowTaskStatus.OVERDUE)));

        followTaskService.getFollowTaskPage(new FollowTaskQuery());

        verify(followTaskMapper).markOverdue(any(), eq(3));
        ArgumentCaptor<FollowTaskQuery> queryCaptor = ArgumentCaptor.forClass(FollowTaskQuery.class);
        verify(followTaskMapper).selectByQuery(queryCaptor.capture());
        assertEquals(3, queryCaptor.getValue().getDataScopeUserId());
    }

    @Test
    void listFollowTasksReadOnly_shouldNotMarkOverdueBeforeQuery() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(3);
        when(followTaskMapper.selectByQuery(any())).thenReturn(List.of(task(1L, FollowTaskStatus.PENDING)));

        followTaskService.getFollowTaskPageReadOnly(new FollowTaskQuery());

        verify(followTaskMapper, never()).markOverdue(any(), any());
        ArgumentCaptor<FollowTaskQuery> queryCaptor = ArgumentCaptor.forClass(FollowTaskQuery.class);
        verify(followTaskMapper).selectByQuery(queryCaptor.capture());
        assertEquals(3, queryCaptor.getValue().getDataScopeUserId());
    }

    @Test
    void completeFollowTask_shouldInsertCommunicationUpdateTaskAndRecentFact() {
        TFollowTask current = task(100L, FollowTaskStatus.IN_PROGRESS);
        when(followTaskMapper.selectByIdForUpdate(100L)).thenReturn(current);
        when(communicationRecordMapper.insert(any())).thenAnswer(invocation -> {
            TCommunicationRecord record = invocation.getArgument(0);
            record.setId(900L);
            return 1;
        });
        when(followTaskMapper.completeIfCurrent(eq(100L), eq("IN_PROGRESS"), eq("已完成回访"), eq(900L),
                any(), eq(7), any(), eq(7))).thenReturn(1);
        TFollowTask completed = task(100L, FollowTaskStatus.COMPLETED);
        when(followTaskMapper.selectById(100L)).thenReturn(completed);

        TFollowTask result = followTaskService.completeFollowTask(100L, completeRequest(false));

        assertSame(completed, result);
        ArgumentCaptor<TCommunicationRecord> recordCaptor = ArgumentCaptor.forClass(TCommunicationRecord.class);
        verify(communicationRecordMapper).insert(recordCaptor.capture());
        assertEquals(100L, recordCaptor.getValue().getFollowTaskId());
        assertEquals("PHONE", recordCaptor.getValue().getCommunicationMethod());
        assertEquals("已电话确认购车计划", recordCaptor.getValue().getSummary());
        verify(relatedObjectResolver).updateRecentFollowFact(eq(FollowRelatedObjectType.CUSTOMER), eq(10L),
                eq(recordCaptor.getValue().getCommunicationTime()), eq("已电话确认购车计划"),
                eq(DUE_TIME.plusDays(1)), eq(7));
        verify(auditRecorder).record(AuditActionEnum.FOLLOW_TASK_COMPLETE, "100");
    }

    @Test
    void completeFollowTask_casFailure_shouldRollbackBeforeRecentFact() {
        TFollowTask current = task(100L, FollowTaskStatus.IN_PROGRESS);
        when(followTaskMapper.selectByIdForUpdate(100L)).thenReturn(current);
        when(communicationRecordMapper.insert(any())).thenAnswer(invocation -> {
            TCommunicationRecord record = invocation.getArgument(0);
            record.setId(900L);
            return 1;
        });
        when(followTaskMapper.completeIfCurrent(eq(100L), eq("IN_PROGRESS"), eq("已完成回访"), eq(900L),
                any(), eq(7), any(), eq(7))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> followTaskService.completeFollowTask(100L, completeRequest(false)));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(relatedObjectResolver, never()).updateRecentFollowFact(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void postponeFollowTask_terminalTask_shouldReject() {
        when(followTaskMapper.selectByIdForUpdate(100L)).thenReturn(task(100L, FollowTaskStatus.COMPLETED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> followTaskService.postponeFollowTask(100L, postponeRequest()));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(followTaskMapper, never()).postponeIfCurrent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test void handoverOwnerCannotReceiveNewFollowTask(){
        when(relatedObjectResolver.requireAccessible("CUSTOMER",10L)).thenReturn(new FollowRelatedObjectContext(FollowRelatedObjectType.CUSTOMER,10L,3,"王先生"));
        org.mockito.Mockito.doThrow(new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT)).when(responsibilityGuard).requireActiveOwner(3);
        BusinessException error=assertThrows(BusinessException.class,()->followTaskService.createFollowTask(createRequest()));
        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,error.getCodeEnum());verify(followTaskMapper,never()).insert(any());verify(auditRecorder,never()).record(any(),anyString());
    }

    private CreateFollowTaskRequest createRequest() {
        CreateFollowTaskRequest request = new CreateFollowTaskRequest();
        request.setTitle("电话回访");
        request.setTaskType("PHONE_FOLLOW_UP");
        request.setRelatedObjectType("CUSTOMER");
        request.setRelatedObjectId(10L);
        request.setOwnerId(3);
        request.setDueTime(DUE_TIME);
        request.setPriority("HIGH");
        return request;
    }

    private CompleteFollowTaskRequest completeRequest(boolean createNext) {
        CompleteFollowTaskRequest request = new CompleteFollowTaskRequest();
        request.setCommunicationMethod("PHONE");
        request.setSummary("已电话确认购车计划");
        request.setCustomerFeedback("客户下周到店");
        request.setResult("已完成回访");
        request.setNextAction("预约到店");
        request.setNextFollowTime(DUE_TIME.plusDays(1));
        request.setCreateNextTask(createNext);
        return request;
    }

    private PostponeFollowTaskRequest postponeRequest() {
        PostponeFollowTaskRequest request = new PostponeFollowTaskRequest();
        request.setReason("客户临时改期");
        request.setNewDueTime(LocalDateTime.now().plusDays(1));
        return request;
    }

    private TFollowTask task(Long id, FollowTaskStatus status) {
        TFollowTask task = new TFollowTask();
        task.setId(id);
        task.setTitle("电话回访");
        task.setTaskType("PHONE_FOLLOW_UP");
        task.setRelatedObjectType("CUSTOMER");
        task.setRelatedObjectId(10L);
        task.setOwnerId(3);
        task.setDueTime(DUE_TIME);
        task.setStatus(status.name());
        task.setPostponeCount(0);
        return task;
    }
}
