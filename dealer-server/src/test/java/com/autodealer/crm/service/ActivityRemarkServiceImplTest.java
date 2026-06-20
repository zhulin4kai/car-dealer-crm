package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.mapper.TActivityRemarkMapper;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.model.TActivityRemark;
import com.autodealer.crm.query.ActivityRemarkQuery;
import com.autodealer.crm.service.impl.ActivityRemarkServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityRemarkServiceImplTest {

    @InjectMocks
    private ActivityRemarkServiceImpl activityRemarkService;

    @Mock
    private TActivityRemarkMapper tActivityRemarkMapper;

    @Mock
    private TActivityMapper tActivityMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(100);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(100);
    }

    @Test
    void testSaveActivityRemark() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(1);
        query.setNoteContent("Test remark content");

            when(tActivityMapper.selectDetailByPrimaryKey(1, 100)).thenReturn(activity(1));
            when(tActivityRemarkMapper.insertSelective(any(TActivityRemark.class))).thenReturn(1);

            int result = activityRemarkService.saveActivityRemark(query);

            assertEquals(1, result);
            verify(tActivityRemarkMapper).insertSelective(argThat(remark ->
                    remark.getActivityId().equals(1) &&
                            remark.getNoteContent().equals("Test remark content") &&
                            remark.getCreateBy().equals(100) &&
                            remark.getCreateTime() != null
            ));
    }

    @Test
    void testSaveActivityRemarkWithDifferentUser() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(2);
        query.setNoteContent("Another remark");

            when(currentUserProvider.getCurrentUserId()).thenReturn(200);
            when(currentUserProvider.getDataScopeUserId()).thenReturn(200);
            when(tActivityMapper.selectDetailByPrimaryKey(2, 200)).thenReturn(activity(2));
            when(tActivityRemarkMapper.insertSelective(any(TActivityRemark.class))).thenReturn(1);

            int result = activityRemarkService.saveActivityRemark(query);

            assertEquals(1, result);
            verify(tActivityRemarkMapper).insertSelective(argThat(remark ->
                    remark.getCreateBy().equals(200)
            ));
    }

    @Test
    void testGetActivityRemarkByPage() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(1);

        List<TActivityRemark> remarks = Arrays.asList(
                createActivityRemark(1, 1, "Remark 1"),
                createActivityRemark(2, 1, "Remark 2")
        );
        when(tActivityRemarkMapper.selectActivityRemarkByPage(any(ActivityRemarkQuery.class))).thenReturn(remarks);

        PageInfo<TActivityRemark> result = activityRemarkService.getActivityRemarkByPage(1, query);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(tActivityRemarkMapper).selectActivityRemarkByPage(query);
    }

    @Test
    void testGetActivityRemarkByPageEmpty() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(999);

        when(tActivityRemarkMapper.selectActivityRemarkByPage(any(ActivityRemarkQuery.class))).thenReturn(Collections.emptyList());

        PageInfo<TActivityRemark> result = activityRemarkService.getActivityRemarkByPage(1, query);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetActivityRemarkById() {
        TActivityRemark remark = createActivityRemark(1, 1, "Test remark");
        when(tActivityRemarkMapper.selectScopedByPrimaryKey(1, 100)).thenReturn(remark);

        TActivityRemark result = activityRemarkService.getActivityRemarkById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test remark", result.getNoteContent());
    }

    @Test
    void testGetActivityRemarkByIdNotFound() {
        assertThrows(RuntimeException.class,
                () -> activityRemarkService.getActivityRemarkById(999));
    }

    @Test
    void testUpdateActivityRemark() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setId(1);
            query.setNoteContent("Updated remark content");

            when(tActivityRemarkMapper.selectScopedByPrimaryKey(1, 100))
                    .thenReturn(createActivityRemark(1, 1, "Old remark"));
            when(tActivityRemarkMapper.updateByPrimaryKeySelective(any(TActivityRemark.class))).thenReturn(1);

            int result = activityRemarkService.updateActivityRemark(query);

            assertEquals(1, result);
            verify(tActivityRemarkMapper).updateByPrimaryKeySelective(argThat(remark ->
                    remark.getId().equals(1) &&
                            remark.getNoteContent().equals("Updated remark content") &&
                            remark.getEditBy().equals(100) &&
                            remark.getEditTime() != null
            ));
    }

    @Test
    void testDelActivityRemarkById() {
        when(tActivityRemarkMapper.selectScopedByPrimaryKey(1, 100))
                .thenReturn(createActivityRemark(1, 1, "Remark"));
        when(tActivityRemarkMapper.updateByPrimaryKeySelective(any(TActivityRemark.class))).thenReturn(1);

        int result = activityRemarkService.delActivityRemarkById(1);

        assertEquals(1, result);
        verify(tActivityRemarkMapper).updateByPrimaryKeySelective(argThat(remark ->
                remark.getId().equals(1) &&
                        remark.getDeleted().equals(1)
        ));
    }

    @Test
    void testDelActivityRemarkByIdLogicalDelete() {
        when(tActivityRemarkMapper.selectScopedByPrimaryKey(5, 100))
                .thenReturn(createActivityRemark(5, 1, "Remark"));
        when(tActivityRemarkMapper.updateByPrimaryKeySelective(any(TActivityRemark.class))).thenReturn(1);

        activityRemarkService.delActivityRemarkById(5);

        verify(tActivityRemarkMapper).updateByPrimaryKeySelective(argThat(remark ->
                remark.getId().equals(5) &&
                        remark.getDeleted().equals(1) &&
                        remark.getNoteContent() == null
        ));
    }

    private TActivityRemark createActivityRemark(Integer id, Integer activityId, String content) {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(id);
        remark.setActivityId(activityId);
        remark.setNoteContent(content);
        remark.setCreateBy(100);
        return remark;
    }

    private TActivity activity(Integer id) {
        TActivity activity = new TActivity();
        activity.setId(id);
        activity.setOwnerId(id == 2 ? 200 : 100);
        return activity;
    }
}
