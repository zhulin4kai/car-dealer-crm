package com.autodealer.crm.service;

import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.service.impl.ProductServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private TProductMapper productMapper;

    @Mock
    private TProductStockRecordMapper stockRecordMapper;

    private TProduct createSampleProduct(Long id, String name, String status) {
        TProduct product = new TProduct();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName(name);
        product.setCategory("Cars");
        product.setPrice(BigDecimal.valueOf(200000));
        product.setStock(10);
        product.setMinStock(5);
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        product.setUpdateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        return product;
    }

    @Test
    void testGetProductList() {
        TProduct product = createSampleProduct(1L, "Model X", "on_sale");
        List<TProduct> products = Collections.singletonList(product);

        when(productMapper.selectList(0, 10)).thenReturn(products);

        PageInfo<TProduct> result = productService.getProductList(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("Model X", result.getList().get(0).getName());
        verify(productMapper).selectList(0, 10);
    }

    @Test
    void testGetProductListEmpty() {
        when(productMapper.selectList(0, 10)).thenReturn(Collections.emptyList());

        PageInfo<TProduct> result = productService.getProductList(1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetProductById() {
        TProduct product = createSampleProduct(1L, "Model X", "on_sale");
        when(productMapper.selectById(1L)).thenReturn(product);

        TProduct result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Model X", result.getName());
        verify(productMapper).selectById(1L);
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);

        TProduct result = productService.getProductById(999L);

        assertNull(result);
    }

    @Test
    void testGetProductBySku() {
        TProduct product = createSampleProduct(1L, "Model X", "on_sale");
        when(productMapper.selectBySku("SKU-1")).thenReturn(product);

        TProduct result = productService.getProductBySku("SKU-1");

        assertNotNull(result);
        assertEquals("SKU-1", result.getSku());
        verify(productMapper).selectBySku("SKU-1");
    }

    @Test
    void testGetProductBySkuNotFound() {
        when(productMapper.selectBySku("NONEXISTENT")).thenReturn(null);

        TProduct result = productService.getProductBySku("NONEXISTENT");

        assertNull(result);
    }

    @Test
    void testAddProduct() {
        TProduct product = createSampleProduct(null, "New Model", "on_sale");

        productService.addProduct(product);

        assertNotNull(product.getCreateTime());
        assertNotNull(product.getUpdateTime());
        verify(productMapper).insert(product);
    }

    @Test
    void testUpdateProduct() {
        TProduct product = createSampleProduct(1L, "Updated Model", "on_sale");

        productService.updateProduct(product);

        assertNotNull(product.getUpdateTime());
        verify(productMapper).update(product);
    }

    @Test
    void testDeleteProduct() {
        productService.deleteProduct(1L);

        verify(productMapper).deleteById(1L);
    }

    @Test
    void testGetStockAlerts() {
        TProduct product = createSampleProduct(1L, "Low Stock Model", "on_sale");
        product.setStock(2);
        product.setMinStock(5);
        List<TProduct> alerts = Collections.singletonList(product);

        when(productMapper.selectStockAlerts(null, null)).thenReturn(alerts);

        PageInfo<TProduct> result = productService.getStockAlerts(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals(2, result.getList().get(0).getStock());
        verify(productMapper).selectStockAlerts(null, null);
    }

    @Test
    void testGetStockAlertsWithFilter() {
        TProduct product = createSampleProduct(1L, "Filtered Model", "on_sale");
        product.setStock(3);
        List<TProduct> alerts = Collections.singletonList(product);

        when(productMapper.selectStockAlertsWithFilter(null, null, "SKU-1", "Filtered", "Cars"))
                .thenReturn(alerts);

        PageInfo<TProduct> result = productService.getStockAlerts(1, 10, "SKU-1", "Filtered", "Cars");

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        verify(productMapper).selectStockAlertsWithFilter(null, null, "SKU-1", "Filtered", "Cars");
    }

    @Test
    void testRestock() {
        productService.restock(1L, 50, "Restocking order");

        verify(productMapper).updateStock(1L, 50);
        verify(stockRecordMapper).insert(argThat(record ->
                record.getProductId().equals(1L)
                        && record.getQuantity().equals(50)
                        && "入库".equals(record.getType())
                        && "Restocking order".equals(record.getRemark())
                        && record.getCreateTime() != null
        ));
    }

    @Test
    void testUpdateStock() {
        productService.updateStock(1L, 100);

        verify(productMapper).updateStock(1L, 100);
    }

    @Test
    void testGetAllOnSaleProduct() {
        TProduct product1 = createSampleProduct(1L, "Model X", "on_sale");
        TProduct product2 = createSampleProduct(2L, "Model Y", "on_sale");
        List<TProduct> products = Arrays.asList(product1, product2);

        when(productMapper.selectAllOnSale()).thenReturn(products);

        List<ProductSimpleDTO> result = productService.getAllOnSaleProduct();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Model X", result.get(0).getName());
        assertEquals("Model Y", result.get(1).getName());
        assertEquals(Integer.valueOf(0), result.get(0).getState());
        assertEquals(BigDecimal.valueOf(200000), result.get(0).getGuidePriceS());
        assertEquals(BigDecimal.valueOf(200000), result.get(0).getQuotation());
        verify(productMapper).selectAllOnSale();
    }

    @Test
    void testGetAllOnSaleProductEmpty() {
        when(productMapper.selectAllOnSale()).thenReturn(Collections.emptyList());

        List<ProductSimpleDTO> result = productService.getAllOnSaleProduct();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllOnSaleProductConvertsStateOff() {
        TProduct product = createSampleProduct(1L, "Sold Out Model", "off_sale");

        when(productMapper.selectAllOnSale()).thenReturn(Collections.singletonList(product));

        List<ProductSimpleDTO> result = productService.getAllOnSaleProduct();

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getState());
    }
}
