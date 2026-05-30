package com.bjpowernode.web;

import com.bjpowernode.model.TActivity;
import com.bjpowernode.query.ActivityQuery;
import com.bjpowernode.service.ActivityService;
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
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @Test
    void activityPage_returnsPageInfo() throws Exception {
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setName("Test Activity");
        PageInfo<TActivity> pageInfo = new PageInfo<>(Collections.singletonList(activity));

        when(activityService.getActivityByPage(eq(1), any(ActivityQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activitys").param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void activityPage_defaultCurrent_whenNull() throws Exception {
        PageInfo<TActivity> pageInfo = new PageInfo<>(Collections.emptyList());
        when(activityService.getActivityByPage(eq(1), any(ActivityQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activitys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addActivity_success() throws Exception {
        when(activityService.saveActivity(any(ActivityQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/activity")
                        .header("Authorization", "test-token")
                        .param("name", "New Activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addActivity_failure() throws Exception {
        when(activityService.saveActivity(any(ActivityQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/activity")
                        .header("Authorization", "test-token")
                        .param("name", "Fail Activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void loadActivity_returnsActivity() throws Exception {
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setName("Loaded Activity");

        when(activityService.getActivityById(1)).thenReturn(activity);

        mockMvc.perform(get("/api/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Loaded Activity"));
    }

    @Test
    void editActivity_success() throws Exception {
        when(activityService.updateActivity(any(ActivityQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/activity")
                        .header("Authorization", "test-token")
                        .param("id", "1")
                        .param("name", "Updated Activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void editActivity_failure() throws Exception {
        when(activityService.updateActivity(any(ActivityQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/activity")
                        .header("Authorization", "test-token")
                        .param("id", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void batchDeleteActivities_success() throws Exception {
        when(activityService.batchDeleteActivities(any())).thenReturn(2);

        mockMvc.perform(post("/api/activity/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void batchDeleteActivities_failure() throws Exception {
        when(activityService.batchDeleteActivities(any())).thenReturn(0);

        mockMvc.perform(post("/api/activity/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[999]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteActivity_success() throws Exception {
        when(activityService.deleteActivity(1)).thenReturn(1);

        mockMvc.perform(delete("/api/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteActivity_failure() throws Exception {
        when(activityService.deleteActivity(999)).thenReturn(0);

        mockMvc.perform(delete("/api/activity/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
