package com.bjpowernode.service;

import com.bjpowernode.mapper.ProductStockRecordMapper;
import com.bjpowernode.model.ProductStockRecord;
import com.bjpowernode.service.impl.ProductStockRecordServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductStockRecordServiceImplTest {

    @InjectMocks
    private ProductStockRecordServiceImpl stockRecordService;

    @Mock
    private ProductStockRecordMapper stockRecordMapper;

    @Test
    void testGetStockRecordsByProductId() {
        Long productId = 1L;
        List<ProductStockRecord> records = Arrays.asList(
                createStockRecord(1L, productId, 100, "IN"),
                createStockRecord(2L, productId, -50, "OUT")
        );
        when(stockRecordMapper.selectByProductId(eq(productId), anyInt(), anyInt())).thenReturn(records);

        PageInfo<ProductStockRecord> result = stockRecordService.getStockRecordsByProductId(productId, 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(stockRecordMapper).selectByProductId(productId, 0, 10);
    }

    @Test
    void testGetStockRecordsByProductIdEmpty() {
        Long productId = 999L;
        when(stockRecordMapper.selectByProductId(eq(productId), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        PageInfo<ProductStockRecord> result = stockRecordService.getStockRecordsByProductId(productId, 1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetStockRecordsByProductIdPagination() {
        Long productId = 1L;
        List<ProductStockRecord> records = Arrays.asList(
                createStockRecord(1L, productId, 10, "IN"),
                createStockRecord(2L, productId, 20, "IN"),
                createStockRecord(3L, productId, -5, "OUT")
        );
        when(stockRecordMapper.selectByProductId(eq(productId), anyInt(), anyInt())).thenReturn(records);

        PageInfo<ProductStockRecord> result = stockRecordService.getStockRecordsByProductId(productId, 2, 10);

        assertNotNull(result);
        assertEquals(3, result.getList().size());
        verify(stockRecordMapper).selectByProductId(productId, 10, 10);
    }

    @Test
    void testGetStockRecordsVerifyCorrectOffset() {
        Long productId = 1L;
        int pageNum = 3;
        int pageSize = 5;
        when(stockRecordMapper.selectByProductId(eq(productId), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        stockRecordService.getStockRecordsByProductId(productId, pageNum, pageSize);

        verify(stockRecordMapper).selectByProductId(productId, 10, 5);
    }

    private ProductStockRecord createStockRecord(Long id, Long productId, Integer quantity, String type) {
        ProductStockRecord record = new ProductStockRecord();
        record.setId(id);
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setType(type);
        record.setRemark("Test remark");
        return record;
    }
}
