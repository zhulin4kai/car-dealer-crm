package com.bjpowernode.manager;

import com.bjpowernode.mapper.ProductMapper;
import com.bjpowernode.mapper.TClueMapper;
import com.bjpowernode.mapper.TCustomerMapper;
import com.bjpowernode.model.Product;
import com.bjpowernode.model.TClue;
import com.bjpowernode.model.TCustomer;
import com.bjpowernode.model.TTran;
import com.bjpowernode.model.TTranProduct;
import com.bjpowernode.query.CustomerQuery;
import com.bjpowernode.service.TranService;
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
    private ProductMapper productMapper;

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

        TClue clue = new TClue();
        clue.setId(1);
        clue.setState(1);

        Product product = new Product();
        product.setId(5L);
        product.setName("比亚迪e2");
        product.setPrice(new BigDecimal("100000"));

        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(clue);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(1);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(tranService.createTransaction(any(TTran.class), anyList())).thenReturn(1);

        Boolean result = customerManager.convertCustomer(query);

        assertTrue(result);
        verify(tCustomerMapper).insertSelective(any(TCustomer.class));
        verify(tClueMapper).updateByPrimaryKeySelective(any(TClue.class));
        verify(tranService).createTransaction(any(TTran.class), anyList());
    }

    @Test
    void testConvertCustomerAlreadyConverted() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);

        TClue clue = new TClue();
        clue.setId(1);
        clue.setState(-1);

        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(clue);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerManager.convertCustomer(query);
        });

        assertTrue(exception.getMessage().contains("已经转过客户"));
    }

    @Test
    void testConvertCustomerWithNullProduct() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);
        query.setProduct(null);

        TClue clue = new TClue();
        clue.setId(1);
        clue.setState(1);

        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(clue);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(1);
        when(tranService.createTransaction(any(TTran.class), anyList())).thenReturn(1);

        Boolean result = customerManager.convertCustomer(query);

        assertTrue(result);
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void testConvertCustomerInsertFails() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);
        query.setCreateBy(10);

        TClue clue = new TClue();
        clue.setId(1);
        clue.setState(1);

        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(clue);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(0);

        Boolean result = customerManager.convertCustomer(query);

        assertFalse(result);
    }
}
