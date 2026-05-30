package com.bjpowernode.web;

import com.bjpowernode.model.TClueRemark;
import com.bjpowernode.query.ClueRemarkQuery;
import com.bjpowernode.service.ClueRemarkService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ClueRemarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClueRemarkService clueRemarkService;

    @Test
    void addClueRemark_success() throws Exception {
        when(clueRemarkService.saveClueRemark(any(ClueRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/clue/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1,\"noteContent\":\"Test remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addClueRemark_failure() throws Exception {
        when(clueRemarkService.saveClueRemark(any(ClueRemarkQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/clue/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1,\"noteContent\":\"Fail remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void clueRemarkPage_returnsPageInfo() throws Exception {
        TClueRemark remark = new TClueRemark();
        remark.setId(1);
        remark.setNoteContent("Clue remark content");
        PageInfo<TClueRemark> pageInfo = new PageInfo<>(Collections.singletonList(remark));

        when(clueRemarkService.getClueRemarkByPage(eq(1), any(ClueRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clue/remark")
                        .param("current", "1")
                        .param("clueId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void clueRemarkPage_defaultCurrent_whenNull() throws Exception {
        PageInfo<TClueRemark> pageInfo = new PageInfo<>(Collections.emptyList());
        when(clueRemarkService.getClueRemarkByPage(eq(1), any(ClueRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clue/remark")
                        .param("clueId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void clueRemarkPage_returnsMultipleRemarks() throws Exception {
        TClueRemark remark1 = new TClueRemark();
        remark1.setId(1);
        remark1.setNoteContent("First remark");
        TClueRemark remark2 = new TClueRemark();
        remark2.setId(2);
        remark2.setNoteContent("Second remark");
        PageInfo<TClueRemark> pageInfo = new PageInfo<>(Arrays.asList(remark1, remark2));

        when(clueRemarkService.getClueRemarkByPage(eq(1), any(ClueRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clue/remark")
                        .param("current", "1")
                        .param("clueId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addClueRemark_withNoteWay_success() throws Exception {
        when(clueRemarkService.saveClueRemark(any(ClueRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/clue/remark")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1,\"noteContent\":\"Phone call\",\"noteWay\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
