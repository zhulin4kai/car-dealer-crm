package com.bjpowernode.web;

import com.bjpowernode.model.ProductStockRecord;
import com.bjpowernode.service.ProductService;
import com.bjpowernode.service.ProductStockRecordService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductStockRecordService stockRecordService;

    @Test
    void restock_success() throws Exception {
        doNothing().when(productService).restock(1L, 100, "Restock remark");

        mockMvc.perform(post("/api/productstock/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":100,\"remark\":\"Restock remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockRecords_returnsPageInfo() throws Exception {
        ProductStockRecord record = new ProductStockRecord();
        record.setId(1L);
        record.setProductId(1L);
        record.setQuantity(50);
        record.setType("入库");
        PageInfo<ProductStockRecord> pageInfo = new PageInfo<>(Collections.singletonList(record));

        when(stockRecordService.getStockRecordsByProductId(eq(1L), eq(1), eq(10))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/productstock/records/1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockRecords_defaultParams() throws Exception {
        PageInfo<ProductStockRecord> pageInfo = new PageInfo<>(Collections.emptyList());
        when(stockRecordService.getStockRecordsByProductId(eq(1L), eq(1), eq(10))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/productstock/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockRecords_emptyResult() throws Exception {
        PageInfo<ProductStockRecord> pageInfo = new PageInfo<>(Collections.emptyList());
        when(stockRecordService.getStockRecordsByProductId(eq(999L), eq(1), eq(10))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/productstock/records/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void restock_withMinimalFields() throws Exception {
        doNothing().when(productService).restock(2L, 50, null);

        mockMvc.perform(post("/api/productstock/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":2,\"quantity\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockRecords_multipleRecords() throws Exception {
        ProductStockRecord record1 = new ProductStockRecord();
        record1.setId(1L);
        record1.setProductId(1L);
        record1.setQuantity(50);
        record1.setType("入库");
        ProductStockRecord record2 = new ProductStockRecord();
        record2.setId(2L);
        record2.setProductId(1L);
        record2.setQuantity(-20);
        record2.setType("出库");
        PageInfo<ProductStockRecord> pageInfo = new PageInfo<>(Arrays.asList(record1, record2));

        when(stockRecordService.getStockRecordsByProductId(eq(1L), eq(1), eq(10))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/productstock/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
