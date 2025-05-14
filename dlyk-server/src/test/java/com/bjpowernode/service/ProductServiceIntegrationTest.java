package com.bjpowernode.service;

import com.bjpowernode.model.Product;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Test
    void testGetStockAlertsNoFilter() {
        // Execute
        PageInfo<Product> result = productService.getStockAlerts(1, 10);
        
        // Verify - we expect all low stock products to be returned
        assertNotNull(result);
        assertTrue(result.getList().size() > 0, "Should find at least one low stock product");
        
        // Verify that all returned products have stock <= minStock
        result.getList().forEach(product -> 
            assertTrue(product.getStock() <= product.getMinStock(), 
                "Product " + product.getSku() + " should have stock <= minStock")
        );
    }
    
    @Test
    void testGetStockAlertsWithSkuFilter() {
        // We're expecting to find products with SKU like LOW00
        PageInfo<Product> result = productService.getStockAlerts(1, 10, "LOW00", null, null);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.getList().size() > 0, "Should find products with SKU like LOW00");
        
        // All returned products should have SKU containing "LOW00"
        result.getList().forEach(product -> 
            assertTrue(product.getSku().contains("LOW00"), 
                "Product SKU should contain LOW00, but was: " + product.getSku())
        );
    }
    
    @Test
    void testGetStockAlertsWithNameFilter() {
        // We're looking for products with name containing "低库存"
        PageInfo<Product> result = productService.getStockAlerts(1, 10, null, "低库存", null);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.getList().size() > 0, "Should find products with name containing 低库存");
        
        // All returned products should have name containing "低库存"
        result.getList().forEach(product -> 
            assertTrue(product.getName().contains("低库存"), 
                "Product name should contain 低库存, but was: " + product.getName())
        );
    }
    
    @Test
    void testGetStockAlertsWithCategoryFilter() {
        // We're looking for products in the "电子产品" category
        PageInfo<Product> result = productService.getStockAlerts(1, 10, null, null, "电子产品");
        
        // Verify
        assertNotNull(result);
        assertTrue(result.getList().size() > 0, "Should find products in 电子产品 category");
        
        // All returned products should be in the "电子产品" category
        result.getList().forEach(product -> 
            assertEquals("电子产品", product.getCategory(), 
                "Product category should be 电子产品, but was: " + product.getCategory())
        );
    }
    
    @Test
    void testGetStockAlertsWithMultipleFilters() {
        // Look for a specific product by combining filters
        PageInfo<Product> result = productService.getStockAlerts(1, 10, "LOW001", "手机", "电子产品");
        
        // Verify - this should return a specific product if it exists
        assertNotNull(result);
        
        if (result.getList().size() > 0) {
            Product product = result.getList().get(0);
            assertEquals("LOW001", product.getSku());
            assertTrue(product.getName().contains("手机"));
            assertEquals("电子产品", product.getCategory());
        }
    }
    
    @Test
    void testGetStockAlertsWithNonExistentFilters() {
        // Look for products with filters that shouldn't match anything
        PageInfo<Product> result = productService.getStockAlerts(1, 10, "NONEXISTENT", null, null);
        
        // Verify - should return empty list
        assertNotNull(result);
        assertEquals(0, result.getList().size(), "Should not find any products with non-existent SKU");
    }
} 