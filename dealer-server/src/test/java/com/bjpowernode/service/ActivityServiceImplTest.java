package com.bjpowernode.service;

import com.bjpowernode.mapper.TActivityMapper;
import com.bjpowernode.model.TActivity;
import com.bjpowernode.model.TUser;
import com.bjpowernode.query.ActivityQuery;
import com.bjpowernode.service.impl.ActivityServiceImpl;
import com.bjpowernode.util.JWTUtils;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
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

    @Test
    void testGetActivityByPage() {
        ActivityQuery query = new ActivityQuery();
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setName("Test Activity");
        List<TActivity> list = Collections.singletonList(activity);

        when(tActivityMapper.selectActivityByPage(query)).thenReturn(list);

        PageInfo<TActivity> result = activityService.getActivityByPage(1, query);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("Test Activity", result.getList().get(0).getName());
        verify(tActivityMapper).selectActivityByPage(query);
    }

    @Test
    void testGetActivityByPageEmpty() {
        ActivityQuery query = new ActivityQuery();
        when(tActivityMapper.selectActivityByPage(query)).thenReturn(Collections.emptyList());

        PageInfo<TActivity> result = activityService.getActivityByPage(1, query);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        verify(tActivityMapper).selectActivityByPage(query);
    }

    @Test
    void testSaveActivity() {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            ActivityQuery query = new ActivityQuery();
            query.setName("New Activity");
            query.setCost(BigDecimal.valueOf(1000));
            query.setToken("test-token");

            TUser loginUser = new TUser();
            loginUser.setId(10);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("test-token")).thenReturn(loginUser);

            when(tActivityMapper.insertSelective(any(TActivity.class))).thenReturn(1);

            int result = activityService.saveActivity(query);

            assertEquals(1, result);
            verify(tActivityMapper).insertSelective(argThat(activity ->
                    "New Activity".equals(activity.getName())
                            && activity.getCreateBy() != null
                            && activity.getCreateTime() != null
            ));
        }
    }

    @Test
    void testGetActivityById() {
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setName("Test Activity");

        when(tActivityMapper.selectDetailByPrimaryKey(1)).thenReturn(activity);

        TActivity result = activityService.getActivityById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test Activity", result.getName());
        verify(tActivityMapper).selectDetailByPrimaryKey(1);
    }

    @Test
    void testGetActivityByIdNotFound() {
        when(tActivityMapper.selectDetailByPrimaryKey(999)).thenReturn(null);

        TActivity result = activityService.getActivityById(999);

        assertNull(result);
        verify(tActivityMapper).selectDetailByPrimaryKey(999);
    }

    @Test
    void testUpdateActivity() {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            ActivityQuery query = new ActivityQuery();
            query.setId(1);
            query.setName("Updated Activity");
            query.setToken("test-token");

            TUser loginUser = new TUser();
            loginUser.setId(10);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("test-token")).thenReturn(loginUser);

            when(tActivityMapper.updateByPrimaryKeySelective(any(TActivity.class))).thenReturn(1);

            int result = activityService.updateActivity(query);

            assertEquals(1, result);
            verify(tActivityMapper).updateByPrimaryKeySelective(argThat(activity ->
                    "Updated Activity".equals(activity.getName())
                            && activity.getEditBy() != null
                            && activity.getEditTime() != null
            ));
        }
    }

    @Test
    void testGetOngoingActivity() {
        TActivity activity1 = new TActivity();
        activity1.setId(1);
        activity1.setName("Ongoing 1");
        TActivity activity2 = new TActivity();
        activity2.setId(2);
        activity2.setName("Ongoing 2");

        when(tActivityMapper.selecOngoingActivity()).thenReturn(Arrays.asList(activity1, activity2));

        List<TActivity> result = activityService.getOngoingActivity();

        assertEquals(2, result.size());
        verify(tActivityMapper).selecOngoingActivity();
    }

    @Test
    void testGetOngoingActivityEmpty() {
        when(tActivityMapper.selecOngoingActivity()).thenReturn(Collections.emptyList());

        List<TActivity> result = activityService.getOngoingActivity();

        assertTrue(result.isEmpty());
        verify(tActivityMapper).selecOngoingActivity();
    }

    @Test
    void testBatchDeleteActivities() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(tActivityMapper.batchDeleteByIds(ids)).thenReturn(3);

        int result = activityService.batchDeleteActivities(ids);

        assertEquals(3, result);
        verify(tActivityMapper).batchDeleteByIds(ids);
    }

    @Test
    void testBatchDeleteActivitiesEmptyList() {
        int result = activityService.batchDeleteActivities(Collections.emptyList());

        assertEquals(0, result);
        verify(tActivityMapper, never()).batchDeleteByIds(anyList());
    }

    @Test
    void testBatchDeleteActivitiesNullList() {
        int result = activityService.batchDeleteActivities(null);

        assertEquals(0, result);
        verify(tActivityMapper, never()).batchDeleteByIds(anyList());
    }

    @Test
    void testDeleteActivity() {
        when(tActivityMapper.deleteByPrimaryKey(1)).thenReturn(1);

        int result = activityService.deleteActivity(1);

        assertEquals(1, result);
        verify(tActivityMapper).deleteByPrimaryKey(1);
    }

    @Test
    void testDeleteActivityNullId() {
        int result = activityService.deleteActivity(null);

        assertEquals(0, result);
        verify(tActivityMapper, never()).deleteByPrimaryKey(any());
    }
}
