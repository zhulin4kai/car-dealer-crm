package com.autodealer.crm.service;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.TranQuery;
import com.autodealer.crm.service.impl.TranServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

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
    private TProductMapper productMapper;
    @Mock
    private RedisManager redisManager;

    private TTran newTran(Integer id, TranStage stage) {
        TTran t = new TTran();
        t.setId(id);
        t.setMoney(BigDecimal.valueOf(100000));
        t.setStage(stage);
        t.setCreateBy(1);
        return t;
    }

    @Test
    void getTransactionList_shouldReturnPageInfo() {
        TTran tran = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByQuery(any())).thenReturn(Collections.singletonList(tran));
        var result = tranService.getTransactionList(new TranQuery(), 1, 10);
        assertEquals(1, result.getList().size());
    }

    @Test
    void getTransactionById_shouldReturnTran() {
        TTran tran = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        assertEquals(1, tranService.getTransactionById(1).getId());
    }

    @Test
    void createTransaction_shouldReturnTranId() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> { ((TTran)inv.getArgument(0)).setId(1); return 1; });
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);
        TTranProduct p = new TTranProduct();
        p.setProductId(1);
        p.setQuantity(1);
        p.setPrice(BigDecimal.TEN);
        assertEquals(1, tranService.createTransaction(tran, Collections.singletonList(p)));
    }

    @Test
    void createTransaction_stockInsufficient_shouldThrow() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> { ((TTran)inv.getArgument(0)).setId(1); return 1; });
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(0);
        TTranProduct p = new TTranProduct();
        p.setProductId(1);
        p.setQuantity(1);
        assertThrows(RuntimeException.class, () -> tranService.createTransaction(tran, Collections.singletonList(p)));
    }

    @Test
    void updateTransaction_quotation_shouldSucceed() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        assertTrue(tranService.updateTransaction(newTran(1, null)));
    }

    @Test
    void updateTransaction_pastQuotation_shouldRejectMoneyChange() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        TTran update = newTran(1, null);
        update.setMoney(BigDecimal.ONE);
        assertThrows(RuntimeException.class, () -> tranService.updateTransaction(update));
    }

    @Test
    void updateTransactionStage_shouldUseAtomicCAS() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.PENDING), eq(TranStage.QUOTATION), any())).thenReturn(1);
        assertTrue(tranService.updateTransactionStage(1, TranStage.PENDING));
    }

    @Test
    void updateTransactionStage_notFound_shouldThrow() {
        when(tranMapper.selectByPrimaryKey(999)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> tranService.updateTransactionStage(999, TranStage.PENDING));
    }

    @Test
    void approveTran_approved_shouldUseAtomicCAS() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.APPROVED), eq(TranStage.PENDING), any())).thenReturn(1);
        when(tranApproveMapper.insertSelective(any())).thenReturn(1);
        assertTrue(tranService.approveTran(1, true, "good", 1));
    }

    @Test
    void approveTran_rejected_shouldSetLost() {
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.LOST), eq(TranStage.PENDING), any())).thenReturn(1);
        when(tranApproveMapper.insertSelective(any())).thenReturn(1);
        assertTrue(tranService.approveTran(1, false, "bad", 1));
    }

    @Test
    void approveTran_wrongStage_shouldThrow() {
        when(tranMapper.updateStageAtomic(eq(1), any(), eq(TranStage.PENDING), anyInt())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> tranService.approveTran(1, true, "ok", 1));
    }

    @Test
    void createTranInvoice_shouldUseAtomicCAS() {
        TTran existing = newTran(1, TranStage.APPROVED);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.PAYMENT), eq(TranStage.APPROVED), any())).thenReturn(1);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranInvoiceMapper.insertSelective(any())).thenReturn(1);
        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(100000));
        inv.setCreateBy(1);
        assertTrue(tranService.createTranInvoice(inv));
    }

    @Test
    void createTranInvoice_wrongStage_shouldThrow() {
        when(tranMapper.updateStageAtomic(eq(1), any(), eq(TranStage.APPROVED), anyInt())).thenReturn(0);
        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(100000));
        inv.setCreateBy(1);
        assertThrows(RuntimeException.class, () -> tranService.createTranInvoice(inv));
    }

    @Test
    void createTranInvoice_amountMismatch_shouldThrow() {
        TTran existing = newTran(1, TranStage.APPROVED);
        existing.setMoney(BigDecimal.valueOf(50000));
        when(tranMapper.updateStageAtomic(anyInt(), any(), any(), any())).thenReturn(1);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(100000));
        inv.setCreateBy(1);
        assertThrows(RuntimeException.class, () -> tranService.createTranInvoice(inv));
    }

    @Test
    void updateTranInvoiceStatus_issued_shouldSetStageToPaymentThenCompleted() {
        TTranInvoice inv = new TTranInvoice();
        inv.setId(1);
        inv.setTranId(1);
        inv.setStatus("PENDING");
        when(tranInvoiceMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        assertTrue(tranService.updateTranInvoiceStatus(1, "ISSUED", 1));
    }

    @Test
    void updateTranInvoiceStatus_void_shouldRevertStage() {
        TTran existing = newTran(1, TranStage.PAYMENT);
        TTranInvoice inv = new TTranInvoice();
        inv.setId(1);
        inv.setTranId(1);
        when(tranInvoiceMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.APPROVED), eq(TranStage.PAYMENT), any())).thenReturn(1);
        assertTrue(tranService.updateTranInvoiceStatus(1, "VOID", 1));
    }

    @Test
    void deleteTransaction_quotation_shouldSucceed() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.deleteByPrimaryKey(1)).thenReturn(1);
        assertTrue(tranService.deleteTransaction(1));
        verify(tranInvoiceMapper).deleteByTranId(1);
        verify(tranApproveMapper).deleteByTranId(1);
    }

    @Test
    void deleteTransaction_nonQuotation_shouldThrow() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> tranService.deleteTransaction(1));
    }

    @Test
    void batchDeleteTransactions_shouldSkipNonQuotation() {
        TTran t1 = newTran(1, TranStage.QUOTATION);
        TTran t2 = newTran(2, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(t1);
        when(tranMapper.selectByPrimaryKey(2)).thenReturn(t2);
        when(tranMapper.deleteByIds(anyList())).thenReturn(1);
        assertTrue(tranService.batchDeleteTransactions(Arrays.asList(1, 2)));
    }

    @Test
    void batchDeleteTransactions_exceedMax_shouldThrow() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BATCH_SIZE + 1; i++) ids.add(i);
        assertThrows(RuntimeException.class, () -> tranService.batchDeleteTransactions(ids));
    }

    @Test
    void resubmitTransaction_shouldUseAtomicCAS() {
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.QUOTATION), eq(TranStage.LOST), any())).thenReturn(1);
        assertTrue(tranService.resubmitTransaction(1, 1));
        verify(tranApproveMapper).deleteByTranId(1);
    }

    @Test
    void resubmitTransaction_wrongStage_shouldThrow() {
        when(tranMapper.updateStageAtomic(eq(1), any(), eq(TranStage.LOST), anyInt())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> tranService.resubmitTransaction(1, 1));
    }

    @Test
    void updateTransactionWithProducts_quotation_shouldSucceed() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        assertTrue(tranService.updateTransactionWithProducts(newTran(1, null), null));
    }

    @Test
    void updateTransactionWithProducts_nonQuotation_shouldThrow() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> tranService.updateTransactionWithProducts(newTran(1, null), null));
    }
}
