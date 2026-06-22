package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.query.TranQuery;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.service.impl.TranServiceImpl;
import com.autodealer.crm.dto.SettleRequest;
import com.autodealer.crm.dto.SettlementPreviewResponse;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock
    private TPaymentMapper paymentMapper;
    @Mock
    private TTranHistoryMapper tranHistoryMapper;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OperationAuditRecorder auditRecorder;
    @Mock
    private ProductPromotionService promotionService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        lenient().when(tranHistoryMapper.insert(any())).thenReturn(1);
        lenient().when(tranMapper.incrementVersion(anyInt(), anyInt(), anyInt())).thenReturn(1);
    }

    private TTran newTran(Integer id, TranStage stage) {
        TTran t = new TTran();
        t.setId(id);
        t.setMoney(BigDecimal.valueOf(100000));
        t.setStage(stage);
        t.setCreateBy(1);
        t.setVersion(0);
        return t;
    }

    private SettleRequest requestFromPreview(Integer tranId) {
        SettlementPreviewResponse preview = tranService.getSettlementPreview(tranId, null);
        SettleRequest request = new SettleRequest();
        request.setExpectedVersion(preview.getTransactionVersion());
        request.setPricingFingerprint(preview.getPricingFingerprint());
        return request;
    }

    private SettleRequest arbitrarySettleRequest() {
        SettleRequest request = new SettleRequest();
        request.setExpectedVersion(0);
        request.setPricingFingerprint("stale");
        return request;
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
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tranMapper.selectScopedById(1, 7)).thenReturn(tran);
        assertEquals(1, tranService.getTransactionById(1).getId());
    }

    @Test
    void createTransaction_shouldReturnTranId() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> { ((TTran)inv.getArgument(0)).setId(1); return 1; });
        // 模拟数据库商品查询 — 价格来自数据库，不接受客户端传入
        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setName("测试商品");
        dbProduct.setPrice(new BigDecimal("569800.00"));
        dbProduct.setStatus("on_sale");
        dbProduct.setStock(100);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);
        TTranProduct p = new TTranProduct();
        p.setProductId(1L);
        p.setQuantity(1);
        // 不设置 price — 服务端从数据库获取
        assertEquals(1, tranService.createTransaction(tran, Collections.singletonList(p)));
        // 验证落库价格来自数据库
        assertEquals(new BigDecimal("569800.00"), p.getPrice());
    }

    @Test
    void createTransaction_stockInsufficient_shouldThrow() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> { ((TTran)inv.getArgument(0)).setId(1); return 1; });
        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setName("测试商品");
        dbProduct.setPrice(BigDecimal.TEN);
        dbProduct.setStatus("on_sale");
        dbProduct.setStock(0);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(0);
        TTranProduct p = new TTranProduct();
        p.setProductId(1L);
        p.setQuantity(1);
        assertThrows(RuntimeException.class, () -> tranService.createTransaction(tran, Collections.singletonList(p)));
    }

    @Test
    void createTransaction_clientPriceIgnored_serverUsesDatabasePrice() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> {
            TTran inserted = inv.getArgument(0);
            assertEquals(new BigDecimal("1139600.00"), inserted.getMoney(),
                    "交易主表插入时必须已经完成服务端金额计算");
            inserted.setId(1);
            return 1;
        });
        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setName("宝马X5");
        dbProduct.setPrice(new BigDecimal("569800.00"));
        dbProduct.setStatus("on_sale");
        dbProduct.setStock(100);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);
        TTranProduct p = new TTranProduct();
        p.setProductId(1L);
        p.setQuantity(2);
        // 即使客户端尝试设置 price = 1.00，服务端也应忽略并使用数据库价格
        p.setPrice(BigDecimal.ONE);
        tranService.createTransaction(tran, Collections.singletonList(p));
        // 最终落库价格必须等于数据库价格
        assertEquals(new BigDecimal("569800.00"), p.getPrice());
        // 交易总金额 = 569800 × 2 = 1139600
        assertEquals(new BigDecimal("1139600.00"), tran.getMoney());
    }

    @Test
    void createTransaction_productNotFound_shouldThrowBusinessException() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(productMapper.selectById(999L)).thenReturn(null);
        TTranProduct p = new TTranProduct();
        p.setProductId(999L);
        p.setQuantity(1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> tranService.createTransaction(tran, Collections.singletonList(p)));
        assertEquals(CodeEnum.NOT_FOUND, ex.getCodeEnum());
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

    private TTranProduct newProduct(Integer productId, int quantity, BigDecimal price) {
        TTranProduct p = new TTranProduct();
        p.setProductId(productId.longValue());
        p.setQuantity(quantity);
        p.setPrice(price);
        return p;
    }

    @Test
    void settleTransaction_shouldUseAtomicCAS() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        // 服务端计算金额：2 × 45000 = 90000
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(newProduct(1, 2, BigDecimal.valueOf(45000))));
        when(tranMapper.settleAtomic(eq(1), any(), any(), any(), isNull(), isNull(), eq(0), eq(7))).thenReturn(1);

        SettlementPreviewResponse settled = tranService.settleTransaction(1, requestFromPreview(1));
        assertEquals(1, settled.getTransactionVersion());

        verify(tranHistoryMapper).insert(argThat(history ->
                history.getTranId().equals(1)
                        && TranStage.PENDING.name().equals(history.getStage())
                        && BigDecimal.valueOf(90000).compareTo(history.getMoney()) == 0));
    }

    @Test
    void settleTransaction_serverSideAmount_matchesProductSnapshot() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        // 多产品合计：3×100 + 2×200 = 700
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(newProduct(1, 3, BigDecimal.valueOf(100)),
                        newProduct(2, 2, BigDecimal.valueOf(200))));
        when(tranMapper.settleAtomic(eq(1), any(), any(), any(), isNull(), isNull(), eq(0), eq(7))).thenReturn(1);

        SettlementPreviewResponse settled = tranService.settleTransaction(1, requestFromPreview(1));
        assertEquals(new BigDecimal("700.00"), settled.getFinalAmount());

        verify(tranMapper).settleAtomic(eq(1), eq(new BigDecimal("700.00")), any(), eq(BigDecimal.ZERO), isNull(), isNull(), eq(0), eq(7));
    }

    @Test
    void settleTransaction_emptyProducts_shouldThrowBusinessException() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tranService.settleTransaction(1, arbitrarySettleRequest()));
        assertEquals(CodeEnum.TRAN_NO_PRODUCTS, ex.getCodeEnum());
    }

    @Test
    void settleTransaction_zeroPrice_shouldThrowBusinessException() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(newProduct(1, 1, BigDecimal.ZERO)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tranService.settleTransaction(1, arbitrarySettleRequest()));
        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
    }

    @Test
    void settleTransaction_casConflict_shouldThrowBusinessException() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(newProduct(1, 1, BigDecimal.valueOf(100))));
        // CAS 返回 0，模拟并发冲突
        when(tranMapper.settleAtomic(eq(1), any(), any(), any(), isNull(), isNull(), eq(0), eq(7))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tranService.settleTransaction(1, requestFromPreview(1)));
        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
    }

    @Test
    void settleTransaction_wrongStage_shouldThrow() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        assertThrows(BusinessException.class,
                () -> tranService.settleTransaction(1, arbitrarySettleRequest()));
    }

    @Test
    void approveTran_approved_shouldUseAtomicCAS() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.APPROVED), eq(TranStage.PENDING), any())).thenReturn(1);
        when(tranApproveMapper.insertSelective(any())).thenReturn(1);
        assertTrue(tranService.approveTran(1, true, "good"));
    }

    @Test
    void approveTran_rejected_shouldSetLost() {
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.PENDING));
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.LOST), eq(TranStage.PENDING), any())).thenReturn(1);
        when(tranApproveMapper.insertSelective(any())).thenReturn(1);
        assertTrue(tranService.approveTran(1, false, "bad"));
    }

    @Test
    void approveTran_wrongStage_shouldThrow() {
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.PENDING));
        when(tranMapper.updateStageAtomic(eq(1), any(), eq(TranStage.PENDING), anyInt())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> tranService.approveTran(1, true, "ok"));
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
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.APPROVED));
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranMapper.updateStageAtomic(1, TranStage.PAYMENT, TranStage.APPROVED, 7)).thenReturn(0);
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
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.PAYMENT));
        when(tranInvoiceMapper.updateStatusIfCurrent(eq(1), eq("PENDING"), eq("ISSUED"),
                any(Date.class), any(Date.class), eq(7))).thenReturn(1);
        assertTrue(tranService.updateTranInvoiceStatus(1, "ISSUED"));
    }

    @Test
    void updateTranInvoiceStatus_void_shouldRevertStage() {
        TTran existing = newTran(1, TranStage.PAYMENT);
        TTranInvoice inv = new TTranInvoice();
        inv.setId(1);
        inv.setTranId(1);
        inv.setStatus("ISSUED");
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        when(tranInvoiceMapper.updateStatusIfCurrent(eq(1), eq("ISSUED"), eq("VOID"),
                any(), any(Date.class), eq(7))).thenReturn(1);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.APPROVED), eq(TranStage.PAYMENT), any())).thenReturn(1);
        assertTrue(tranService.updateTranInvoiceStatus(1, "VOID"));
    }

    @Test
    void deleteTransaction_quotation_shouldSucceed() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranMapper.deleteByPrimaryKey(1)).thenReturn(1);
        assertTrue(tranService.deleteTransaction(1));
        verify(tranInvoiceMapper).deleteByTranId(1);
        verify(tranApproveMapper).deleteByTranId(1);
    }

    @Test
    void deleteTransaction_nonQuotation_shouldThrow() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> tranService.deleteTransaction(1));
    }

    @Test
    void batchDeleteTransactions_shouldFailEntireBatchForNonQuotation() {
        TTran t1 = newTran(1, TranStage.QUOTATION);
        TTran t2 = newTran(2, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(t1);
        when(tranMapper.selectByPrimaryKey(2)).thenReturn(t2);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(t1);
        when(tranMapper.selectByPrimaryKeyForUpdate(2)).thenReturn(t2);
        assertThrows(RuntimeException.class,
                () -> tranService.batchDeleteTransactions(Arrays.asList(1, 2)));
        verify(tranMapper, never()).deleteByPrimaryKey(anyInt());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(tranRemarkMapper, never()).deleteByTranId(2);
    }

    @Test
    void batchDeleteTransactions_exceedMax_shouldThrow() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BATCH_SIZE + 1; i++) ids.add(i);
        assertThrows(RuntimeException.class, () -> tranService.batchDeleteTransactions(ids));
    }

    @Test
    void resubmitTransaction_shouldUseAtomicCAS() {
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.LOST));
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.QUOTATION), eq(TranStage.LOST), any())).thenReturn(1);
        assertTrue(tranService.resubmitTransaction(1));
        verify(tranApproveMapper).deleteByTranId(1);
    }

    @Test
    void resubmitTransaction_wrongStage_shouldThrow() {
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.LOST));
        when(tranMapper.updateStageAtomic(eq(1), any(), eq(TranStage.LOST), anyInt())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> tranService.resubmitTransaction(1));
    }

    @Test
    void updateTransactionWithProducts_quotation_shouldSucceed() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        assertTrue(tranService.updateTransactionWithProducts(newTran(1, null), null));
        verify(tranProductMapper, never()).selectByTranId(anyInt());
        verify(tranProductMapper, never()).deleteByTranId(anyInt());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void updateTransactionWithProducts_emptyProducts_shouldRejectWithoutDeletingExistingProducts() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.updateTransactionWithProducts(newTran(1, null), Collections.emptyList()));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tranProductMapper, never()).deleteByTranId(anyInt());
    }

    @Test
    void updateTransactionWithProducts_moneyUpdateFailure_shouldThrow() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateByPrimaryKeySelective(any())).thenReturn(1, 0);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setName("测试商品");
        dbProduct.setPrice(BigDecimal.TEN);
        dbProduct.setStatus("on_sale");
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        when(productMapper.updateStock(1L, -1)).thenReturn(1);

        TTranProduct product = new TTranProduct();
        product.setProductId(1L);
        product.setQuantity(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.updateTransactionWithProducts(newTran(1, null), List.of(product)));
        assertEquals(CodeEnum.OPERATION_FAILED, exception.getCodeEnum());
    }

    @Test
    void updateTransactionWithProducts_nonQuotation_shouldThrow() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> tranService.updateTransactionWithProducts(newTran(1, null), null));
    }

    @Test
    void recordPayment_completedAmount_shouldCompleteTransaction() {
        TTran tran = newTran(1, TranStage.PAYMENT);
        TPayment payment = new TPayment();
        payment.setTranId(1);
        payment.setAmount(tran.getMoney());
        payment.setPaymentMethod("CASH");
        payment.setPaymentType("FULL");

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.insertSelective(payment)).thenReturn(1);
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranMapper.updateStageToCompleted(1, 7)).thenReturn(1);

        TPayment result = tranService.recordPayment(payment);

        assertEquals("COMPLETED", result.getPaymentStatus());
        verify(tranMapper).updateStageToCompleted(1, 7);
        verify(tranHistoryMapper).insert(argThat(history ->
                history.getTranId().equals(1) && "COMPLETED".equals(history.getStage())));
    }

    @Test
    void refundPayment_shouldAtomicallyMarkPaymentBeforeSideEffects() {
        TPayment original = new TPayment();
        original.setId(10);
        original.setTranId(1);
        original.setAmount(BigDecimal.valueOf(100000));
        original.setPaymentStatus("COMPLETED");
        original.setPaymentType("FULL");

        when(paymentMapper.selectByPrimaryKey(10)).thenReturn(original);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.COMPLETED));
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.singletonList(original));
        when(tranMapper.updateStageAtomic(1, TranStage.CANCELLED, TranStage.COMPLETED, 7)).thenReturn(1);
        when(paymentMapper.markRefundedIfCompleted(eq(10), any(Date.class), eq(7))).thenReturn(1);
        when(paymentMapper.insertSelective(any(TPayment.class))).thenReturn(1);
        when(tranProductMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        TPayment refund = tranService.refundPayment(10);

        assertEquals(BigDecimal.valueOf(-100000), refund.getAmount());
        assertEquals("REFUND", refund.getPaymentType());
        verify(paymentMapper).markRefundedIfCompleted(eq(10), any(Date.class), eq(7));
        verify(paymentMapper).insertSelective(refund);
    }

    @Test
    void refundPayment_concurrentRefund_shouldStopBeforeSideEffects() {
        TPayment original = new TPayment();
        original.setId(10);
        original.setTranId(1);
        original.setAmount(BigDecimal.valueOf(100000));
        original.setPaymentStatus("COMPLETED");
        original.setPaymentType("FULL");

        when(paymentMapper.selectByPrimaryKey(10)).thenReturn(original);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.COMPLETED));
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.singletonList(original));
        when(tranMapper.updateStageAtomic(1, TranStage.CANCELLED, TranStage.COMPLETED, 7)).thenReturn(0);

        assertThrows(RuntimeException.class, () -> tranService.refundPayment(10));

        verify(paymentMapper, never()).insertSelective(any());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(paymentMapper, never()).markRefundedIfCompleted(anyInt(), any(), anyInt());
    }
}
