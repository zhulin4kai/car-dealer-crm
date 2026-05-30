package com.bjpowernode.service;

import com.bjpowernode.mapper.TActivityRemarkMapper;
import com.bjpowernode.model.TActivityRemark;
import com.bjpowernode.model.TUser;
import com.bjpowernode.query.ActivityRemarkQuery;
import com.bjpowernode.service.impl.ActivityRemarkServiceImpl;
import com.bjpowernode.util.JWTUtils;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    @Test
    void testSaveActivityRemark() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(1);
        query.setNoteContent("Test remark content");
        query.setToken("valid-token");

        TUser mockUser = new TUser();
        mockUser.setId(100);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(mockUser);
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
    }

    @Test
    void testSaveActivityRemarkWithDifferentUser() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(2);
        query.setNoteContent("Another remark");
        query.setToken("valid-token-2");

        TUser mockUser = new TUser();
        mockUser.setId(200);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token-2")).thenReturn(mockUser);
            when(tActivityRemarkMapper.insertSelective(any(TActivityRemark.class))).thenReturn(1);

            int result = activityRemarkService.saveActivityRemark(query);

            assertEquals(1, result);
            verify(tActivityRemarkMapper).insertSelective(argThat(remark ->
                    remark.getCreateBy().equals(200)
            ));
        }
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
        when(tActivityRemarkMapper.selectByPrimaryKey(1)).thenReturn(remark);

        TActivityRemark result = activityRemarkService.getActivityRemarkById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test remark", result.getNoteContent());
    }

    @Test
    void testGetActivityRemarkByIdNotFound() {
        when(tActivityRemarkMapper.selectByPrimaryKey(999)).thenReturn(null);

        TActivityRemark result = activityRemarkService.getActivityRemarkById(999);

        assertNull(result);
    }

    @Test
    void testUpdateActivityRemark() {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setId(1);
        query.setNoteContent("Updated remark content");
        query.setToken("valid-token");

        TUser mockUser = new TUser();
        mockUser.setId(100);

        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid-token")).thenReturn(mockUser);
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
    }

    @Test
    void testDelActivityRemarkById() {
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
}
