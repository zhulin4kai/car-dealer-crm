package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.manager.CustomerManager;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.CustomerQuery;
import com.autodealer.crm.result.CustomerExcel;
import com.autodealer.crm.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Mock
    private CustomerManager customerManager;

    @Mock
    private TCustomerMapper tCustomerMapper;

    @Mock
    private TTranMapper tTranMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    // ==================== getCustomerList ====================

    @Test
    void getCustomerList_shouldReturnPageInfo() {
        CustomerQuery query = new CustomerQuery();
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setDescription("Test customer");
        List<TCustomer> list = Collections.singletonList(customer);

        when(tCustomerMapper.selectByQuery(query)).thenReturn(list);

        var pageInfo = customerService.getCustomerList(query, 1, 10);

        assertNotNull(pageInfo);
        assertEquals(1, pageInfo.getList().size());
        assertEquals(1, pageInfo.getList().get(0).getId());
        verify(tCustomerMapper).selectByQuery(query);
    }

    @Test
    void getCustomerList_emptyResult_shouldReturnEmptyPageInfo() {
        CustomerQuery query = new CustomerQuery();
        when(tCustomerMapper.selectByQuery(query)).thenReturn(Collections.emptyList());

        var pageInfo = customerService.getCustomerList(query, 1, 10);

        assertNotNull(pageInfo);
        assertTrue(pageInfo.getList().isEmpty());
    }

    // ==================== getCustomerByPage ====================

    @Test
    void getCustomerByPage_shouldReturnPageInfo() {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        List<TCustomer> list = Collections.singletonList(customer);

        when(tCustomerMapper.selectByQuery(any(CustomerQuery.class))).thenReturn(list);

        var pageInfo = customerService.getCustomerByPage(1);

        assertNotNull(pageInfo);
        assertEquals(1, pageInfo.getList().size());
        verify(tCustomerMapper).selectByQuery(any(CustomerQuery.class));
    }

    @Test
    void getCustomerByPage_emptyResult_shouldReturnEmptyPageInfo() {
        when(tCustomerMapper.selectByQuery(any(CustomerQuery.class))).thenReturn(Collections.emptyList());

        var pageInfo = customerService.getCustomerByPage(1);

        assertNotNull(pageInfo);
        assertTrue(pageInfo.getList().isEmpty());
    }

    // ==================== getCustomerById ====================

    @Test
    void getCustomerById_found_shouldReturnCustomer() {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setDescription("VIP customer");
        customer.setClueId(10);

        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tCustomerMapper.selectScopedById(1, 7)).thenReturn(customer);

        TCustomer result = customerService.getCustomerById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("VIP customer", result.getDescription());
        assertEquals(10, result.getClueId());
    }

    @Test
    void getCustomerById_notFound_shouldReturnNull() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tCustomerMapper.selectScopedById(999, 7)).thenReturn(null);

        TCustomer result = customerService.getCustomerById(999);

        assertNull(result);
    }

    // ==================== convertCustomer ====================

    @Test
    void convertCustomer_success_shouldReturnTrue() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);

        when(customerManager.convertCustomer(query, 7)).thenReturn(true);

        Boolean result = customerService.convertCustomer(query, 7);

        assertTrue(result);
        verify(customerManager).convertCustomer(query, 7);
    }

    @Test
    void convertCustomer_alreadyConverted_shouldReturnFalse() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);

        when(customerManager.convertCustomer(query, 7)).thenReturn(false);

        Boolean result = customerService.convertCustomer(query, 7);

        assertFalse(result);
    }

    @Test
    void convertCustomer_alreadyConverted_throwsException() {
        CustomerQuery query = new CustomerQuery();
        query.setClueId(1);

        when(customerManager.convertCustomer(query, 7))
                .thenThrow(new RuntimeException("该线索已经转过客户，不能再转了."));

        assertThrows(RuntimeException.class, () -> customerService.convertCustomer(query, 7));
    }

    // ==================== getCustomerByExcel ====================

    @Test
    void getCustomerByExcel_shouldConvertToExcelFormat() {
        List<String> idList = Arrays.asList("1", "2");

        TClue clue = new TClue();
        clue.setFullName("John Doe");
        clue.setPhone("13800138000");
        clue.setWeixin("john_wx");
        clue.setQq("12345");
        clue.setEmail("john@example.com");
        clue.setAge(30);
        clue.setJob("Engineer");
        clue.setYearIncome(BigDecimal.valueOf(200000));
        clue.setAddress("Beijing");

        TUser owner = new TUser();
        owner.setName("Owner A");
        clue.setOwnerDO(owner);

        TActivity activity = new TActivity();
        activity.setName("Spring Sale");
        clue.setActivityDO(activity);

        TDicValue appellation = new TDicValue();
        appellation.setTypeValue("Mr.");
        clue.setAppellationDO(appellation);

        TDicValue needLoan = new TDicValue();
        needLoan.setTypeValue("Yes");
        clue.setNeedLoanDO(needLoan);

        TDicValue source = new TDicValue();
        source.setTypeValue("Web");
        clue.setSourceDO(source);

        ProductSimpleDTO product = new ProductSimpleDTO();
        product.setName("Car Model X");
        clue.setIntentionProductDO(product);

        TCustomer customer1 = new TCustomer();
        customer1.setClueDO(clue);
        customer1.setDescription("First customer");
        customer1.setNextContactTime(new Date());

        TCustomer customer2 = new TCustomer();
        TClue clue2 = new TClue();
        clue2.setFullName("Jane Smith");
        clue2.setPhone("13900139000");
        clue2.setAge(25);
        TUser owner2 = new TUser();
        owner2.setName("Owner B");
        clue2.setOwnerDO(owner2);
        clue2.setActivityDO(new TActivity());
        clue2.setAppellationDO(new TDicValue());
        clue2.setNeedLoanDO(new TDicValue());
        clue2.setSourceDO(new TDicValue());
        clue2.setIntentionProductDO(new ProductSimpleDTO());
        customer2.setClueDO(clue2);
        customer2.setDescription("Second customer");

        List<TCustomer> customers = Arrays.asList(customer1, customer2);

        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(tCustomerMapper.selectCustomerByExcel(idList, null)).thenReturn(customers);

        List<CustomerExcel> result = customerService.getCustomerByExcel(idList);

        assertNotNull(result);
        assertEquals(2, result.size());

        CustomerExcel excel1 = result.get(0);
        assertEquals("Owner A", excel1.getOwnerName());
        assertEquals("Spring Sale", excel1.getActivityName());
        assertEquals("John Doe", excel1.getFullName());
        assertEquals("Mr.", excel1.getAppellationName());
        assertEquals("13800138000", excel1.getPhone());
        assertEquals("john_wx", excel1.getWeixin());
        assertEquals("12345", excel1.getQq());
        assertEquals("john@example.com", excel1.getEmail());
        assertEquals(30, excel1.getAge());
        assertEquals("Engineer", excel1.getJob());
        assertEquals(BigDecimal.valueOf(200000), excel1.getYearIncome());
        assertEquals("Beijing", excel1.getAddress());
        assertEquals("Yes", excel1.getNeedLoanName());
        assertEquals("Car Model X", excel1.getProductName());
        assertEquals("Web", excel1.getSourceName());
        assertEquals("First customer", excel1.getDescription());

        CustomerExcel excel2 = result.get(1);
        assertEquals("Jane Smith", excel2.getFullName());
        assertEquals("13900139000", excel2.getPhone());
        assertEquals("Owner B", excel2.getOwnerName());
    }

    @Test
    void getCustomerByExcel_emptyList_shouldReturnEmptyResult() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(tCustomerMapper.selectCustomerByExcel(Collections.emptyList(), null))
                .thenReturn(Collections.emptyList());

        List<CustomerExcel> result = customerService.getCustomerByExcel(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getCustomerOptions ====================

    @Test
    void getCustomerOptions_shouldReturnOptions() {
        CustomerOption option = new CustomerOption();
        option.setCustomerId(1);
        option.setCustomerName("John Doe");
        option.setClueId(10);

        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(tCustomerMapper.selectCustomerOptions(null)).thenReturn(Collections.singletonList(option));

        List<CustomerOption> result = customerService.getCustomerOptions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getCustomerName());
    }

    @Test
    void getCustomerOptions_empty_shouldReturnEmptyList() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(tCustomerMapper.selectCustomerOptions(null)).thenReturn(Collections.emptyList());

        List<CustomerOption> result = customerService.getCustomerOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== deleteCustomer ====================

    @Test
    void deleteCustomer_success_shouldReturnTrue() {
        TCustomer customer = new TCustomer();
        customer.setId(1);

        when(tCustomerMapper.selectByPrimaryKey(1)).thenReturn(customer);
        when(tTranMapper.selectCountByCustomerId(1)).thenReturn(0);
        when(tCustomerMapper.deleteByPrimaryKey(1)).thenReturn(1);

        boolean result = customerService.deleteCustomer(1);

        assertTrue(result);
        verify(tCustomerMapper).selectByPrimaryKey(1);
        verify(tTranMapper).selectCountByCustomerId(1);
        verify(tCustomerMapper).deleteByPrimaryKey(1);
    }

    @Test
    void deleteCustomer_notFound_shouldReturnFalse() {
        when(tCustomerMapper.selectByPrimaryKey(999)).thenReturn(null);

        boolean result = customerService.deleteCustomer(999);

        assertFalse(result);
        verify(tCustomerMapper).selectByPrimaryKey(999);
        verify(tTranMapper, never()).selectCountByCustomerId(anyInt());
        verify(tCustomerMapper, never()).deleteByPrimaryKey(anyInt());
    }

    @Test
    void deleteCustomer_hasTransactions_shouldThrowException() {
        TCustomer customer = new TCustomer();
        customer.setId(1);

        when(tCustomerMapper.selectByPrimaryKey(1)).thenReturn(customer);
        when(tTranMapper.selectCountByCustomerId(1)).thenReturn(3);
        when(tTranMapper.selectActiveCountByCustomerId(1)).thenReturn(3);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> customerService.deleteCustomer(1));

        assertEquals("该客户有未完成的交易，无法删除", exception.getMessage());
        verify(tCustomerMapper).selectByPrimaryKey(1);
        verify(tTranMapper).selectCountByCustomerId(1);
        verify(tCustomerMapper, never()).deleteByPrimaryKey(anyInt());
    }

    @Test
    void deleteCustomer_deleteFails_shouldReturnFalse() {
        TCustomer customer = new TCustomer();
        customer.setId(1);

        when(tCustomerMapper.selectByPrimaryKey(1)).thenReturn(customer);
        when(tTranMapper.selectCountByCustomerId(1)).thenReturn(0);
        when(tCustomerMapper.deleteByPrimaryKey(1)).thenReturn(0);

        boolean result = customerService.deleteCustomer(1);

        assertFalse(result);
    }
}
