package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CorrectCommunicationRecordRequest;
import com.autodealer.crm.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.dto.VoidCommunicationRecordRequest;
import com.autodealer.crm.enums.CommunicationRecordStatus;
import com.autodealer.crm.enums.FollowRelatedObjectType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TCommunicationRecordMapper;
import com.autodealer.crm.mapper.TFollowTaskMapper;
import com.autodealer.crm.model.TCommunicationRecord;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.CommunicationRecordServiceImpl;
import com.autodealer.crm.service.impl.FollowRelatedObjectContext;
import com.autodealer.crm.service.impl.FollowRelatedObjectResolver;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationRecordServiceImplTest {

    private static final LocalDateTime COMM_TIME = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Mock private TCommunicationRecordMapper communicationRecordMapper;
    @Mock private TFollowTaskMapper followTaskMapper;
    @Mock private FollowRelatedObjectResolver relatedObjectResolver;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @InjectMocks private CommunicationRecordServiceImpl communicationRecordService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
    }

    @Test
    void createCommunicationRecord_withTask_shouldRequireSameObjectAndUpdateRecentFact() {
        FollowRelatedObjectContext context = context();
        when(relatedObjectResolver.requireAccessible("CUSTOMER", 10L)).thenReturn(context);
        when(followTaskMapper.selectById(100L)).thenReturn(task());
        when(communicationRecordMapper.insert(any())).thenAnswer(invocation -> {
            TCommunicationRecord record = invocation.getArgument(0);
            record.setId(900L);
            return 1;
        });
        TCommunicationRecord persisted = record(900L, CommunicationRecordStatus.ACTIVE);
        when(communicationRecordMapper.selectById(900L)).thenReturn(persisted);

        TCommunicationRecord result = communicationRecordService.createCommunicationRecord(createRequest());

        assertSame(persisted, result);
        ArgumentCaptor<TCommunicationRecord> recordCaptor = ArgumentCaptor.forClass(TCommunicationRecord.class);
        verify(communicationRecordMapper).insert(recordCaptor.capture());
        assertEquals(100L, recordCaptor.getValue().getFollowTaskId());
        assertEquals("WECHAT", recordCaptor.getValue().getCommunicationMethod());
        assertEquals(3, recordCaptor.getValue().getOwnerId());
        verify(relatedObjectResolver).updateRecentFollowFact(FollowRelatedObjectType.CUSTOMER, 10L,
                COMM_TIME, "微信确认到店时间", COMM_TIME.plusDays(1), 7);
        verify(auditRecorder).record(AuditActionEnum.COMMUNICATION_RECORD_CREATE, "900");
    }

    @Test
    void createCommunicationRecord_taskObjectMismatch_shouldRejectBeforeInsert() {
        when(relatedObjectResolver.requireAccessible("CUSTOMER", 10L)).thenReturn(context());
        TFollowTask task = task();
        task.setRelatedObjectId(11L);
        when(followTaskMapper.selectById(100L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communicationRecordService.createCommunicationRecord(createRequest()));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(communicationRecordMapper, never()).insert(any());
    }

    @Test
    void correctCommunicationRecord_shouldMarkOldAndInsertReplacement() {
        TCommunicationRecord current = record(900L, CommunicationRecordStatus.ACTIVE);
        when(communicationRecordMapper.selectByIdForUpdate(900L)).thenReturn(current);
        when(communicationRecordMapper.markCorrected(eq(900L), eq("ACTIVE"), eq("摘要修正"), any(), eq(7)))
                .thenReturn(1);
        when(relatedObjectResolver.requireAccessible("CUSTOMER", 10L)).thenReturn(context());
        when(communicationRecordMapper.insert(any())).thenAnswer(invocation -> {
            TCommunicationRecord record = invocation.getArgument(0);
            record.setId(901L);
            return 1;
        });
        TCommunicationRecord corrected = record(901L, CommunicationRecordStatus.ACTIVE);
        when(communicationRecordMapper.selectById(901L)).thenReturn(corrected);

        TCommunicationRecord result = communicationRecordService.correctCommunicationRecord(900L, correctRequest());

        assertSame(corrected, result);
        ArgumentCaptor<TCommunicationRecord> recordCaptor = ArgumentCaptor.forClass(TCommunicationRecord.class);
        verify(communicationRecordMapper).insert(recordCaptor.capture());
        assertEquals(900L, recordCaptor.getValue().getParentRecordId());
        assertEquals("电话重新确认", recordCaptor.getValue().getSummary());
        verify(auditRecorder).record(AuditActionEnum.COMMUNICATION_RECORD_CORRECT, "900");
    }

    @Test
    void voidCommunicationRecord_shouldOnlyMarkVoided() {
        TCommunicationRecord current = record(900L, CommunicationRecordStatus.ACTIVE);
        when(communicationRecordMapper.selectByIdForUpdate(900L)).thenReturn(current);
        when(communicationRecordMapper.voidIfActive(eq(900L), eq("ACTIVE"), eq("误登记"), any(), eq(7)))
                .thenReturn(1);
        TCommunicationRecord voided = record(900L, CommunicationRecordStatus.VOIDED);
        when(communicationRecordMapper.selectById(900L)).thenReturn(voided);

        TCommunicationRecord result = communicationRecordService.voidCommunicationRecord(900L, voidRequest());

        assertSame(voided, result);
        verify(communicationRecordMapper, never()).insert(any());
        verify(auditRecorder).record(AuditActionEnum.COMMUNICATION_RECORD_VOID, "900");
    }

    private CreateCommunicationRecordRequest createRequest() {
        CreateCommunicationRecordRequest request = new CreateCommunicationRecordRequest();
        request.setFollowTaskId(100L);
        request.setRelatedObjectType("CUSTOMER");
        request.setRelatedObjectId(10L);
        request.setCommunicationMethod("WECHAT");
        request.setCommunicationTime(COMM_TIME);
        request.setSummary("微信确认到店时间");
        request.setCustomerFeedback("客户确认");
        request.setNextAction("到店接待");
        request.setNextFollowTime(COMM_TIME.plusDays(1));
        return request;
    }

    private CorrectCommunicationRecordRequest correctRequest() {
        CorrectCommunicationRecordRequest request = new CorrectCommunicationRecordRequest();
        request.setCommunicationMethod("PHONE");
        request.setCommunicationTime(COMM_TIME.plusHours(1));
        request.setSummary("电话重新确认");
        request.setCorrectionReason("摘要修正");
        return request;
    }

    private VoidCommunicationRecordRequest voidRequest() {
        VoidCommunicationRecordRequest request = new VoidCommunicationRecordRequest();
        request.setReason("误登记");
        return request;
    }

    private FollowRelatedObjectContext context() {
        return new FollowRelatedObjectContext(FollowRelatedObjectType.CUSTOMER, 10L, 3, "王先生");
    }

    private TFollowTask task() {
        TFollowTask task = new TFollowTask();
        task.setId(100L);
        task.setRelatedObjectType("CUSTOMER");
        task.setRelatedObjectId(10L);
        task.setOwnerId(3);
        return task;
    }

    private TCommunicationRecord record(Long id, CommunicationRecordStatus status) {
        TCommunicationRecord record = new TCommunicationRecord();
        record.setId(id);
        record.setFollowTaskId(100L);
        record.setRelatedObjectType("CUSTOMER");
        record.setRelatedObjectId(10L);
        record.setOwnerId(3);
        record.setCommunicationMethod("WECHAT");
        record.setCommunicationTime(COMM_TIME);
        record.setSummary("微信确认到店时间");
        record.setNextFollowTime(COMM_TIME.plusDays(1));
        record.setStatus(status.name());
        return record;
    }
}
