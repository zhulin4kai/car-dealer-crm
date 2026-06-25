package com.autodealer.crm.service;

import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductPromotionMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TTranProductMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.result.CodeEnum;
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

    @Mock
    private TTranProductMapper tranProductMapper;

    @Mock
    private TProductPromotionMapper promotionMapper;

    @Mock
    private TCustomerMapper customerMapper;

    @Mock
    private TClueMapper clueMapper;

    @Mock
    private OperationAuditRecorder auditRecorder;

    private TProduct createSampleProduct(Long id, String name, String status) {
        TProduct product = new TProduct();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName(name);
        product.setCategoryId(1L);
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
        TProduct product = createSampleProduct(1L, "Model X", "ON_SALE");
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
        TProduct product = createSampleProduct(1L, "Model X", "ON_SALE");
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
        TProduct product = createSampleProduct(1L, "Model X", "ON_SALE");
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
        TProduct product = createSampleProduct(null, "New Model", "ON_SALE");

        productService.addProduct(product);

        assertNotNull(product.getCreateTime());
        assertNotNull(product.getUpdateTime());
        verify(productMapper).insert(product);
    }

    @Test
    void addProduct_chineseStatus_shouldRejectBeforeInsert() {
        TProduct product = createSampleProduct(null, "New Model", "上架");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.addProduct(product));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(productMapper, never()).insert(any());
    }

    @Test
    void testUpdateProduct() {
        TProduct product = createSampleProduct(1L, "Updated Model", "ON_SALE");

        productService.updateProduct(product);

        assertNotNull(product.getUpdateTime());
        verify(productMapper).update(product);
    }

    @Test
    void updateProduct_chineseStatus_shouldRejectBeforeUpdate() {
        TProduct product = createSampleProduct(1L, "Updated Model", "下架");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.updateProduct(product));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(productMapper, never()).update(any());
    }

    @Test
    void updateProduct_shouldNotPersistStockQuantityFromProductEdit() {
        TProduct product = createSampleProduct(1L, "Updated Model", "ON_SALE");
        product.setStock(999);

        productService.updateProduct(product);

        verify(productMapper).update(argThat(updated -> updated.getStock() == null));
    }

    @Test
    void testDeleteProduct() {
        when(productMapper.selectById(1L)).thenReturn(createSampleProduct(1L, "Model X", "ON_SALE"));

        productService.deleteProduct(1L);

        verify(productMapper).deleteById(1L);
    }

    @Test
    void deleteProduct_notFound_shouldRejectWithoutPhysicalDelete() {
        when(productMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deleteProduct(1L));

        assertEquals(CodeEnum.NOT_FOUND, exception.getCodeEnum());
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteProduct_referencedByTransaction_shouldRejectWithoutPhysicalDelete() {
        when(productMapper.selectById(1L)).thenReturn(createSampleProduct(1L, "Model X", "ON_SALE"));
        when(tranProductMapper.countByProductId(1L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deleteProduct(1L));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteProduct_referencedByStockRecord_shouldRejectWithoutPhysicalDelete() {
        when(productMapper.selectById(1L)).thenReturn(createSampleProduct(1L, "Model X", "ON_SALE"));
        when(stockRecordMapper.selectCountByProductId(1L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deleteProduct(1L));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteProduct_referencedByPromotion_shouldRejectWithoutPhysicalDelete() {
        when(productMapper.selectById(1L)).thenReturn(createSampleProduct(1L, "Model X", "ON_SALE"));
        when(promotionMapper.countByProductId(1L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deleteProduct(1L));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteProduct_referencedByCustomer_shouldRejectWithoutPhysicalDelete() {
        when(productMapper.selectById(1L)).thenReturn(createSampleProduct(1L, "Model X", "ON_SALE"));
        when(customerMapper.countByProductId(1L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deleteProduct(1L));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteProduct_referencedByClue_shouldRejectWithoutPhysicalDelete() {
        when(productMapper.selectById(1L)).thenReturn(createSampleProduct(1L, "Model X", "ON_SALE"));
        when(clueMapper.countByIntentionProductId(1L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deleteProduct(1L));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetStockAlerts() {
        TProduct product = createSampleProduct(1L, "Low Stock Model", "ON_SALE");
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
        TProduct product = createSampleProduct(1L, "Filtered Model", "ON_SALE");
        product.setStock(3);
        List<TProduct> alerts = Collections.singletonList(product);

        when(productMapper.selectStockAlertsWithFilter(null, null, "SKU-1", "Filtered", 1L))
                .thenReturn(alerts);

        PageInfo<TProduct> result = productService.getStockAlerts(1, 10, "SKU-1", "Filtered", 1L);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        verify(productMapper).selectStockAlertsWithFilter(null, null, "SKU-1", "Filtered", 1L);
    }

    @Test
    void testRestock() {
        when(productMapper.updateStock(1L, 50)).thenReturn(1);
        when(stockRecordMapper.insert(any())).thenReturn(1);

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
        TProduct product1 = createSampleProduct(1L, "Model X", "ON_SALE");
        TProduct product2 = createSampleProduct(2L, "Model Y", "ON_SALE");
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
        TProduct product = createSampleProduct(1L, "Sold Out Model", "OFF_SALE");

        when(productMapper.selectAllOnSale()).thenReturn(Collections.singletonList(product));

        List<ProductSimpleDTO> result = productService.getAllOnSaleProduct();

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getState());
    }
}
