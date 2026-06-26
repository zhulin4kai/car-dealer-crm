package com.autodealer.crm.service;

import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.ActivityLifecycleRequest;
import com.autodealer.crm.dto.CreateActivityRequest;
import com.autodealer.crm.dto.ReviewActivityRequest;
import com.autodealer.crm.dto.UpdateActivityRequest;
import com.autodealer.crm.enums.ActivityStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.result.ActivityExportRow;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.ActivityServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Mock
    private TActivityMapper tActivityMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OperationAuditRecorder auditRecorder;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(10);
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(10);
    }

    @Test
    void getActivityByPageRejectsOversizedPageSize() {
        ActivityQuery query = new ActivityQuery();
        query.setPageSize(101);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.getActivityByPage(1, query));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(tActivityMapper, never()).selectActivityByPage(any());
    }

    @Test
    void saveActivityCreatesDraftOwnedByCurrentUser() {
        CreateActivityRequest request = createRequest();
        when(tActivityMapper.insertSelective(any(TActivity.class))).thenAnswer(invocation -> {
            TActivity activity = invocation.getArgument(0);
            activity.setId(99);
            return 1;
        });

        int result = activityService.saveActivity(request);

        assertEquals(1, result);
        verify(tActivityMapper).insertSelective(argThat(activity ->
                activity.getOwnerId().equals(10)
                        && ActivityStatus.DRAFT.name().equals(activity.getStatus())
                        && "店内活动".equals(activity.getChannel())
                        && activity.getCreateBy().equals(10)
        ));
    }

    @Test
    void saveActivityRejectsInvalidTimeRangeBeforeInsert() {
        CreateActivityRequest request = createRequest();
        request.setEndTime(request.getStartTime());

        assertThrows(BusinessException.class, () -> activityService.saveActivity(request));

        verify(tActivityMapper, never()).insertSelective(any());
    }

    @Test
    void updateActivityRejectsReviewedCoreFacts() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.REVIEWED));

        assertThrows(BusinessException.class, () -> activityService.updateActivity(updateRequest()));

        verify(tActivityMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void updateActivityKeepsExistingOwnerAndWritesAudit() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.PLANNED));
        when(tActivityMapper.updateByPrimaryKeySelective(any(TActivity.class))).thenReturn(1);

        int result = activityService.updateActivity(updateRequest());

        assertEquals(1, result);
        verify(tActivityMapper).updateByPrimaryKeySelective(argThat(activity ->
                activity.getOwnerId().equals(10)
                        && "更新活动".equals(activity.getName())
                        && activity.getEditBy().equals(10)
        ));
    }

    @Test
    void publishActivityUsesOldStatusCas() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.DRAFT), activity(1, ActivityStatus.PLANNED));
        when(tActivityMapper.updateStatusAtomic(1, "DRAFT", "PLANNED", 10, null, 10)).thenReturn(1);

        TActivity result = activityService.publishActivity(1);

        assertEquals(ActivityStatus.PLANNED.name(), result.getStatus());
        verify(tActivityMapper).updateStatusAtomic(1, "DRAFT", "PLANNED", 10, null, 10);
    }

    @Test
    void startActivityRejectsInvalidTransition() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.ENDED));

        assertThrows(BusinessException.class, () -> activityService.startActivity(1));

        verify(tActivityMapper, never()).updateStatusAtomic(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void reviewActivityRequiresEndedAndWritesSnapshotFields() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.ENDED), activity(1, ActivityStatus.REVIEWED));
        when(tActivityMapper.reviewAtomic(eq(1), eq("ENDED"), any(TActivity.class), eq(10))).thenReturn(1);

        TActivity result = activityService.reviewActivity(1, reviewRequest());

        assertEquals(ActivityStatus.REVIEWED.name(), result.getStatus());
        verify(tActivityMapper).reviewAtomic(eq(1), eq("ENDED"), argThat(record ->
                BigDecimal.valueOf(12000).equals(record.getActualCost())
                        && "复盘结果".equals(record.getResultSummary())
                        && record.getReviewedBy().equals(10)
        ), eq(10));
    }

    @Test
    void deleteActivityRejectsReferencedActivity() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.DRAFT));
        when(tActivityMapper.countBusinessReferences(1)).thenReturn(1);

        assertThrows(BusinessException.class, () -> activityService.deleteActivity(1));

        verify(tActivityMapper, never()).deleteByPrimaryKey(any());
    }

    @Test
    void deleteActivityRejectsNonDraftEvenWithoutReferences() {
        when(tActivityMapper.selectDetailByPrimaryKey(1, 10))
                .thenReturn(activity(1, ActivityStatus.PLANNED));

        assertThrows(BusinessException.class, () -> activityService.deleteActivity(1));

        verify(tActivityMapper, never()).countBusinessReferences(any());
        verify(tActivityMapper, never()).deleteByPrimaryKey(any());
    }

    @Test
    void exportActivityRoiWritesAuditAndReturnsRows() {
        ActivityQuery query = new ActivityQuery();
        when(tActivityMapper.selectActivityExportRows(query))
                .thenReturn(Collections.singletonList(new ActivityExportRow()));

        List<ActivityExportRow> result = activityService.exportActivityRoi(query);

        assertEquals(1, result.size());
        verify(auditRecorder).recordQuietly(any(), eq("export"), eq("SUCCESS"), contains("\"count\":1"));
    }

    private CreateActivityRequest createRequest() {
        CreateActivityRequest request = new CreateActivityRequest();
        request.setName("新活动");
        request.setChannel("店内活动");
        request.setTargetModel("SUV");
        request.setStartTime(new Date(1_000));
        request.setEndTime(new Date(2_000));
        request.setCost(BigDecimal.valueOf(10000));
        request.setDescription("活动描述");
        return request;
    }

    private UpdateActivityRequest updateRequest() {
        UpdateActivityRequest request = new UpdateActivityRequest();
        request.setId(1);
        request.setName("更新活动");
        request.setChannel("店内活动");
        request.setTargetModel("SUV");
        request.setStartTime(new Date(1_000));
        request.setEndTime(new Date(2_000));
        request.setCost(BigDecimal.valueOf(10000));
        request.setDescription("更新描述");
        return request;
    }

    private ReviewActivityRequest reviewRequest() {
        ReviewActivityRequest request = new ReviewActivityRequest();
        request.setActualCost(BigDecimal.valueOf(12000));
        request.setResultSummary("复盘结果");
        request.setReviewConclusion("复盘结论");
        return request;
    }

    private ActivityLifecycleRequest reasonRequest() {
        ActivityLifecycleRequest request = new ActivityLifecycleRequest();
        request.setReason("业务关闭");
        return request;
    }

    private TActivity activity(Integer id, ActivityStatus status) {
        TActivity activity = new TActivity();
        activity.setId(id);
        activity.setOwnerId(10);
        activity.setName("测试活动");
        activity.setStatus(status.name());
        activity.setStartTime(new Date(1_000));
        activity.setEndTime(new Date(2_000));
        activity.setCost(BigDecimal.valueOf(10000));
        return activity;
    }
}
