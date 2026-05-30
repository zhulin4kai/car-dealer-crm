package com.bjpowernode.web;

import com.bjpowernode.model.TDicType;
import com.bjpowernode.model.TDicValue;
import com.bjpowernode.query.DicQuery;
import com.bjpowernode.service.DicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
class DicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DicService dicService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/dict/types ====================

    @Test
    @WithMockUser
    void getDicTypes_shouldReturnPageInfo() throws Exception {
        TDicType dicType = new TDicType();
        dicType.setId(1);
        dicType.setTypeCode("source");
        dicType.setTypeName("来源");
        PageInfo<TDicType> pageInfo = new PageInfo<>(Collections.singletonList(dicType));

        when(dicService.getDicTypes(any(DicQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/dict/types")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].typeCode").value("source"));
    }

    @Test
    @WithMockUser
    void getDicTypes_withDefaultPaging_shouldUseDefaults() throws Exception {
        PageInfo<TDicType> pageInfo = new PageInfo<>(Collections.emptyList());
        when(dicService.getDicTypes(any(DicQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/dict/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/dict/type/get/{id} ====================

    @Test
    @WithMockUser
    void getDicTypeById_shouldReturnDicType() throws Exception {
        TDicType dicType = new TDicType();
        dicType.setId(1);
        dicType.setTypeCode("source");
        dicType.setTypeName("来源");

        when(dicService.getDicTypeById(1)).thenReturn(dicType);

        mockMvc.perform(get("/api/dict/type/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.typeCode").value("source"));
    }

    @Test
    @WithMockUser
    void getDicTypeById_nonExistentId_shouldReturnNull() throws Exception {
        when(dicService.getDicTypeById(999)).thenReturn(null);

        mockMvc.perform(get("/api/dict/type/get/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== POST /api/dict/type/create ====================

    @Test
    @WithMockUser
    void addDicType_success_shouldReturnOk() throws Exception {
        TDicType dicType = new TDicType();
        dicType.setTypeCode("intention");
        dicType.setTypeName("意向");

        when(dicService.addDicType(any(TDicType.class))).thenReturn(true);

        mockMvc.perform(post("/api/dict/type/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void addDicType_failure_shouldReturnFail() throws Exception {
        TDicType dicType = new TDicType();
        dicType.setTypeCode("intention");
        dicType.setTypeName("意向");

        when(dicService.addDicType(any(TDicType.class))).thenReturn(false);

        mockMvc.perform(post("/api/dict/type/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("添加字典类型失败"));
    }

    // ==================== PUT /api/dict/type/update/{id} ====================

    @Test
    @WithMockUser
    void updateDicType_success_shouldReturnOk() throws Exception {
        TDicType dicType = new TDicType();
        dicType.setTypeCode("source");
        dicType.setTypeName("来源更新");

        when(dicService.updateDicType(eq(1), any(TDicType.class))).thenReturn(true);

        mockMvc.perform(put("/api/dict/type/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void updateDicType_failure_shouldReturnFail() throws Exception {
        TDicType dicType = new TDicType();
        dicType.setTypeCode("source");
        dicType.setTypeName("来源更新");

        when(dicService.updateDicType(eq(1), any(TDicType.class))).thenReturn(false);

        mockMvc.perform(put("/api/dict/type/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("更新字典类型失败"));
    }

    // ==================== DELETE /api/dict/type/delete/{id} ====================

    @Test
    @WithMockUser
    void deleteDicType_success_shouldReturnOk() throws Exception {
        when(dicService.deleteDicType(1)).thenReturn(true);

        mockMvc.perform(delete("/api/dict/type/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void deleteDicType_failure_shouldReturnFail() throws Exception {
        when(dicService.deleteDicType(1)).thenReturn(false);

        mockMvc.perform(delete("/api/dict/type/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("删除字典类型失败"));
    }

    // ==================== DELETE /api/dict/types/batch ====================

    @Test
    @WithMockUser
    void batchDeleteDicTypes_success_shouldReturnOk() throws Exception {
        when(dicService.deleteDicTypesByIds(anyList())).thenReturn(true);

        mockMvc.perform(delete("/api/dict/types/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void batchDeleteDicTypes_failure_shouldReturnFail() throws Exception {
        when(dicService.deleteDicTypesByIds(anyList())).thenReturn(false);

        mockMvc.perform(delete("/api/dict/types/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("批量删除字典类型失败"));
    }

    // ==================== GET /api/dict/values ====================

    @Test
    @WithMockUser
    void getDicValues_shouldReturnPageInfo() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(1);
        dicValue.setTypeCode("source");
        dicValue.setTypeValue("网络");
        PageInfo<TDicValue> pageInfo = new PageInfo<>(Collections.singletonList(dicValue));

        when(dicService.getDicValues(any(DicQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/dict/values")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].typeValue").value("网络"));
    }

    @Test
    @WithMockUser
    void getDicValues_withDefaultPaging_shouldUseDefaults() throws Exception {
        PageInfo<TDicValue> pageInfo = new PageInfo<>(Collections.emptyList());
        when(dicService.getDicValues(any(DicQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/dict/values"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/dict/value/get/{id} ====================

    @Test
    @WithMockUser
    void getDicValueById_shouldReturnDicValue() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(1);
        dicValue.setTypeCode("source");
        dicValue.setTypeValue("网络");

        when(dicService.getDicValueById(1)).thenReturn(dicValue);

        mockMvc.perform(get("/api/dict/value/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.typeValue").value("网络"));
    }

    // ==================== POST /api/dict/value/create ====================

    @Test
    @WithMockUser
    void addDicValue_success_shouldReturnOk() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode("source");
        dicValue.setTypeValue("线下");

        when(dicService.addDicValue(any(TDicValue.class))).thenReturn(true);

        mockMvc.perform(post("/api/dict/value/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void addDicValue_failure_shouldReturnFail() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode("source");
        dicValue.setTypeValue("线下");

        when(dicService.addDicValue(any(TDicValue.class))).thenReturn(false);

        mockMvc.perform(post("/api/dict/value/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("添加字典值失败"));
    }

    // ==================== PUT /api/dict/value/update/{id} ====================

    @Test
    @WithMockUser
    void updateDicValue_success_shouldReturnOk() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode("source");
        dicValue.setTypeValue("网络更新");

        when(dicService.updateDicValue(eq(1), any(TDicValue.class))).thenReturn(true);

        mockMvc.perform(put("/api/dict/value/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void updateDicValue_failure_shouldReturnFail() throws Exception {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode("source");
        dicValue.setTypeValue("网络更新");

        when(dicService.updateDicValue(eq(1), any(TDicValue.class))).thenReturn(false);

        mockMvc.perform(put("/api/dict/value/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dicValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("更新字典值失败"));
    }

    // ==================== DELETE /api/dict/value/delete/{id} ====================

    @Test
    @WithMockUser
    void deleteDicValue_success_shouldReturnOk() throws Exception {
        when(dicService.deleteDicValue(1)).thenReturn(true);

        mockMvc.perform(delete("/api/dict/value/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void deleteDicValue_failure_shouldReturnFail() throws Exception {
        when(dicService.deleteDicValue(1)).thenReturn(false);

        mockMvc.perform(delete("/api/dict/value/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("删除字典值失败"));
    }

    // ==================== DELETE /api/dict/value/batch ====================

    @Test
    @WithMockUser
    void batchDeleteDicValues_success_shouldReturnOk() throws Exception {
        when(dicService.deleteDicValuesByIds(anyList())).thenReturn(true);

        mockMvc.perform(delete("/api/dict/value/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void batchDeleteDicValues_failure_shouldReturnFail() throws Exception {
        when(dicService.deleteDicValuesByIds(anyList())).thenReturn(false);

        mockMvc.perform(delete("/api/dict/value/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("批量删除字典值失败"));
    }

    // ==================== GET /api/dict/clear ====================

    @Test
    @WithMockUser(authorities = {"admin"})
    void clearCache_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/dict/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"admin"})
    void clearCache_withForceRefresh_shouldRefreshAll() throws Exception {
        mockMvc.perform(get("/api/dict/clear")
                        .param("forceRefresh", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dicService).clearCache("*");
        verify(dicService).refreshTypeCache();
        verify(dicService).refreshValueCache();
    }

    // ==================== GET /api/dict/refresh ====================

    @Test
    @WithMockUser
    void refreshDictData_withType_shouldRefreshType() throws Exception {
        mockMvc.perform(get("/api/dict/refresh")
                        .param("type", "type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dicService).refreshTypeCache();
        verify(dicService, never()).refreshValueCache();
    }

    @Test
    @WithMockUser
    void refreshDictData_withValue_shouldRefreshValue() throws Exception {
        mockMvc.perform(get("/api/dict/refresh")
                        .param("type", "value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dicService).refreshValueCache();
        verify(dicService, never()).refreshTypeCache();
    }

    @Test
    @WithMockUser
    void refreshDictData_withoutType_shouldRefreshAll() throws Exception {
        mockMvc.perform(get("/api/dict/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dicService).refreshTypeCache();
        verify(dicService).refreshValueCache();
    }
}
