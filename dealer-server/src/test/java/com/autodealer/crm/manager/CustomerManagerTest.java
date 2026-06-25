package com.autodealer.crm.manager;

import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.service.TranService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OperationAuditRecorder auditRecorder;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(10);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(10);
    }

    @Test
    void convertCustomer_shouldOnlyCreateCustomerFact() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);
        request.setProduct(5L);
        request.setDescription("测试客户");
        request.setNextContactTime(new Date());

        TProduct product = new TProduct();
        product.setId(5L);
        product.setName("比亚迪e2");

        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(productMapper.selectById(5L)).thenReturn(product);

        assertDoesNotThrow(() -> customerManager.convertCustomer(request));

        verify(tClueMapper).updateStateToConverted(1, 10, 10);
        verify(tCustomerMapper).insertSelective(argThat(customer ->
                customer.getClueId().equals(1)
                        && customer.getProduct().equals(5L)
                        && "测试客户".equals(customer.getDescription())));
        verify(tranService, never()).createTransaction(any(), anyList());
    }

    @Test
    void testConvertCustomerAlreadyConverted() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            customerManager.convertCustomer(request);
        });

        assertEquals(com.autodealer.crm.result.CodeEnum.FAIL, exception.getCodeEnum());
        verify(tClueMapper).updateStateToConverted(1, 10, 10);
        verify(tCustomerMapper, never()).insertSelective(any());
    }

    @Test
    void convertCustomer_withoutProduct_shouldNotCreateTransaction() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);
        request.setProduct(null);
        request.setDescription("测试客户");

        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);

        assertDoesNotThrow(() -> customerManager.convertCustomer(request));

        verify(tCustomerMapper).insertSelective(any(TCustomer.class));
        verify(tranService, never()).createTransaction(any(), anyList());
    }

    @Test
    void testConvertCustomerWithProductNotFound() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);
        request.setProduct(999L);
        request.setDescription("测试客户");

        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> customerManager.convertCustomer(request));
    }

    @Test
    void testConvertCustomerInsertFails() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> customerManager.convertCustomer(request));
        verify(tranService, never()).createTransaction(any(), anyList());
    }
}
