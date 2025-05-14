package com.bjpowernode.web;

import com.bjpowernode.config.TestConfig;
import com.bjpowernode.model.Product;
import com.bjpowernode.result.Result;
import com.bjpowernode.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("测试获取库存预警商品接口 - 不带筛选条件")
    public void testGetStockAlertsWithoutFilters() throws Exception {
        // 创建测试数据
        List<Product> products = createTestProducts();
        PageInfo<Product> pageInfo = new PageInfo<>(products);
        pageInfo.setTotal(products.size());
        
        // Mock service方法
        when(productService.getStockAlerts(anyInt(), anyInt(), isNull(), isNull(), isNull()))
                .thenReturn(pageInfo);
        
        // 执行GET请求
        MvcResult result = mockMvc.perform(get("/api/products/stock-alerts")
                .param("page", "1")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(products.size()))
                .andReturn();
        
        // 解析并验证返回结果
        String content = result.getResponse().getContentAsString();
        Result<PageInfo<Product>> response = objectMapper.readValue(content, 
                new TypeReference<Result<PageInfo<Product>>>() {});
        
        assertNotNull(response.getData());
        assertEquals(products.size(), response.getData().getList().size());
        assertEquals("LOW001", response.getData().getList().get(0).getSku());
        
        // 验证service方法被调用
        verify(productService, times(1))
                .getStockAlerts(eq(1), eq(10), isNull(), isNull(), isNull());
    }
    
    @Test
    @DisplayName("测试获取库存预警商品接口 - 带筛选条件")
    public void testGetStockAlertsWithFilters() throws Exception {
        // 创建测试数据 - 只包含符合筛选条件的产品
        List<Product> products = new ArrayList<>();
        products.add(createTestProduct(1L, "LOW001", "低库存手机", "电子产品", 5, 20));
        PageInfo<Product> pageInfo = new PageInfo<>(products);
        pageInfo.setTotal(products.size());
        
        // Mock service方法
        when(productService.getStockAlerts(anyInt(), anyInt(), eq("LOW001"), eq("手机"), eq("电子产品")))
                .thenReturn(pageInfo);
        
        // 执行GET请求，带筛选参数
        MvcResult result = mockMvc.perform(get("/api/products/stock-alerts")
                .param("page", "1")
                .param("size", "10")
                .param("sku", "LOW001")
                .param("name", "手机")
                .param("category", "电子产品")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();
        
        // 解析并验证返回结果
        String content = result.getResponse().getContentAsString();
        Result<PageInfo<Product>> response = objectMapper.readValue(content, 
                new TypeReference<Result<PageInfo<Product>>>() {});
        
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getList().size());
        assertEquals("LOW001", response.getData().getList().get(0).getSku());
        assertEquals("低库存手机", response.getData().getList().get(0).getName());
        assertEquals("电子产品", response.getData().getList().get(0).getCategory());
        
        // 验证service方法被调用，并且参数正确
        verify(productService, times(1))
                .getStockAlerts(eq(1), eq(10), eq("LOW001"), eq("手机"), eq("电子产品"));
    }
    
    @Test
    @DisplayName("测试获取库存预警商品接口 - 空结果")
    public void testGetStockAlertsEmptyResult() throws Exception {
        // 创建空结果数据
        List<Product> products = new ArrayList<>();
        PageInfo<Product> pageInfo = new PageInfo<>(products);
        pageInfo.setTotal(0);
        
        // Mock service方法返回空结果
        when(productService.getStockAlerts(anyInt(), anyInt(), eq("NONEXISTENT"), isNull(), isNull()))
                .thenReturn(pageInfo);
        
        // 执行GET请求，使用不存在的SKU
        MvcResult result = mockMvc.perform(get("/api/products/stock-alerts")
                .param("page", "1")
                .param("size", "10")
                .param("sku", "NONEXISTENT")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0))
                .andReturn();
        
        // 解析并验证返回结果
        String content = result.getResponse().getContentAsString();
        Result<PageInfo<Product>> response = objectMapper.readValue(content, 
                new TypeReference<Result<PageInfo<Product>>>() {});
        
        assertNotNull(response.getData());
        assertTrue(response.getData().getList().isEmpty());
        assertEquals(0, response.getData().getTotal());
        
        // 验证service方法被调用
        verify(productService, times(1))
                .getStockAlerts(eq(1), eq(10), eq("NONEXISTENT"), isNull(), isNull());
    }
    
    // 创建测试产品列表
    private List<Product> createTestProducts() {
        List<Product> products = new ArrayList<>();
        products.add(createTestProduct(1L, "LOW001", "低库存手机", "电子产品", 5, 20));
        products.add(createTestProduct(2L, "LOW002", "低库存笔记本", "电子产品", 2, 10));
        products.add(createTestProduct(3L, "LOW003", "低库存平板", "电子产品", 3, 15));
        return products;
    }
    
    // 创建单个测试产品
    private Product createTestProduct(Long id, String sku, String name, String category, 
                                     Integer stock, Integer minStock) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setSpecification("测试规格");
        product.setPrice(new BigDecimal("1999.00"));
        product.setStock(stock);
        product.setMinStock(minStock);
        product.setStatus("上架");
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        return product;
    }
} 