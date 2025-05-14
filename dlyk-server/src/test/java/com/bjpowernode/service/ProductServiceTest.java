package com.bjpowernode.service;

import com.bjpowernode.mapper.ProductMapper;
import com.bjpowernode.mapper.ProductStockRecordMapper;
import com.bjpowernode.model.Product;
import com.bjpowernode.model.ProductStockRecord;
import com.bjpowernode.service.impl.ProductServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductStockRecordMapper productStockRecordMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private List<Product> stockAlertProducts;
    private Product lowStockProduct1;
    private Product lowStockProduct2;

    @BeforeEach
    void setUp() {
        // Setup test data
        lowStockProduct1 = new Product();
        lowStockProduct1.setId(1L);
        lowStockProduct1.setSku("LOW001");
        lowStockProduct1.setName("低库存手机");
        lowStockProduct1.setCategory("电子产品");
        lowStockProduct1.setSpecification("64GB 黑色");
        lowStockProduct1.setPrice(new BigDecimal("2999.00"));
        lowStockProduct1.setStock(5);
        lowStockProduct1.setMinStock(20);
        lowStockProduct1.setStatus("上架");
        lowStockProduct1.setCreateTime(LocalDateTime.now());
        lowStockProduct1.setUpdateTime(LocalDateTime.now());

        lowStockProduct2 = new Product();
        lowStockProduct2.setId(2L);
        lowStockProduct2.setSku("LOW002");
        lowStockProduct2.setName("低库存笔记本");
        lowStockProduct2.setCategory("电子产品");
        lowStockProduct2.setSpecification("i5 8GB 256GB");
        lowStockProduct2.setPrice(new BigDecimal("4999.00"));
        lowStockProduct2.setStock(2);
        lowStockProduct2.setMinStock(10);
        lowStockProduct2.setStatus("上架");
        lowStockProduct2.setCreateTime(LocalDateTime.now());
        lowStockProduct2.setUpdateTime(LocalDateTime.now());

        stockAlertProducts = Arrays.asList(lowStockProduct1, lowStockProduct2);
    }

    @Test
    void testGetStockAlertsWithoutFilter() {
        // Setup mocks
        when(productMapper.selectStockAlerts(null, null)).thenReturn(stockAlertProducts);

        // Execute method
        PageInfo<Product> result = productService.getStockAlerts(1, 10);

        // Verify the result
        assertNotNull(result);
        assertEquals(2, result.getList().size());
        assertEquals("LOW001", result.getList().get(0).getSku());
        assertEquals("LOW002", result.getList().get(1).getSku());
        
        // Verify that the mapper method was called
        verify(productMapper, times(1)).selectStockAlerts(null, null);
    }

    @Test
    void testGetStockAlertsWithSKUFilter() {
        // Setup filtered results
        List<Product> filteredProducts = Arrays.asList(lowStockProduct1);
        
        // Setup mocks
        when(productMapper.selectStockAlertsWithFilter(
            null, null, "LOW001", null, null
        )).thenReturn(filteredProducts);

        // Execute method
        PageInfo<Product> result = productService.getStockAlerts(1, 10, "LOW001", null, null);

        // Verify the result
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("LOW001", result.getList().get(0).getSku());
        
        // Verify that the mapper method was called with correct parameters
        verify(productMapper, times(1)).selectStockAlertsWithFilter(
            null, null, "LOW001", null, null
        );
    }

    @Test
    void testGetStockAlertsWithNameFilter() {
        // Setup filtered results
        List<Product> filteredProducts = Arrays.asList(lowStockProduct2);
        
        // Setup mocks
        when(productMapper.selectStockAlertsWithFilter(
            null, null, null, "笔记本", null
        )).thenReturn(filteredProducts);

        // Execute method
        PageInfo<Product> result = productService.getStockAlerts(1, 10, null, "笔记本", null);

        // Verify the result
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("LOW002", result.getList().get(0).getSku());
        
        // Verify that the mapper method was called with correct parameters
        verify(productMapper, times(1)).selectStockAlertsWithFilter(
            null, null, null, "笔记本", null
        );
    }

    @Test
    void testGetStockAlertsWithCategoryFilter() {
        // Setup mocks - since both products are in the same category, return both
        when(productMapper.selectStockAlertsWithFilter(
            null, null, null, null, "电子产品"
        )).thenReturn(stockAlertProducts);

        // Execute method
        PageInfo<Product> result = productService.getStockAlerts(1, 10, null, null, "电子产品");

        // Verify the result
        assertNotNull(result);
        assertEquals(2, result.getList().size());
        
        // Verify that the mapper method was called with correct parameters
        verify(productMapper, times(1)).selectStockAlertsWithFilter(
            null, null, null, null, "电子产品"
        );
    }

    @Test
    void testGetStockAlertsWithMultipleFilters() {
        // Setup filtered results - the combination of filters should return empty list
        List<Product> filteredProducts = new ArrayList<>();
        
        // Setup mocks
        when(productMapper.selectStockAlertsWithFilter(
            null, null, "LOW001", "笔记本", "电子产品"
        )).thenReturn(filteredProducts);

        // Execute method
        PageInfo<Product> result = productService.getStockAlerts(1, 10, "LOW001", "笔记本", "电子产品");

        // Verify the result
        assertNotNull(result);
        assertEquals(0, result.getList().size());
        
        // Verify that the mapper method was called with correct parameters
        verify(productMapper, times(1)).selectStockAlertsWithFilter(
            null, null, "LOW001", "笔记本", "电子产品"
        );
    }

    @Test
    void testRestock() {
        // Setup
        Long productId = 1L;
        Integer quantity = 10;
        String remark = "补货测试";
        
        // Mock behavior
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);
        when(productStockRecordMapper.insert(any(ProductStockRecord.class))).thenReturn(1);

        // Execute
        productService.restock(productId, quantity, remark);
        
        // Verify that methods were called correctly
        verify(productMapper, times(1)).updateStock(eq(productId), eq(quantity));
        verify(productStockRecordMapper, times(1)).insert(any(ProductStockRecord.class));
    }
} 