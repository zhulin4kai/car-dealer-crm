package com.bjpowernode.web;

import com.bjpowernode.model.TClue;
import com.bjpowernode.query.ClueQuery;
import com.bjpowernode.service.ClueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ClueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClueService clueService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/clues ====================

    @Test
    @WithMockUser(authorities = {"clue:list"})
    void cluePage_shouldReturnPageInfo() throws Exception {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setFullName("张三");
        clue.setPhone("13800138000");
        PageInfo<TClue> pageInfo = new PageInfo<>(Collections.singletonList(clue));

        when(clueService.getClueByPage(1, null)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clues")
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].fullName").value("张三"));
    }

    @Test
    @WithMockUser(authorities = {"clue:list"})
    void cluePage_withoutCurrentParam_shouldDefaultToOne() throws Exception {
        PageInfo<TClue> pageInfo = new PageInfo<>(Collections.emptyList());
        when(clueService.getClueByPage(1, null)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/clue/{phone} ====================

    @Test
    @WithMockUser
    void checkPhone_available_shouldReturnOk() throws Exception {
        when(clueService.checkPhone("13800138000")).thenReturn(true);

        mockMvc.perform(get("/api/clue/13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void checkPhone_taken_shouldReturnFail() throws Exception {
        when(clueService.checkPhone("13800138000")).thenReturn(false);

        mockMvc.perform(get("/api/clue/13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== POST /api/clue ====================

    @Test
    @WithMockUser(authorities = {"clue:add"})
    void addClue_success_shouldReturnOk() throws Exception {
        when(clueService.saveClue(any(ClueQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("fullName", "李四")
                        .param("phone", "13900139000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:add"})
    void addClue_failure_shouldReturnFail() throws Exception {
        when(clueService.saveClue(any(ClueQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("fullName", "李四")
                        .param("phone", "13900139000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== GET /api/clue/detail/{id} ====================

    @Test
    @WithMockUser(authorities = {"clue:view"})
    void loadClue_shouldReturnClue() throws Exception {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setFullName("张三");
        clue.setPhone("13800138000");

        when(clueService.getClueById(1)).thenReturn(clue);

        mockMvc.perform(get("/api/clue/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fullName").value("张三"));
    }

    @Test
    @WithMockUser(authorities = {"clue:view"})
    void loadClue_nonExistentId_shouldReturnNull() throws Exception {
        when(clueService.getClueById(999)).thenReturn(null);

        mockMvc.perform(get("/api/clue/detail/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== PUT /api/clue ====================

    @Test
    @WithMockUser(authorities = {"clue:edit"})
    void editClue_success_shouldReturnOk() throws Exception {
        when(clueService.updateClue(any(ClueQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1")
                        .param("fullName", "张三改"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:edit"})
    void editClue_failure_shouldReturnFail() throws Exception {
        when(clueService.updateClue(any(ClueQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1")
                        .param("fullName", "张三改"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== DELETE /api/clue/{id} ====================

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void delClue_success_shouldReturnOk() throws Exception {
        when(clueService.delClueById(1)).thenReturn(1);

        mockMvc.perform(delete("/api/clue/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void delClue_failure_shouldReturnFail() throws Exception {
        when(clueService.delClueById(1)).thenReturn(0);

        mockMvc.perform(delete("/api/clue/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== POST /api/clue/batch ====================

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void batchDelClue_success_shouldReturnOk() throws Exception {
        when(clueService.batchDelClueByIds(anyList())).thenReturn(2);

        mockMvc.perform(post("/api/clue/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void batchDelClue_failure_shouldReturnFail() throws Exception {
        when(clueService.batchDelClueByIds(anyList())).thenReturn(0);

        mockMvc.perform(post("/api/clue/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== POST /api/importExcel ====================

    @Test
    @WithMockUser(authorities = {"clue:import"})
    void importExcel_shouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clues.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test data".getBytes());

        mockMvc.perform(multipart("/api/importExcel")
                        .file(file)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

}
