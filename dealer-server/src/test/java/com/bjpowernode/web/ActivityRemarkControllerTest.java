package com.bjpowernode.web;

import com.bjpowernode.model.TActivityRemark;
import com.bjpowernode.query.ActivityRemarkQuery;
import com.bjpowernode.service.ActivityRemarkService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ActivityRemarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityRemarkService activityRemarkService;

    @Test
    void addActivityRemark_success() throws Exception {
        when(activityRemarkService.saveActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/activity/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"noteContent\":\"Test remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addActivityRemark_failure() throws Exception {
        when(activityRemarkService.saveActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/activity/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"noteContent\":\"Fail remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void activityRemarkPage_returnsPageInfo() throws Exception {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(1);
        remark.setNoteContent("Remark content");
        PageInfo<TActivityRemark> pageInfo = new PageInfo<>(Collections.singletonList(remark));

        when(activityRemarkService.getActivityRemarkByPage(eq(1), any(ActivityRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activity/remark")
                        .param("current", "1")
                        .param("activityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void activityRemarkPage_defaultCurrent_whenNull() throws Exception {
        PageInfo<TActivityRemark> pageInfo = new PageInfo<>(Collections.emptyList());
        when(activityRemarkService.getActivityRemarkByPage(eq(1), any(ActivityRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activity/remark")
                        .param("activityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getActivityRemarkById_returnsRemark() throws Exception {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(1);
        remark.setNoteContent("Test Remark");

        when(activityRemarkService.getActivityRemarkById(1)).thenReturn(remark);

        mockMvc.perform(get("/api/activity/remark/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.noteContent").value("Test Remark"));
    }

    @Test
    void editActivityRemark_success() throws Exception {
        when(activityRemarkService.updateActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/activity/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"noteContent\":\"Updated remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void editActivityRemark_failure() throws Exception {
        when(activityRemarkService.updateActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/activity/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"noteContent\":\"Fail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void delActivityRemark_success() throws Exception {
        when(activityRemarkService.delActivityRemarkById(1)).thenReturn(1);

        mockMvc.perform(delete("/api/activity/remark/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delActivityRemark_failure() throws Exception {
        when(activityRemarkService.delActivityRemarkById(999)).thenReturn(0);

        mockMvc.perform(delete("/api/activity/remark/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
