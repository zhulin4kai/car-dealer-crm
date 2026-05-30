package com.bjpowernode.service;

import com.bjpowernode.constant.Constants;
import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.*;
import com.bjpowernode.model.*;
import com.bjpowernode.query.TranQuery;
import com.bjpowernode.service.impl.TranServiceImpl;
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
class TranServiceImplTest {

    @InjectMocks
    private TranServiceImpl tranService;

    @Mock
    private TTranMapper tranMapper;

    @Mock
    private TTranRemarkMapper tranRemarkMapper;

    @Mock
    private TTranProductMapper tranProductMapper;

    @Mock
    private TTranInvoiceMapper tranInvoiceMapper;

    @Mock
    private TTranApproveMapper tranApproveMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private RedisManager redisManager;

    // ==================== getTransactionList ====================

    @Test
    void getTransactionList_shouldReturnPageInfo() {
        TranQuery query = new TranQuery();
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("TN20260101000001");
        List<TTran> list = Collections.singletonList(tran);

        when(tranMapper.selectByQuery(query)).thenReturn(list);

        var pageInfo = tranService.getTransactionList(query, 1, 10);

        assertNotNull(pageInfo);
        assertEquals(1, pageInfo.getList().size());
        verify(tranMapper).selectByQuery(query);
    }

    @Test
    void getTransactionList_emptyResult_shouldReturnEmptyPageInfo() {
        TranQuery query = new TranQuery();
        when(tranMapper.selectByQuery(query)).thenReturn(Collections.emptyList());

        var pageInfo = tranService.getTransactionList(query, 1, 10);

        assertNotNull(pageInfo);
        assertTrue(pageInfo.getList().isEmpty());
    }

    // ==================== getTransactionById ====================

    @Test
    void getTransactionById_found_shouldReturnTran() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("TN20260101000001");
        tran.setMoney(BigDecimal.valueOf(50000));

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);

        TTran result = tranService.getTransactionById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("TN20260101000001", result.getTranNo());
    }

    @Test
    void getTransactionById_notFound_shouldReturnNull() {
        when(tranMapper.selectByPrimaryKey(999)).thenReturn(null);

        TTran result = tranService.getTransactionById(999);

        assertNull(result);
    }

    // ==================== createTransaction ====================

    @Test
    void createTransaction_withProducts_shouldInsertTranAndProducts() {
        TTran tran = new TTran();
        tran.setCustomerId(1);
        tran.setMoney(BigDecimal.valueOf(100000));

        TTranProduct product1 = new TTranProduct();
        product1.setProductId(10);
        product1.setQuantity(2);
        product1.setPrice(BigDecimal.valueOf(50000));

        TTranProduct product2 = new TTranProduct();
        product2.setProductId(20);
        product2.setQuantity(1);
        product2.setPrice(BigDecimal.valueOf(50000));

        List<TTranProduct> products = Arrays.asList(product1, product2);

        when(tranMapper.insertSelective(any(TTran.class))).thenAnswer(invocation -> {
            TTran t = invocation.getArgument(0);
            t.setId(1);
            return 1;
        });
        when(tranProductMapper.insertSelective(any(TTranProduct.class))).thenReturn(1);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);

        Integer tranId = tranService.createTransaction(tran, products);

        assertNotNull(tranId);
        assertEquals(1, tranId);
        assertNotNull(tran.getCreateTime());
        verify(tranMapper).insertSelective(tran);
        verify(tranProductMapper, times(2)).insertSelective(any(TTranProduct.class));
        verify(productMapper).updateStock(10L, -2);
        verify(productMapper).updateStock(20L, -1);
        verify(redisManager).deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
    }

    @Test
    void createTransaction_withoutProducts_shouldInsertTranOnly() {
        TTran tran = new TTran();
        tran.setCustomerId(1);

        when(tranMapper.insertSelective(any(TTran.class))).thenAnswer(invocation -> {
            TTran t = invocation.getArgument(0);
            t.setId(2);
            return 1;
        });

        Integer tranId = tranService.createTransaction(tran, null);

        assertNotNull(tranId);
        assertEquals(2, tranId);
        verify(tranMapper).insertSelective(tran);
        verify(tranProductMapper, never()).insertSelective(any());
    }

    @Test
    void createTransaction_emptyProducts_shouldInsertTranOnly() {
        TTran tran = new TTran();
        tran.setCustomerId(1);

        when(tranMapper.insertSelective(any(TTran.class))).thenAnswer(invocation -> {
            TTran t = invocation.getArgument(0);
            t.setId(3);
            return 1;
        });

        Integer tranId = tranService.createTransaction(tran, Collections.emptyList());

        assertNotNull(tranId);
        verify(tranProductMapper, never()).insertSelective(any());
    }

    // ==================== updateTransaction ====================

    @Test
    void updateTransaction_success_shouldReturnTrue() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setMoney(BigDecimal.valueOf(80000));

        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.updateTransaction(tran);

        assertTrue(result);
        assertNotNull(tran.getEditTime());
        verify(tranMapper).updateByPrimaryKeySelective(tran);
        verify(redisManager).deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
    }

    @Test
    void updateTransaction_notFound_shouldReturnFalse() {
        TTran tran = new TTran();
        tran.setId(999);

        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(0);

        boolean result = tranService.updateTransaction(tran);

        assertFalse(result);
        verify(redisManager, never()).deletePattern(anyString());
    }

    // ==================== updateTransactionStage ====================

    @Test
    void updateTransactionStage_success_shouldReturnTrue() {
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.updateTransactionStage(1, 42);

        assertTrue(result);
        verify(tranMapper).updateByPrimaryKeySelective(argThat(t -> t.getId().equals(1) && t.getStage().equals(42)));
        verify(redisManager).deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
    }

    @Test
    void updateTransactionStage_notFound_shouldReturnFalse() {
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(0);

        boolean result = tranService.updateTransactionStage(999, 42);

        assertFalse(result);
    }

    // ==================== addTransactionRemark ====================

    @Test
    void addTransactionRemark_success_shouldReturnTrue() {
        TTranRemark remark = new TTranRemark();
        remark.setTranId(1);
        remark.setNoteContent("Follow up call");

        when(tranRemarkMapper.insert(any(TTranRemark.class))).thenReturn(1);

        boolean result = tranService.addTransactionRemark(remark);

        assertTrue(result);
        assertNotNull(remark.getCreateTime());
        verify(tranRemarkMapper).insert(remark);
        verify(redisManager).delete(Constants.CACHE_KEY_TRAN + 1);
    }

    @Test
    void addTransactionRemark_failure_shouldReturnFalse() {
        TTranRemark remark = new TTranRemark();
        remark.setTranId(1);

        when(tranRemarkMapper.insert(any(TTranRemark.class))).thenReturn(0);

        boolean result = tranService.addTransactionRemark(remark);

        assertFalse(result);
    }

    // ==================== getTransactionProducts ====================

    @Test
    void getTransactionProducts_cacheHit_shouldReturnFromCache() {
        TTranProduct cached = new TTranProduct();
        cached.setProductId(10);
        List<TTranProduct> cachedList = Collections.singletonList(cached);

        when(redisManager.get(Constants.CACHE_KEY_TRAN_PRODUCTS + 1)).thenReturn(cachedList);

        List<TTranProduct> result = tranService.getTransactionProducts(1);

        assertEquals(1, result.size());
        verify(tranProductMapper, never()).selectByTranId(anyInt());
    }

    @Test
    void getTransactionProducts_cacheMiss_shouldQueryAndCache() {
        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        List<TTranProduct> dbList = Collections.singletonList(product);

        when(redisManager.get(Constants.CACHE_KEY_TRAN_PRODUCTS + 1)).thenReturn(null);
        when(tranProductMapper.selectByTranId(1)).thenReturn(dbList);

        List<TTranProduct> result = tranService.getTransactionProducts(1);

        assertEquals(1, result.size());
        verify(tranProductMapper).selectByTranId(1);
        verify(redisManager).set(eq(Constants.CACHE_KEY_TRAN_PRODUCTS + 1), eq(dbList), eq(Constants.CACHE_EXPIRE_TIME));
    }

    // ==================== createInvoice (old method) ====================

    @Test
    void createInvoice_success_shouldReturnTrue() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);

        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(1);

        boolean result = tranService.createInvoice(invoice);

        assertTrue(result);
        assertNotNull(invoice.getCreateTime());
        verify(redisManager).delete(Constants.CACHE_KEY_TRAN_INVOICES + 1);
    }

    @Test
    void createInvoice_failure_shouldReturnFalse() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);

        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(0);

        boolean result = tranService.createInvoice(invoice);

        assertFalse(result);
    }

    // ==================== getTransactionInvoices ====================

    @Test
    void getTransactionInvoices_cacheHit_shouldReturnFromCache() {
        TTranInvoice cached = new TTranInvoice();
        cached.setId(1);
        List<TTranInvoice> cachedList = Collections.singletonList(cached);

        when(redisManager.get(Constants.CACHE_KEY_TRAN_INVOICES + 1)).thenReturn(cachedList);

        List<TTranInvoice> result = tranService.getTransactionInvoices(1);

        assertEquals(1, result.size());
        verify(tranInvoiceMapper, never()).selectByTranId(anyInt());
    }

    @Test
    void getTransactionInvoices_cacheMiss_shouldQueryAndCache() {
        when(redisManager.get(Constants.CACHE_KEY_TRAN_INVOICES + 1)).thenReturn(null);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        List<TTranInvoice> result = tranService.getTransactionInvoices(1);

        assertNotNull(result);
        verify(tranInvoiceMapper).selectByTranId(1);
    }

    // ==================== updateInvoiceStatus ====================

    @Test
    void updateInvoiceStatus_success_shouldReturnTrue() {
        TTranInvoice updatedInvoice = new TTranInvoice();
        updatedInvoice.setId(1);
        updatedInvoice.setTranId(10);

        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(updatedInvoice);

        boolean result = tranService.updateInvoiceStatus(1, "ISSUED");

        assertTrue(result);
        verify(tranInvoiceMapper).selectByPrimaryKey(1);
        verify(redisManager).delete(Constants.CACHE_KEY_TRAN_INVOICES + 10);
    }

    @Test
    void updateInvoiceStatus_failure_shouldReturnFalse() {
        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(0);

        boolean result = tranService.updateInvoiceStatus(999, "ISSUED");

        assertFalse(result);
        verify(tranInvoiceMapper, never()).selectByPrimaryKey(anyInt());
    }

    // ==================== getTransactionRemarks ====================

    @Test
    void getTransactionRemarks_shouldReturnList() {
        TTranRemark remark = new TTranRemark();
        remark.setId(1);
        remark.setNoteContent("Called customer");
        List<TTranRemark> remarks = Collections.singletonList(remark);

        when(tranRemarkMapper.selectByTranId(1)).thenReturn(remarks);

        List<TTranRemark> result = tranService.getTransactionRemarks(1);

        assertEquals(1, result.size());
        assertEquals("Called customer", result.get(0).getNoteContent());
    }

    // ==================== getTransactionProductDetails ====================

    @Test
    void getTransactionProductDetails_shouldReturnList() {
        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setProductName("Car Model A");
        List<TTranProduct> products = Collections.singletonList(product);

        when(tranMapper.selectTranProductsByTranId(1)).thenReturn(products);

        List<TTranProduct> result = tranService.getTransactionProductDetails(1);

        assertEquals(1, result.size());
        assertEquals("Car Model A", result.get(0).getProductName());
    }

    // ==================== deleteTransactionProducts ====================

    @Test
    void deleteTransactionProducts_success_shouldRestoreStockAndDelete() {
        Integer tranId = 1;
        TTranProduct product1 = new TTranProduct();
        product1.setProductId(10);
        product1.setQuantity(3);

        TTranProduct product2 = new TTranProduct();
        product2.setProductId(20);
        product2.setQuantity(5);

        List<TTranProduct> products = Arrays.asList(product1, product2);

        when(tranProductMapper.selectByTranId(tranId)).thenReturn(products);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);
        when(tranProductMapper.deleteByTranId(tranId)).thenReturn(2);

        boolean result = tranService.deleteTransactionProducts(tranId);

        assertTrue(result);
        verify(productMapper).updateStock(10L, 3);
        verify(productMapper).updateStock(20L, 5);
        verify(tranProductMapper).deleteByTranId(tranId);
        verify(redisManager).delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
    }

    @Test
    void deleteTransactionProducts_noProducts_shouldStillDeleteAndReturnTrue() {
        Integer tranId = 1;
        when(tranProductMapper.selectByTranId(tranId)).thenReturn(Collections.emptyList());
        when(tranProductMapper.deleteByTranId(tranId)).thenReturn(0);

        boolean result = tranService.deleteTransactionProducts(tranId);

        assertTrue(result);
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(tranProductMapper).deleteByTranId(tranId);
    }

    @Test
    void deleteTransactionProducts_stockRestoreFails_shouldPropagateException() {
        Integer tranId = 1;
        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(3);

        when(tranProductMapper.selectByTranId(tranId)).thenReturn(Collections.singletonList(product));
        doThrow(new RuntimeException("DB connection lost")).when(productMapper).updateStock(10L, 3);

        assertThrows(RuntimeException.class, () -> tranService.deleteTransactionProducts(tranId));
        verify(tranProductMapper, never()).deleteByTranId(tranId);
    }

    // ==================== addTransactionProducts ====================

    @Test
    void addTransactionProducts_success_shouldInsertAndReduceStock() {
        Integer tranId = 1;
        TTranProduct product1 = new TTranProduct();
        product1.setProductId(10);
        product1.setQuantity(2);

        TTranProduct product2 = new TTranProduct();
        product2.setProductId(20);
        product2.setQuantity(1);

        List<TTranProduct> products = Arrays.asList(product1, product2);

        when(tranProductMapper.insertSelective(any(TTranProduct.class))).thenReturn(1);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);

        boolean result = tranService.addTransactionProducts(tranId, products);

        assertTrue(result);
        verify(tranProductMapper, times(2)).insertSelective(any(TTranProduct.class));
        verify(productMapper).updateStock(10L, -2);
        verify(productMapper).updateStock(20L, -1);
        verify(redisManager).delete(Constants.CACHE_KEY_TRAN_PRODUCTS + tranId);
    }

    @Test
    void addTransactionProducts_emptyList_shouldReturnTrue() {
        boolean result = tranService.addTransactionProducts(1, Collections.emptyList());

        assertTrue(result);
        verify(tranProductMapper, never()).insertSelective(any());
    }

    @Test
    void addTransactionProducts_nullList_shouldReturnTrue() {
        boolean result = tranService.addTransactionProducts(1, null);

        assertTrue(result);
        verify(tranProductMapper, never()).insertSelective(any());
    }

    @Test
    void addTransactionProducts_stockUpdateFails_shouldPropagateException() {
        Integer tranId = 1;
        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(2);

        when(tranProductMapper.insertSelective(any(TTranProduct.class))).thenReturn(1);
        doThrow(new RuntimeException("DB error")).when(productMapper).updateStock(10L, -2);

        assertThrows(RuntimeException.class, () -> tranService.addTransactionProducts(tranId, Collections.singletonList(product)));
    }

    // ==================== approveTran ====================

    @Test
    void approveTran_approved_shouldSetStageTo43() {
        Integer tranId = 1;
        TTran tran = new TTran();
        tran.setId(tranId);
        tran.setStage(42); // 当前状态为42（待审批）

        when(tranMapper.selectByPrimaryKey(tranId)).thenReturn(tran);
        when(tranApproveMapper.insertSelective(any(TTranApprove.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.approveTran(tranId, true, "Approved", 1);

        assertTrue(result);
        verify(tranApproveMapper).insertSelective(argThat(a -> {
            TTranApprove approve = (TTranApprove) a;
            return approve.getTranId().equals(tranId)
                    && Boolean.TRUE.equals(approve.getApproveResult())
                    && "Approved".equals(approve.getApproveComment());
        }));
        verify(tranMapper).updateByPrimaryKeySelective(argThat(t -> {
            TTran tt = (TTran) t;
            return tt.getId().equals(tranId) && tt.getStage().equals(43);
        }));
        verify(redisManager).deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
    }

    @Test
    void approveTran_rejected_shouldSetStageTo21() {
        Integer tranId = 1;
        TTran tran = new TTran();
        tran.setId(tranId);
        tran.setStage(42); // 当前状态为42（待审批）

        when(tranMapper.selectByPrimaryKey(tranId)).thenReturn(tran);
        when(tranApproveMapper.insertSelective(any(TTranApprove.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.approveTran(tranId, false, "Rejected", 2);

        assertTrue(result);
        verify(tranMapper).updateByPrimaryKeySelective(argThat(t -> {
            TTran tt = (TTran) t;
            return tt.getId().equals(tranId) && tt.getStage().equals(21);
        }));
    }

    @Test
    void approveTran_approveInsertFails_shouldReturnFalse() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(42);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranApproveMapper.insertSelective(any(TTranApprove.class))).thenReturn(0);

        boolean result = tranService.approveTran(1, true, "Approved", 1);

        assertFalse(result);
        verify(tranMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void approveTran_tranUpdateFails_shouldReturnFalse() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(42);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranApproveMapper.insertSelective(any(TTranApprove.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(0);

        boolean result = tranService.approveTran(1, true, "Approved", 1);

        assertFalse(result);
    }

    @Test
    void approveTran_exceptionDuringApprove_shouldPropagate() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(42);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranApproveMapper.insertSelective(any(TTranApprove.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> tranService.approveTran(1, true, "Approved", 1));
    }

    // ==================== ISSUE-002: 状态流转校验测试 ====================

    @Test
    void approveTran_invalidCurrentStage_shouldThrowException() {
        // 测试：审批时当前状态必须为42（待审批）
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(41); // 当前状态为41（待报价），不是42

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tranService.approveTran(1, true, "Approved", 1));

        assertEquals("当前交易状态不允许执行此操作，需要状态: 42", exception.getMessage());
        verify(tranApproveMapper, never()).insertSelective(any());
    }

    // ==================== ISSUE-006: 库存扣减下限校验测试 ====================

    @Test
    void createTransaction_insufficientStock_shouldThrowException() {
        // 测试：库存不足时应该抛出异常
        TTran tran = new TTran();
        tran.setCustomerId(1);
        tran.setMoney(BigDecimal.valueOf(50000));

        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(5);
        product.setPrice(BigDecimal.valueOf(10000));

        when(tranMapper.insertSelective(any(TTran.class))).thenReturn(1);
        when(productMapper.updateStock(10L, -5)).thenReturn(0); // 库存不足，返回0

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tranService.createTransaction(tran, Collections.singletonList(product)));

        assertEquals("产品 [10] 库存不足，无法完成交易", exception.getMessage());
        verify(tranProductMapper).insertSelective(any(TTranProduct.class));
        verify(productMapper).updateStock(10L, -5);
    }

    @Test
    void createTransaction_sufficientStock_shouldSucceed() {
        // 测试：库存充足时应该成功
        TTran tran = new TTran();
        tran.setCustomerId(1);
        tran.setMoney(BigDecimal.valueOf(50000));

        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(5);
        product.setPrice(BigDecimal.valueOf(10000));

        when(tranMapper.insertSelective(any(TTran.class))).thenAnswer(invocation -> {
            TTran t = invocation.getArgument(0);
            t.setId(1); // 模拟MyBatis设置生成的ID
            return 1;
        });
        when(productMapper.updateStock(10L, -5)).thenReturn(1); // 库存充足，返回1

        Integer tranId = tranService.createTransaction(tran, Collections.singletonList(product));

        assertNotNull(tranId);
        assertEquals(1, tranId);
        verify(tranProductMapper).insertSelective(any(TTranProduct.class));
        verify(productMapper).updateStock(10L, -5);
    }

    @Test
    void addTransactionProducts_insufficientStock_shouldThrowException() {
        // 测试：添加产品时库存不足应该抛出异常
        Integer tranId = 1;
        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(2);

        when(tranProductMapper.insertSelective(any(TTranProduct.class))).thenReturn(1);
        when(productMapper.updateStock(10L, -2)).thenReturn(0); // 库存不足，返回0

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tranService.addTransactionProducts(tranId, Collections.singletonList(product)));

        assertEquals("产品 [10] 库存不足，无法完成交易", exception.getMessage());
        verify(tranProductMapper).insertSelective(any(TTranProduct.class));
        verify(productMapper).updateStock(10L, -2);
    }

    // ==================== ISSUE-008: 编号唯一性测试 ====================

    @Test
    void generateTranNo_shouldGenerateUniqueNumbers() {
        // 测试：生成的交易编号应该是唯一的
        // 由于是随机数，我们测试格式正确性
        TTran tran = new TTran();
        tran.setCustomerId(1);
        tran.setMoney(BigDecimal.valueOf(50000));

        when(tranMapper.insertSelective(any(TTran.class))).thenAnswer(invocation -> {
            TTran t = invocation.getArgument(0);
            t.setId(1); // 模拟MyBatis设置生成的ID
            return 1;
        });

        Integer tranId1 = tranService.createTransaction(tran, Collections.emptyList());
        Integer tranId2 = tranService.createTransaction(tran, Collections.emptyList());

        assertNotNull(tranId1);
        assertNotNull(tranId2);

        // 验证交易编号格式：TN + 日期 + 6位随机数
        verify(tranMapper, times(2)).insertSelective(argThat(t -> {
            TTran tt = (TTran) t;
            String tranNo = tt.getTranNo();
            return tranNo != null && tranNo.matches("TN\\d{8}\\d{6}");
        }));
    }

    @Test
    void generateInvoiceNo_shouldGenerateUniqueNumbers() {
        // 测试：生成的发票号码应该是唯一的
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setCreateBy(1);

        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(43);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.createTranInvoice(invoice);

        assertTrue(result);

        // 验证发票号码格式：INV + 日期 + 6位随机数
        verify(tranInvoiceMapper).insertSelective(argThat(i -> {
            TTranInvoice inv = (TTranInvoice) i;
            String invoiceNo = inv.getInvoiceNo();
            return invoiceNo != null && invoiceNo.matches("INV\\d{8}\\d{6}");
        }));
    }

    @Test
    void approveTran_validCurrentStage_shouldSucceed() {
        // 测试：审批时当前状态为42（待审批），应该成功
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(42); // 当前状态为42

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranApproveMapper.insertSelective(any(TTranApprove.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.approveTran(1, true, "Approved", 1);

        assertTrue(result);
        verify(tranApproveMapper).insertSelective(any());
    }

    @Test
    void createTranInvoice_invalidCurrentStage_shouldThrowException() {
        // 测试：创建发票时当前状态必须为43（已审批）
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setCreateBy(1);

        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(42); // 当前状态为42（待审批），不是43

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tranService.createTranInvoice(invoice));

        assertEquals("当前交易状态不允许执行此操作，需要状态: 43", exception.getMessage());
        verify(tranInvoiceMapper, never()).insertSelective(any());
    }

    @Test
    void createTranInvoice_validCurrentStage_shouldSucceed() {
        // 测试：创建发票时当前状态为43（已审批），应该成功
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setCreateBy(1);

        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(43); // 当前状态为43

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.createTranInvoice(invoice);

        assertTrue(result);
        verify(tranInvoiceMapper).insertSelective(any());
    }

    @Test
    void updateTranInvoiceStatus_invalidCurrentStage_shouldThrowException() {
        // 测试：更新发票状态为ISSUED时，当前交易状态必须为45（待收款）
        TTranInvoice currentInvoice = new TTranInvoice();
        currentInvoice.setId(1);
        currentInvoice.setTranId(10);

        TTran tran = new TTran();
        tran.setId(10);
        tran.setStage(43); // 当前状态为43（已审批），不是45

        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(currentInvoice);
        when(tranMapper.selectByPrimaryKey(10)).thenReturn(tran);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tranService.updateTranInvoiceStatus(1, "ISSUED", 1));

        assertEquals("当前交易状态不允许执行此操作，需要状态: 45", exception.getMessage());
        verify(tranMapper, never()).updateByPrimaryKeySelective(argThat(t -> {
            TTran tt = (TTran) t;
            return tt.getStage() != null && tt.getStage().equals(46);
        }));
    }

    @Test
    void updateTranInvoiceStatus_validCurrentStage_shouldSucceed() {
        // 测试：更新发票状态为ISSUED时，当前交易状态为45（待收款），应该成功
        TTranInvoice currentInvoice = new TTranInvoice();
        currentInvoice.setId(1);
        currentInvoice.setTranId(10);

        TTran tran = new TTran();
        tran.setId(10);
        tran.setStage(45); // 当前状态为45

        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(currentInvoice);
        when(tranMapper.selectByPrimaryKey(10)).thenReturn(tran);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.updateTranInvoiceStatus(1, "ISSUED", 1);

        assertTrue(result);
        verify(tranMapper).updateByPrimaryKeySelective(argThat(t -> {
            TTran tt = (TTran) t;
            return tt.getStage().equals(46);
        }));
    }

    // ==================== getTranApprove ====================

    @Test
    void getTranApprove_shouldReturnApprove() {
        TTranApprove approve = new TTranApprove();
        approve.setId(1);
        approve.setTranId(10);
        approve.setApproveResult(true);

        when(tranApproveMapper.selectByTranId(10)).thenReturn(approve);

        TTranApprove result = tranService.getTranApprove(10);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertTrue(result.getApproveResult());
    }

    @Test
    void getTranApprove_notFound_shouldReturnNull() {
        when(tranApproveMapper.selectByTranId(999)).thenReturn(null);

        TTranApprove result = tranService.getTranApprove(999);

        assertNull(result);
    }

    // ==================== createTranInvoice ====================

    @Test
    void createTranInvoice_success_shouldSetStageTo45() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setCreateBy(1);

        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(43); // 当前状态为43（已审批）

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.createTranInvoice(invoice);

        assertTrue(result);
        assertNotNull(invoice.getInvoiceNo());
        assertEquals("PENDING", invoice.getStatus());
        assertNotNull(invoice.getCreateTime());
        assertNotNull(invoice.getUpdateTime());
        verify(tranMapper).updateByPrimaryKeySelective(argThat(t -> {
            TTran tt = (TTran) t;
            return tt.getId().equals(1) && tt.getStage().equals(45);
        }));
        verify(redisManager).deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
    }

    @Test
    void createTranInvoice_insertFails_shouldReturnFalse() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);

        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(43);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(0);

        boolean result = tranService.createTranInvoice(invoice);

        assertFalse(result);
        verify(tranMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void createTranInvoice_tranUpdateFails_shouldReturnFalse() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setTranId(1);
        invoice.setCreateBy(1);

        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(43);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranInvoiceMapper.insertSelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(0);

        boolean result = tranService.createTranInvoice(invoice);

        assertFalse(result);
    }

    // ==================== getTranInvoices ====================

    @Test
    void getTranInvoices_shouldReturnList() {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(1);
        invoice.setInvoiceNo("INV20260101000001");

        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.singletonList(invoice));

        List<TTranInvoice> result = tranService.getTranInvoices(1);

        assertEquals(1, result.size());
        assertEquals("INV20260101000001", result.get(0).getInvoiceNo());
    }

    // ==================== updateTranInvoiceStatus ====================

    @Test
    void updateTranInvoiceStatus_issued_shouldSetStageTo46() {
        TTranInvoice currentInvoice = new TTranInvoice();
        currentInvoice.setId(1);
        currentInvoice.setTranId(10);

        TTran tran = new TTran();
        tran.setId(10);
        tran.setStage(45); // 当前状态为45（待收款）

        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(1);
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(currentInvoice);
        when(tranMapper.selectByPrimaryKey(10)).thenReturn(tran);
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class))).thenReturn(1);

        boolean result = tranService.updateTranInvoiceStatus(1, "ISSUED", 1);

        assertTrue(result);
        verify(tranInvoiceMapper).updateByPrimaryKeySelective(argThat(inv -> {
            TTranInvoice i = (TTranInvoice) inv;
            return "ISSUED".equals(i.getStatus()) && i.getIssueTime() != null;
        }));
        verify(tranMapper).updateByPrimaryKeySelective(argThat(t -> {
            TTran tt = (TTran) t;
            return tt.getId().equals(10) && tt.getStage().equals(46);
        }));
        verify(redisManager).deletePattern(Constants.CACHE_KEY_TRAN_LIST + "*");
    }

    @Test
    void updateTranInvoiceStatus_pending_shouldNotUpdateTranStage() {
        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(1);

        boolean result = tranService.updateTranInvoiceStatus(1, "PENDING", 1);

        assertTrue(result);
        verify(tranInvoiceMapper, never()).selectByPrimaryKey(anyInt());
        verify(tranMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void updateTranInvoiceStatus_failure_shouldReturnFalse() {
        when(tranInvoiceMapper.updateByPrimaryKeySelective(any(TTranInvoice.class))).thenReturn(0);

        boolean result = tranService.updateTranInvoiceStatus(999, "ISSUED", 1);

        assertFalse(result);
    }

    // ==================== deleteTransaction ====================

    @Test
    void deleteTransaction_success_shouldDeleteAndRestoreStock() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(41); // 待报价状态

        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(3);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.singletonList(product));
        when(productMapper.updateStock(10L, 3)).thenReturn(1);
        when(tranProductMapper.deleteByTranId(1)).thenReturn(1);
        when(tranRemarkMapper.deleteByTranId(1)).thenReturn(1);
        when(tranMapper.deleteByPrimaryKey(1)).thenReturn(1);

        boolean result = tranService.deleteTransaction(1);

        assertTrue(result);
        verify(productMapper).updateStock(10L, 3);
        verify(tranProductMapper).deleteByTranId(1);
        verify(tranRemarkMapper).deleteByTranId(1);
        verify(tranMapper).deleteByPrimaryKey(1);
        verify(redisManager).delete(Constants.CACHE_KEY_TRAN + 1);
    }

    @Test
    void deleteTransaction_invalidStage_shouldThrowException() {
        // 测试：非待报价状态的交易不能删除
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(42); // 待审批状态，不是41

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tranService.deleteTransaction(1));

        assertEquals("只有待报价状态的交易才能删除", exception.getMessage());
        verify(tranProductMapper, never()).selectByTranId(anyInt());
        verify(tranMapper, never()).deleteByPrimaryKey(anyInt());
    }

    @Test
    void deleteTransaction_notFound_shouldReturnFalse() {
        when(tranMapper.selectByPrimaryKey(999)).thenReturn(null);

        boolean result = tranService.deleteTransaction(999);

        assertFalse(result);
        verify(tranProductMapper, never()).selectByTranId(anyInt());
        verify(tranMapper, never()).deleteByPrimaryKey(anyInt());
    }

    @Test
    void deleteTransaction_noProducts_shouldDeleteWithoutStockRestore() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(41); // 待报价状态

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranRemarkMapper.deleteByTranId(1)).thenReturn(1);
        when(tranMapper.deleteByPrimaryKey(1)).thenReturn(1);

        boolean result = tranService.deleteTransaction(1);

        assertTrue(result);
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(tranProductMapper, never()).deleteByTranId(anyInt());
    }

    @Test
    void deleteTransaction_mainDeleteFails_shouldReturnFalse() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(41); // 待报价状态

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranRemarkMapper.deleteByTranId(1)).thenReturn(1);
        when(tranMapper.deleteByPrimaryKey(1)).thenReturn(0);

        boolean result = tranService.deleteTransaction(1);

        assertFalse(result);
    }

    @Test
    void deleteTransaction_stockRestoreFails_shouldPropagateException() {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setStage(41); // 待报价状态
        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(3);

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.singletonList(product));
        doThrow(new RuntimeException("DB error")).when(productMapper).updateStock(10L, 3);

        assertThrows(RuntimeException.class, () -> tranService.deleteTransaction(1));
        verify(tranMapper, never()).deleteByPrimaryKey(anyInt());
    }

    // ==================== batchDeleteTransactions ====================

    @Test
    void batchDeleteTransactions_success_shouldDeleteAll() {
        List<Integer> ids = Arrays.asList(1, 2, 3);

        when(tranProductMapper.selectByTranId(anyInt())).thenReturn(Collections.emptyList());
        when(tranRemarkMapper.deleteByTranId(anyInt())).thenReturn(1);
        when(tranMapper.deleteByIds(ids)).thenReturn(3);

        boolean result = tranService.batchDeleteTransactions(ids);

        assertTrue(result);
        verify(tranProductMapper, times(3)).selectByTranId(anyInt());
        verify(tranRemarkMapper, times(3)).deleteByTranId(anyInt());
        verify(tranMapper).deleteByIds(ids);
    }

    @Test
    void batchDeleteTransactions_withProducts_shouldRestoreStockAndDelete() {
        List<Integer> ids = Arrays.asList(1, 2);

        TTranProduct product = new TTranProduct();
        product.setProductId(10);
        product.setQuantity(2);

        when(tranProductMapper.selectByTranId(anyInt())).thenReturn(Collections.singletonList(product));
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);
        when(tranProductMapper.deleteByTranId(anyInt())).thenReturn(1);
        when(tranRemarkMapper.deleteByTranId(anyInt())).thenReturn(1);
        when(tranMapper.deleteByIds(ids)).thenReturn(2);

        boolean result = tranService.batchDeleteTransactions(ids);

        assertTrue(result);
        verify(productMapper, times(2)).updateStock(10L, 2);
        verify(tranProductMapper, times(2)).deleteByTranId(anyInt());
    }

    @Test
    void batchDeleteTransactions_emptyList_shouldReturnFalse() {
        boolean result = tranService.batchDeleteTransactions(Collections.emptyList());

        assertFalse(result);
        verify(tranMapper, never()).deleteByIds(any());
    }

    @Test
    void batchDeleteTransactions_nullList_shouldReturnFalse() {
        boolean result = tranService.batchDeleteTransactions(null);

        assertFalse(result);
        verify(tranMapper, never()).deleteByIds(any());
    }

    @Test
    void batchDeleteTransactions_mainDeleteFails_shouldReturnFalse() {
        List<Integer> ids = Collections.singletonList(1);

        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranRemarkMapper.deleteByTranId(1)).thenReturn(1);
        when(tranMapper.deleteByIds(ids)).thenReturn(0);

        boolean result = tranService.batchDeleteTransactions(ids);

        assertFalse(result);
    }
}
