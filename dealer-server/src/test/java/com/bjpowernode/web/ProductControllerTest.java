package com.bjpowernode.web;

import com.bjpowernode.model.Product;
import com.bjpowernode.service.ProductService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getProductList_returnsPageInfo() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setSku("SKU001");
        PageInfo<Product> pageInfo = new PageInfo<>(Collections.singletonList(product));

        when(productService.getProductList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getProductList_defaultParams() throws Exception {
        PageInfo<Product> pageInfo = new PageInfo<>(Collections.emptyList());
        when(productService.getProductList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getProductById_returnsProduct() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Product"));
    }

    @Test
    void addProduct_success() throws Exception {
        doNothing().when(productService).addProduct(any(Product.class));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Product\",\"sku\":\"SKU002\",\"price\":49.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProduct_success() throws Exception {
        doNothing().when(productService).updateProduct(any(Product.class));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Product\",\"sku\":\"SKU001\",\"price\":59.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteProduct_success() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockAlerts_returnsPageInfo() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Low Stock Product");
        product.setStock(5);
        product.setMinStock(10);
        PageInfo<Product> pageInfo = new PageInfo<>(Collections.singletonList(product));

        when(productService.getStockAlerts(1, 10, null, null, null)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/products/stockalerts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockAlerts_withFilters() throws Exception {
        PageInfo<Product> pageInfo = new PageInfo<>(Collections.emptyList());
        when(productService.getStockAlerts(1, 10, "SKU001", "Test", "Parts")).thenReturn(pageInfo);

        mockMvc.perform(get("/api/products/stockalerts")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sku", "SKU001")
                        .param("name", "Test")
                        .param("category", "Parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void restock_success() throws Exception {
        doNothing().when(productService).restock(1L, 100, "Restock remark");

        mockMvc.perform(post("/api/products/stock/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":100,\"remark\":\"Restock remark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
