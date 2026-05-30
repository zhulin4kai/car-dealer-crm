package com.bjpowernode.web;

import com.bjpowernode.model.TSystem;
import com.bjpowernode.service.SystemService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemService systemService;

    @Test
    void getAllList_returnsList() throws Exception {
        TSystem system1 = new TSystem();
        system1.setId(1);
        system1.setName("System A");
        TSystem system2 = new TSystem();
        system2.setId(2);
        system2.setName("System B");
        List<TSystem> list = Arrays.asList(system1, system2);

        when(systemService.getAllList()).thenReturn(list);

        mockMvc.perform(get("/api/system/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getAllList_emptyList() throws Exception {
        when(systemService.getAllList()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/system/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getSystemDetail_returnsSystem() throws Exception {
        TSystem system = new TSystem();
        system.setId(1);
        system.setName("Test System");
        system.setIsopen("1");

        when(systemService.getById(1)).thenReturn(system);

        mockMvc.perform(get("/api/system/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test System"));
    }

    @Test
    void createSystem_success() throws Exception {
        doNothing().when(systemService).create(any(TSystem.class));

        mockMvc.perform(post("/api/system/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New System\",\"systemCode\":\"SYS001\",\"isopen\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateSystem_success() throws Exception {
        doNothing().when(systemService).update(eq(1), any(TSystem.class));

        mockMvc.perform(put("/api/system/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated System\",\"isopen\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteSystem_success() throws Exception {
        doNothing().when(systemService).delete(1);

        mockMvc.perform(delete("/api/system/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void batchDeleteSystems_success() throws Exception {
        doNothing().when(systemService).batchDelete(any());

        mockMvc.perform(delete("/api/system/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void toggleSystemStatus_success() throws Exception {
        doNothing().when(systemService).toggleStatus(1, "0");

        mockMvc.perform(put("/api/system/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isopen\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
