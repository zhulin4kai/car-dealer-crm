package com.autodealer.crm.manager;

import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.ConvertCustomerRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.result.CodeEnum;
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

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(0);
        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(productMapper.selectById(5L)).thenReturn(product);

        assertDoesNotThrow(() -> customerManager.convertCustomer(request));

        verify(tClueMapper).updateStateToConverted(1, 10, 10);
        verify(tCustomerMapper).insertSelective(argThat(customer ->
                customer.getClueId().equals(1)
                        && customer.getProduct().equals(5L)
                        && customer.getOwnerId().equals(20)
                        && "张三".equals(customer.getCustomerName())
                        && "13800138000".equals(customer.getPhone())
                        && customer.getSource().equals(13)
                        && customer.getOriginalClueSource().equals(13)
                        && "测试客户".equals(customer.getDescription())));
        verify(tranService, never()).createTransaction(any(), anyList());
    }

    @Test
    void convertCustomer_shouldCopyCustomerSnapshotFromAccessibleClue() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);
        request.setProduct(5L);
        request.setDescription("客户描述");
        request.setNextContactTime(new Date());
        TProduct product = new TProduct();
        product.setId(5L);

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(0);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);

        customerManager.convertCustomer(request);

        verify(tCustomerMapper).insertSelective(argThat(customer ->
                customer.getOwnerId().equals(20)
                        && "张三".equals(customer.getCustomerName())
                        && "13800138000".equals(customer.getPhone())
                        && "wx_snapshot".equals(customer.getWeixin())
                        && customer.getActivityId().equals(3)
                        && customer.getAppellation().equals(4)
                        && customer.getNeedLoan().equals(21)
                        && customer.getIntentionState().equals(17)
                        && customer.getSource().equals(13)
                        && customer.getOriginalClueSource().equals(13)
                        && customer.getCreateBy().equals(10)));
    }

    @Test
    void convertCustomer_duplicateActiveContact_shouldRejectBeforeUpdatingClue() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerManager.convertCustomer(request));

        assertEquals(CodeEnum.DUPLICATE, exception.getCodeEnum());
        verify(tClueMapper, never()).updateStateToConverted(anyInt(), anyInt(), any());
        verify(tCustomerMapper, never()).insertSelective(any());
    }

    @Test
    void testConvertCustomerAlreadyConverted() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(0);
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

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(0);
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

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(0);
        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> customerManager.convertCustomer(request));
    }

    @Test
    void testConvertCustomerInsertFails() {
        ConvertCustomerRequest request = new ConvertCustomerRequest();
        request.setClueId(1);

        when(tClueMapper.selectScopedByPrimaryKey(1, 10)).thenReturn(buildClue());
        when(tCustomerMapper.countActiveDuplicateContacts("13800138000", "wx_snapshot", "张三", null)).thenReturn(0);
        when(tClueMapper.updateStateToConverted(1, 10, 10)).thenReturn(1);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> customerManager.convertCustomer(request));
        verify(tranService, never()).createTransaction(any(), anyList());
    }

    private TClue buildClue() {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setOwnerId(20);
        clue.setActivityId(3);
        clue.setFullName("张三");
        clue.setPhone("13800138000");
        clue.setWeixin("wx_snapshot");
        clue.setQq("123456");
        clue.setEmail("snapshot@example.com");
        clue.setAge(30);
        clue.setJob("Engineer");
        clue.setYearIncome(new java.math.BigDecimal("200000"));
        clue.setAddress("北京市朝阳区测试路100号");
        clue.setAppellation(4);
        clue.setNeedLoan(21);
        clue.setIntentionState(17);
        clue.setIntentionProduct(5);
        clue.setSource(13);
        return clue;
    }
}
