package com.autodealer.crm.manager;

import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.query.CustomerQuery;
import com.autodealer.crm.service.TranService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerManagerTest {

    @InjectMocks
    private CustomerManager customerManager;

    @Mock
    private TCustomerMapper tCustomerMapper;

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private TProductMapper productMapper;

    @Mock
    private TranService tranService;

    @Test
    void testConvertCustomerSuccess() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);
        query.setProduct(5);
        query.setDescription("测试客户");
        query.setNextContactTime(new Date());

        TProduct product = new TProduct();
        product.setId(5L);
        product.setName("比亚迪e2");
        product.setPrice(new BigDecimal("100000"));

        when(tClueMapper.updateStateToConverted(1, 10)).thenReturn(1); // 原子性更新成功
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(tranService.createTransaction(any(TTran.class), anyList())).thenReturn(1);

        Boolean result = customerManager.convertCustomer(query);

        assertTrue(result);
        verify(tClueMapper).updateStateToConverted(1, 10);
        verify(tCustomerMapper).insertSelective(any(TCustomer.class));
        verify(tranService).createTransaction(any(TTran.class), anyList());
    }

    @Test
    void testConvertCustomerAlreadyConverted() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);

        when(tClueMapper.updateStateToConverted(1, 10)).thenReturn(0); // 已经转过客户，返回0

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerManager.convertCustomer(query);
        });

        assertTrue(exception.getMessage().contains("已经转过客户"));
        verify(tClueMapper).updateStateToConverted(1, 10);
        verify(tCustomerMapper, never()).insertSelective(any());
    }

    @Test
    void testConvertCustomerWithNullProduct() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);
        query.setProduct(null);
        query.setDescription("测试客户");

        when(tClueMapper.updateStateToConverted(1, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(tranService.createTransaction(any(TTran.class), anyList())).thenReturn(1);

        Boolean result = customerManager.convertCustomer(query);

        assertTrue(result);
        verify(tCustomerMapper).insertSelective(any(TCustomer.class));
        verify(tranService).createTransaction(any(TTran.class), argThat(products -> products.isEmpty()));
    }

    @Test
    void testConvertCustomerWithProductNotFound() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);
        query.setProduct(999);
        query.setDescription("测试客户");

        when(tClueMapper.updateStateToConverted(1, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(productMapper.selectById(999L)).thenReturn(null);
        when(tranService.createTransaction(any(TTran.class), anyList())).thenReturn(1);

        Boolean result = customerManager.convertCustomer(query);

        assertTrue(result);
        verify(tranService).createTransaction(any(TTran.class), argThat(products -> products.isEmpty()));
    }

    @Test
    void testConvertCustomerInsertFails() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);

        when(tClueMapper.updateStateToConverted(1, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(0);

        Boolean result = customerManager.convertCustomer(query);

        assertFalse(result);
        verify(tranService, never()).createTransaction(any(), anyList());
    }
}
