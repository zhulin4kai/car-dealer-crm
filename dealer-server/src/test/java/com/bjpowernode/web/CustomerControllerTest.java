package com.bjpowernode.web;

import com.bjpowernode.model.CustomerOption;
import com.bjpowernode.model.TCustomer;
import com.bjpowernode.query.CustomerQuery;
import com.bjpowernode.result.CustomerExcel;
import com.bjpowernode.service.CustomerService;
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

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/customer/list ====================

    @Test
    @WithMockUser
    void list_shouldReturnPageInfo() throws Exception {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setClueId(100);
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.singletonList(customer));

        when(customerService.getCustomerList(any(CustomerQuery.class), eq(1), eq(10)))
                .thenReturn(pageInfo);

        mockMvc.perform(get("/api/customer/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(1));
    }

    @Test
    @WithMockUser
    void list_withDefaultParams_shouldUseDefaults() throws Exception {
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.emptyList());
        when(customerService.getCustomerList(any(CustomerQuery.class), eq(1), eq(10)))
                .thenReturn(pageInfo);

        mockMvc.perform(get("/api/customer/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/customer/options ====================

    @Test
    @WithMockUser
    void options_shouldReturnCustomerOptions() throws Exception {
        CustomerOption option = new CustomerOption();
        option.setCustomerId(1);
        option.setCustomerName("张三");

        when(customerService.getCustomerOptions()).thenReturn(Collections.singletonList(option));

        mockMvc.perform(get("/api/customer/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].customerName").value("张三"));
    }

    @Test
    @WithMockUser
    void options_emptyList_shouldReturnEmpty() throws Exception {
        when(customerService.getCustomerOptions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/customer/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== GET /api/customer/{id} ====================

    @Test
    @WithMockUser
    void detail_shouldReturnCustomer() throws Exception {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setClueId(100);

        when(customerService.getCustomerById(1)).thenReturn(customer);

        mockMvc.perform(get("/api/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser
    void detail_nonExistentId_shouldReturnNull() throws Exception {
        when(customerService.getCustomerById(999)).thenReturn(null);

        mockMvc.perform(get("/api/customer/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== POST /api/clue/customer ====================

    @Test
    @WithMockUser
    void convertCustomer_success_shouldReturnOk() throws Exception {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);

        when(customerService.convertCustomer(any(CustomerQuery.class))).thenReturn(true);

        mockMvc.perform(post("/api/clue/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void convertCustomer_failure_shouldReturnFail() throws Exception {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);

        when(customerService.convertCustomer(any(CustomerQuery.class))).thenReturn(false);

        mockMvc.perform(post("/api/clue/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== GET /api/customers ====================

    @Test
    @WithMockUser
    void cluePage_shouldReturnPageInfo() throws Exception {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.singletonList(customer));

        when(customerService.getCustomerByPage(1)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/customers")
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(1));
    }

    @Test
    @WithMockUser
    void cluePage_withoutCurrentParam_shouldDefaultToOne() throws Exception {
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.emptyList());
        when(customerService.getCustomerByPage(1)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/exportExcel ====================

    @Test
    @WithMockUser
    void exportExcel_shouldReturnExcelFile() throws Exception {
        CustomerExcel excel = new CustomerExcel();
        excel.setFullName("张三");
        excel.setPhone("13800138000");

        when(customerService.getCustomerByExcel(anyList()))
                .thenReturn(Collections.singletonList(excel));

        mockMvc.perform(get("/api/exportExcel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    @WithMockUser
    void exportExcel_withIds_shouldFilterByIds() throws Exception {
        when(customerService.getCustomerByExcel(anyList()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/exportExcel")
                        .param("ids", "1,2,3"))
                .andExpect(status().isOk());
    }
}
