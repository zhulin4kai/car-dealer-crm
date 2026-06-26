package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.dto.CustomerDetailResponse;
import com.autodealer.crm.dto.MergeCustomerRequest;
import com.autodealer.crm.dto.TransferCustomerOwnerRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.CustomerManager;
import com.autodealer.crm.mapper.TCustomerOwnerHistoryMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.dto.CustomerOption;
import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.CustomerListQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.CustomerExcel;
import com.autodealer.crm.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
    private TUserMapper tUserMapper;

    @Mock
    private TCustomerOwnerHistoryMapper tCustomerOwnerHistoryMapper;

    @Mock
    private OperationAuditRecorder auditRecorder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
    }

    // ==================== getCustomerList ====================

    @Test
    void getCustomerList_shouldReturnPageInfo() {
        CustomerListQuery query = new CustomerListQuery();
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setCustomerName("Test customer");
        customer.setOwnerId(7);
        customer.setDescription("Test customer");
        List<TCustomer> list = Collections.singletonList(customer);

        when(tCustomerMapper.selectByQuery(query)).thenReturn(list);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);

        var pageInfo = customerService.getCustomerList(query, 1, 10);

        assertNotNull(pageInfo);
        assertEquals(1, pageInfo.getList().size());
        assertEquals(1, pageInfo.getList().get(0).getId());
        verify(tCustomerMapper).selectByQuery(query);
    }

    @Test
    void getCustomerList_shouldUseCustomerSnapshotInsteadOfClueFields() {
        CustomerListQuery query = new CustomerListQuery();
        TCustomer customer = buildCustomerSnapshot();
        TClue changedClue = new TClue();
        changedClue.setFullName("线索后续改名");
        changedClue.setPhone("13900009999");
        customer.setClueDO(changedClue);

        when(tCustomerMapper.selectByQuery(query)).thenReturn(Collections.singletonList(customer));
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);

        var pageInfo = customerService.getCustomerList(query, 1, 10);

        assertEquals("客户快照姓名", pageInfo.getList().get(0).getCustomerName());
        assertEquals("13800138000", pageInfo.getList().get(0).getPhone());
        assertEquals("客户负责人", pageInfo.getList().get(0).getOwnerName());
        assertEquals("线上渠道", pageInfo.getList().get(0).getSourceName());
        assertEquals("意向", pageInfo.getList().get(0).getStateName());
    }

    @Test
    void getCustomerList_withoutSensitivePermission_shouldMaskSensitiveFields() {
        CustomerListQuery query = new CustomerListQuery();
        TCustomer customer = buildCustomerSnapshot();

        when(tCustomerMapper.selectByQuery(query)).thenReturn(Collections.singletonList(customer));
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(false);

        var pageInfo = customerService.getCustomerList(query, 1, 10);

        assertEquals("138****8000", pageInfo.getList().get(0).getPhone());
        assertEquals("wx***hot", pageInfo.getList().get(0).getWeixin());
    }

    @Test
    void getCustomerList_emptyResult_shouldReturnEmptyPageInfo() {
        CustomerListQuery query = new CustomerListQuery();
        when(tCustomerMapper.selectByQuery(query)).thenReturn(Collections.emptyList());

        var pageInfo = customerService.getCustomerList(query, 1, 10);

        assertNotNull(pageInfo);
        assertTrue(pageInfo.getList().isEmpty());
    }

    // ==================== getCustomerById ====================

    @Test
    void getCustomerById_found_shouldReturnCustomer() {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setCustomerName("VIP customer");
        customer.setOwnerId(7);
        customer.setDescription("VIP customer");
        customer.setClueId(10);

        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);
        when(tCustomerMapper.selectScopedById(1, 7)).thenReturn(customer);

        CustomerDetailResponse result = customerService.getCustomerById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("VIP customer", result.getDescription());
        assertEquals(10, result.getClueId());
    }

    @Test
    void getCustomerById_shouldUseCustomerSnapshotInsteadOfClueFields() {
        TCustomer customer = buildCustomerSnapshot();
        customer.setId(1);
        customer.setClueId(10);
        TClue changedClue = new TClue();
        changedClue.setFullName("线索后续改名");
        changedClue.setPhone("13900009999");
        customer.setClueDO(changedClue);

        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);
        when(tCustomerMapper.selectScopedById(1, 7)).thenReturn(customer);

        CustomerDetailResponse result = customerService.getCustomerById(1);

        assertNotNull(result);
        assertEquals("客户快照姓名", result.getCustomerName());
        assertEquals("13800138000", result.getPhone());
        assertEquals("wx_snapshot", result.getWeixin());
        assertEquals("客户负责人", result.getOwnerName());
        assertEquals("线上渠道", result.getSourceName());
        assertEquals("宝马X5", result.getProductName());
    }

    @Test
    void getCustomerById_notFound_shouldReturnNull() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tCustomerMapper.selectScopedById(999, 7)).thenReturn(null);

        CustomerDetailResponse result = customerService.getCustomerById(999);

        assertNull(result);
    }

    // ==================== convertCustomer ====================

    @Test
    void convertCustomer_success_shouldNotThrow() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        doNothing().when(customerManager).convertCustomer(request);

        assertDoesNotThrow(() -> customerService.convertCustomer(request));
        verify(customerManager).convertCustomer(request);
    }

    @Test
    void convertCustomer_alreadyConverted_throwsException() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        doThrow(new RuntimeException("该线索已经转过客户，不能再转了."))
                .when(customerManager).convertCustomer(request);

        assertThrows(RuntimeException.class, () -> customerService.convertCustomer(request));
    }

    // ==================== getCustomerByExcel ====================

    @Test
    void getCustomerByExcel_shouldConvertToExcelFormat() {
        List<String> idList = Arrays.asList("1", "2");

        TCustomer customer1 = buildCustomerSnapshot();
        customer1.setDescription("First customer");
        customer1.setNextContactTime(new Date());

        TCustomer customer2 = new TCustomer();
        customer2.setCustomerName("Jane Smith");
        customer2.setPhone("13900139000");
        customer2.setAge(25);
        TUser owner2 = new TUser();
        owner2.setName("Owner B");
        customer2.setOwnerDO(owner2);
        customer2.setDescription("Second customer");

        List<TCustomer> customers = Arrays.asList(customer1, customer2);

        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);
        when(tCustomerMapper.countCustomerByExcel(idList, null)).thenReturn(customers.size());
        when(tCustomerMapper.selectCustomerByExcel(idList, null, null)).thenReturn(customers);

        List<CustomerExcel> result = customerService.getCustomerByExcel(idList);

        assertNotNull(result);
        assertEquals(2, result.size());

        CustomerExcel excel1 = result.get(0);
        assertEquals("客户负责人", excel1.getOwnerName());
        assertEquals("春季车展", excel1.getActivityName());
        assertEquals("客户快照姓名", excel1.getFullName());
        assertEquals("先生", excel1.getAppellationName());
        assertEquals("13800138000", excel1.getPhone());
        assertEquals("wx_snapshot", excel1.getWeixin());
        assertEquals("123456", excel1.getQq());
        assertEquals("snapshot@example.com", excel1.getEmail());
        assertEquals(30, excel1.getAge());
        assertEquals("Engineer", excel1.getJob());
        assertEquals(BigDecimal.valueOf(200000), excel1.getYearIncome());
        assertEquals("北京市朝阳区测试路100号", excel1.getAddress());
        assertEquals("是", excel1.getNeedLoanName());
        assertEquals("宝马X5", excel1.getProductName());
        assertEquals("线上渠道", excel1.getSourceName());
        assertEquals("First customer", excel1.getDescription());

        CustomerExcel excel2 = result.get(1);
        assertEquals("Jane Smith", excel2.getFullName());
        assertEquals("13900139000", excel2.getPhone());
        assertEquals("Owner B", excel2.getOwnerName());
    }

    @Test
    void getCustomerByExcel_withoutSensitivePermission_shouldExportMaskedFields() {
        List<String> idList = Collections.singletonList("1");
        TCustomer customer = buildCustomerSnapshot();

        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(false);
        when(tCustomerMapper.countCustomerByExcel(idList, null)).thenReturn(1);
        when(tCustomerMapper.selectCustomerByExcel(idList, null, null)).thenReturn(Collections.singletonList(customer));

        List<CustomerExcel> result = customerService.getCustomerByExcel(idList);

        assertEquals("138****8000", result.get(0).getPhone());
        assertEquals("wx***hot", result.get(0).getWeixin());
        assertEquals("s***@example.com", result.get(0).getEmail());
        assertEquals("北京市朝阳区***", result.get(0).getAddress());
    }

    @Test
    void getCustomerByExcel_emptyList_shouldReturnEmptyResult() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(tCustomerMapper.countCustomerByExcel(Collections.emptyList(), null)).thenReturn(0);

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

        when(tCustomerMapper.selectScopedById(1, null)).thenReturn(customer);
        when(tCustomerMapper.countBusinessReferences(1)).thenReturn(0);
        when(tCustomerMapper.deleteScopedByPrimaryKey(1, null)).thenReturn(1);

        boolean result = customerService.deleteCustomer(1);

        assertTrue(result);
        verify(tCustomerMapper).selectScopedById(1, null);
        verify(tCustomerMapper).countBusinessReferences(1);
        verify(tCustomerMapper).deleteScopedByPrimaryKey(1, null);
    }

    @Test
    void deleteCustomer_notFound_shouldReturnFalse() {
        when(tCustomerMapper.selectScopedById(999, null)).thenReturn(null);

        boolean result = customerService.deleteCustomer(999);

        assertFalse(result);
        verify(tCustomerMapper).selectScopedById(999, null);
        verify(tCustomerMapper, never()).countBusinessReferences(anyInt());
        verify(tCustomerMapper, never()).deleteScopedByPrimaryKey(anyInt(), nullable(Integer.class));
    }

    @Test
    void deleteCustomer_inaccessibleCustomer_shouldReturnFalseBeforeReferenceCheck() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tCustomerMapper.selectScopedById(999, 7)).thenReturn(null);

        boolean result = customerService.deleteCustomer(999);

        assertFalse(result);
        verify(tCustomerMapper).selectScopedById(999, 7);
        verify(tCustomerMapper, never()).countBusinessReferences(anyInt());
        verify(tCustomerMapper, never()).deleteScopedByPrimaryKey(anyInt(), anyInt());
    }

    @Test
    void deleteCustomer_hasTransactions_shouldThrowException() {
        TCustomer customer = new TCustomer();
        customer.setId(1);

        when(tCustomerMapper.selectScopedById(1, null)).thenReturn(customer);
        when(tCustomerMapper.countBusinessReferences(1)).thenReturn(3);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerService.deleteCustomer(1));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(tCustomerMapper).selectScopedById(1, null);
        verify(tCustomerMapper, never()).deleteScopedByPrimaryKey(anyInt(), nullable(Integer.class));
    }

    @Test
    void deleteCustomer_hasCompletedTransactions_shouldAlsoBlockPhysicalDelete() {
        TCustomer customer = new TCustomer();
        customer.setId(1);

        when(tCustomerMapper.selectScopedById(1, null)).thenReturn(customer);
        when(tCustomerMapper.countBusinessReferences(1)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerService.deleteCustomer(1));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(tCustomerMapper, never()).deleteScopedByPrimaryKey(anyInt(), nullable(Integer.class));
    }

    @Test
    void deleteCustomer_deleteFails_shouldReturnFalse() {
        TCustomer customer = new TCustomer();
        customer.setId(1);

        when(tCustomerMapper.selectScopedById(1, null)).thenReturn(customer);
        when(tCustomerMapper.countBusinessReferences(1)).thenReturn(0);
        when(tCustomerMapper.deleteScopedByPrimaryKey(1, null)).thenReturn(0);

        boolean result = customerService.deleteCustomer(1);

        assertFalse(result);
    }

    @Test
    void transferCustomerOwner_success_shouldUpdateOwnerAndWriteHistory() {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setOwnerId(7);
        customer.setCustomerName("客户快照姓名");
        TUser targetOwner = new TUser();
        targetOwner.setId(8);
        targetOwner.setName("新负责人");
        targetOwner.setAccountEnabled(1);
        targetOwner.setAccountNoLocked(1);
        targetOwner.setAccountNoExpired(1);
        targetOwner.setCredentialsNoExpired(1);
        TransferCustomerOwnerRequest request = new TransferCustomerOwnerRequest();
        request.setNewOwnerId(8);
        request.setReason("主管重新分配");

        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tCustomerMapper.selectScopedById(1, null)).thenReturn(customer);
        when(tUserMapper.selectByPrimaryKey(8)).thenReturn(targetOwner);
        when(tCustomerMapper.updateOwnerAtomic(1, 7, 8, 99, null)).thenReturn(1);
        when(tCustomerOwnerHistoryMapper.insert(any(TCustomerOwnerHistory.class))).thenReturn(1);

        customerService.transferOwner(1, request);

        verify(tCustomerMapper).updateOwnerAtomic(1, 7, 8, 99, null);
        verify(tCustomerOwnerHistoryMapper).insert(argThat(history ->
                history.getCustomerId().equals(1)
                        && history.getFromOwnerId().equals(7)
                        && history.getToOwnerId().equals(8)
                        && "主管重新分配".equals(history.getReason())
                        && history.getOperatorId().equals(99)));
    }

    @Test
    void mergeCustomer_success_shouldMoveReferencesAndMarkSourceMerged() {
        TCustomer target = new TCustomer();
        target.setId(1);
        target.setOwnerId(7);
        target.setCustomerStatus("INTENTION");
        TCustomer source = new TCustomer();
        source.setId(2);
        source.setOwnerId(7);
        source.setCustomerStatus("INTENTION");
        MergeCustomerRequest request = new MergeCustomerRequest();
        request.setSourceCustomerId(2);
        request.setReason("同一手机号重复客户");

        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tCustomerMapper.selectScopedById(1, null)).thenReturn(target);
        when(tCustomerMapper.selectScopedById(2, null)).thenReturn(source);
        when(tCustomerMapper.reassignCustomerRemarks(2, 1)).thenReturn(1);
        when(tCustomerMapper.reassignTransactions(2, 1)).thenReturn(2);
        when(tCustomerMapper.reassignQuotes(2, 1)).thenReturn(3);
        when(tCustomerMapper.markMerged(2, 1, "同一手机号重复客户", 99, null)).thenReturn(1);

        customerService.mergeCustomer(1, request);

        verify(tCustomerMapper).reassignCustomerRemarks(2, 1);
        verify(tCustomerMapper).reassignTransactions(2, 1);
        verify(tCustomerMapper).reassignQuotes(2, 1);
        verify(tCustomerMapper).markMerged(2, 1, "同一手机号重复客户", 99, null);
    }

    @Test
    void mergeCustomer_scopeChangedBeforeMarkMerged_shouldThrowConflict() {
        TCustomer target = new TCustomer();
        target.setId(1);
        target.setOwnerId(7);
        target.setCustomerStatus("INTENTION");
        TCustomer source = new TCustomer();
        source.setId(2);
        source.setOwnerId(7);
        source.setCustomerStatus("INTENTION");
        MergeCustomerRequest request = new MergeCustomerRequest();
        request.setSourceCustomerId(2);
        request.setReason("同一手机号重复客户");

        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(currentUserProvider.getCurrentUserId()).thenReturn(99);
        when(tCustomerMapper.selectScopedById(1, 7)).thenReturn(target);
        when(tCustomerMapper.selectScopedById(2, 7)).thenReturn(source);
        when(tCustomerMapper.markMerged(2, 1, "同一手机号重复客户", 99, 7)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerService.mergeCustomer(1, request));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(tCustomerMapper).markMerged(2, 1, "同一手机号重复客户", 99, 7);
    }

    private TCustomer buildCustomerSnapshot() {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setClueId(10);
        customer.setCustomerName("客户快照姓名");
        customer.setPhone("13800138000");
        customer.setWeixin("wx_snapshot");
        customer.setQq("123456");
        customer.setEmail("snapshot@example.com");
        customer.setAge(30);
        customer.setJob("Engineer");
        customer.setYearIncome(BigDecimal.valueOf(200000));
        customer.setAddress("北京市朝阳区测试路100号");
        customer.setOwnerId(7);
        customer.setActivityId(3);
        customer.setAppellation(4);
        customer.setNeedLoan(21);
        customer.setIntentionState(17);
        customer.setSource(13);
        customer.setOriginalClueSource(13);
        customer.setProduct(1L);
        customer.setCustomerStatus("INTENTION");

        TUser owner = new TUser();
        owner.setName("客户负责人");
        customer.setOwnerDO(owner);
        TActivity activity = new TActivity();
        activity.setName("春季车展");
        customer.setActivityDO(activity);
        TDicValue appellation = new TDicValue();
        appellation.setTypeValue("先生");
        customer.setAppellationDO(appellation);
        TDicValue needLoan = new TDicValue();
        needLoan.setTypeValue("是");
        customer.setNeedLoanDO(needLoan);
        TDicValue intentionState = new TDicValue();
        intentionState.setTypeValue("高意向");
        customer.setIntentionStateDO(intentionState);
        TDicValue source = new TDicValue();
        source.setTypeValue("线上渠道");
        customer.setSourceDO(source);
        ProductSimpleDTO product = new ProductSimpleDTO();
        product.setName("宝马X5");
        customer.setProductDO(product);
        return customer;
    }
}
