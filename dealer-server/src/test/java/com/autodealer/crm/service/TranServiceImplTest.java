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
import java.time.LocalDateTime;
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
    private TCustomerMapper customerMapper;
    @Mock
    private RedisManager redisManager;
    @Mock
    private TPaymentMapper paymentMapper;
    @Mock
    private TRefundRequestMapper refundRequestMapper;
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
        lenient().when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.all());
        TCustomer accessibleCustomer = new TCustomer();
        accessibleCustomer.setId(10);
        lenient().when(customerMapper.selectScopedById(eq(10), nullable(Integer.class)))
                .thenReturn(accessibleCustomer);
        lenient().when(tranHistoryMapper.insert(any())).thenReturn(1);
        lenient().when(tranMapper.incrementVersion(anyInt(), anyInt(), anyInt())).thenReturn(1);
    }

    private TTran newTran(Integer id, TranStage stage) {
        TTran t = new TTran();
        t.setId(id);
        t.setMoney(BigDecimal.valueOf(100000));
        t.setStage(stage);
        t.setCustomerId(10);
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
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(tranMapper.selectScopedById(eq(1), eq(7), eq(false), anyList())).thenReturn(tran);
        assertEquals(1, tranService.getTransactionById(1).getId());
    }

    @Test
    void getTransactionById_nonAdminUsesSelfDataScopeOnly() {
        TTran tran = newTran(1, TranStage.PENDING);
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(tranMapper.selectScopedById(eq(1), eq(7), eq(false), anyList())).thenReturn(tran);

        assertEquals(1, tranService.getTransactionById(1).getId());

        verify(tranMapper).selectScopedById(eq(1), eq(7), eq(false), anyList());
    }

    @Test
    void createTransaction_shouldReturnTranId() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> { ((TTran)inv.getArgument(0)).setId(1); return 1; });
        // 模拟数据库商品查询 — 价格来自数据库，不接受客户端传入
        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setSku("SKU-001");
        dbProduct.setName("测试商品");
        dbProduct.setSpecification("2026款");
        dbProduct.setPrice(new BigDecimal("569800.00"));
        dbProduct.setStatus("ON_SALE");
        dbProduct.setStock(100);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        TTranProduct p = new TTranProduct();
        p.setProductId(1L);
        p.setQuantity(1);
        // 不设置 price — 服务端从数据库获取
        assertEquals(1, tranService.createTransaction(tran, Collections.singletonList(p)));
        // 验证落库价格来自数据库
        assertEquals(new BigDecimal("569800.00"), p.getPrice());
        assertEquals("SKU-001", p.getProductSku());
        assertEquals("测试商品", p.getProductName());
        assertEquals("2026款", p.getProductSpecification());
        assertEquals(new BigDecimal("569800.00"), p.getGuidePrice());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void createTransaction_inaccessibleCustomer_shouldRejectBeforeAnyWrite() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        tran.setCustomerId(99);
        when(customerMapper.selectScopedById(99, null)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tranService.createTransaction(tran, Collections.emptyList()));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(tranMapper, never()).insertSelective(any());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(auditRecorder, never()).record(any(), anyString());
    }

    @Test
    void createTransaction_quotationDoesNotRequireStockDeduction() {
        TTran tran = newTran(null, TranStage.QUOTATION);
        when(tranMapper.insertSelective(any())).thenAnswer(inv -> { ((TTran)inv.getArgument(0)).setId(1); return 1; });
        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setName("测试商品");
        dbProduct.setPrice(BigDecimal.TEN);
        dbProduct.setStatus("ON_SALE");
        dbProduct.setStock(0);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        TTranProduct p = new TTranProduct();
        p.setProductId(1L);
        p.setQuantity(1);

        assertEquals(1, tranService.createTransaction(tran, Collections.singletonList(p)));
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
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
        dbProduct.setStatus("ON_SALE");
        dbProduct.setStock(100);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
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
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
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
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class)))
                .thenReturn(1);
        assertTrue(tranService.updateTransaction(newTran(1, null)));
    }

    @Test
    void updateTransaction_limitedScope_shouldPassScopeToFinalUpdate() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(tranMapper.selectScopedById(eq(1), eq(7), eq(false), anyList())).thenReturn(existing);
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), eq(false), eq(7)))
                .thenReturn(1);

        assertTrue(tranService.updateTransaction(newTran(1, null)));

        verify(tranMapper).updateScopedByPrimaryKeySelective(any(TTran.class), eq(false), eq(7));
    }

    @Test
    void updateTransactionWithProducts_rebindingToInaccessibleCustomer_shouldRejectBeforeUpdate() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        existing.setCustomerId(10);
        TTran update = newTran(1, null);
        update.setCustomerId(99);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(customerMapper.selectScopedById(99, null)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tranService.updateTransactionWithProducts(update, null));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(tranMapper, never()).updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class));
    }

    @Test
    void updateTransactionWithProducts_shouldIgnoreClientOwnedFields() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        TTran update = newTran(1, null);
        update.setTranNo("CLIENT-NO");
        update.setCreateBy(999);
        update.setCreateTime(new Date(0));
        update.setMoney(BigDecimal.ONE);
        update.setStage(TranStage.PAYMENT);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class)))
                .thenReturn(1);

        assertTrue(tranService.updateTransactionWithProducts(update, null));

        verify(tranMapper).updateScopedByPrimaryKeySelective(argThat(argument ->
                argument.getTranNo() == null
                        && argument.getCreateBy() == null
                        && argument.getCreateTime() == null
                        && argument.getMoney() == null
                        && argument.getStage() == null
                        && Integer.valueOf(7).equals(argument.getEditBy())),
                eq(true), isNull());
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

    private TProductPromotion newPromotion(Long id, Long productId, String type, BigDecimal discount, String status) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(id);
        promotion.setProductId(productId);
        promotion.setName("结算促销");
        promotion.setType(type);
        promotion.setDiscount(discount);
        promotion.setStatus(status);
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(1));
        promotion.setUpdateTime(LocalDateTime.now());
        return promotion;
    }

    @Test
    void getAvailablePromotions_shouldUseQuotationProductIds() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(
                        newProduct(1, 1, BigDecimal.TEN),
                        newProduct(1, 2, BigDecimal.TEN),
                        newProduct(2, 1, BigDecimal.TEN)));
        List<TProductPromotion> promotions = List.of(
                newPromotion(10L, 1L, "AMOUNT", BigDecimal.TEN, "进行中"));
        when(promotionService.getAvailablePromotions(List.of(1L, 2L))).thenReturn(promotions);

        assertEquals(promotions, tranService.getAvailablePromotions(1));

        verify(promotionService).getAvailablePromotions(List.of(1L, 2L));
    }

    @Test
    void getAvailablePromotions_nonQuotation_shouldThrowBusinessException() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.getAvailablePromotions(1));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(promotionService, never()).getAvailablePromotions(anyList());
    }

    @Test
    void getSettlementPreview_percentagePromotion_shouldDiscountMatchedProductOnly() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(
                        newProduct(1, 2, BigDecimal.valueOf(100)),
                        newProduct(2, 1, BigDecimal.valueOf(50))));
        when(promotionService.getPromotionById(10L)).thenReturn(
                newPromotion(10L, 1L, "PERCENTAGE", new BigDecimal("0.90"), "ACTIVE"));

        SettlementPreviewResponse preview = tranService.getSettlementPreview(1, 10L);

        assertEquals(new BigDecimal("250.00"), preview.getOriginalAmount());
        assertEquals(new BigDecimal("20.00"), preview.getDiscountAmount());
        assertEquals(new BigDecimal("230.00"), preview.getFinalAmount());
        assertNotNull(preview.getPromotion());
        assertEquals(1L, preview.getPromotion().getProductId());
    }

    @Test
    void getSettlementPreview_amountPromotion_shouldCapDiscountAtMatchedAmount() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranProductMapper.selectByTranId(1)).thenReturn(
                List.of(
                        newProduct(1, 1, BigDecimal.valueOf(100)),
                        newProduct(2, 1, BigDecimal.valueOf(50))));
        when(promotionService.getPromotionById(10L)).thenReturn(
                newPromotion(10L, 1L, "AMOUNT", BigDecimal.valueOf(150), "进行中"));

        SettlementPreviewResponse preview = tranService.getSettlementPreview(1, 10L);

        assertEquals(new BigDecimal("150.00"), preview.getOriginalAmount());
        assertEquals(new BigDecimal("100.00"), preview.getDiscountAmount());
        assertEquals(new BigDecimal("50.00"), preview.getFinalAmount());
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
    void approveTran_creatorShouldBeRejectedBeforeStageUpdate() {
        TTran selfCreated = newTran(1, TranStage.PENDING);
        selfCreated.setCreateBy(7);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(selfCreated);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.approveTran(1, true, "ok"));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
        verify(tranApproveMapper, never()).insertSelective(any());
    }

    @Test
    void createTranInvoice_shouldKeepApprovedStage() {
        TTran existing = newTran(1, TranStage.APPROVED);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.insertSelective(any())).thenReturn(1);
        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(100000));
        inv.setCreateBy(1);
        assertTrue(tranService.createTranInvoice(inv));
        verify(tranMapper, never()).updateStageAtomic(eq(1), eq(TranStage.PAYMENT), eq(TranStage.APPROVED), any());
    }

    @Test
    void createTranInvoice_nonAdminScopeDoesNotExpandByFinanceStage() {
        TTran existing = newTran(1, TranStage.APPROVED);
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(tranMapper.selectScopedById(eq(1), eq(7), eq(false), eq(List.of()))).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        when(tranInvoiceMapper.insertSelective(any())).thenReturn(1);

        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(100000));

        assertTrue(tranService.createTranInvoice(inv));
        verify(tranMapper).selectScopedById(eq(1), eq(7), eq(false), eq(List.of()));
        verify(tranMapper, never()).updateStageAtomic(eq(1), eq(TranStage.PAYMENT), eq(TranStage.APPROVED), any());
    }

    @Test
    void createTranInvoice_wrongStage_shouldThrow() {
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.QUOTATION));
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(newTran(1, TranStage.QUOTATION));
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(Collections.emptyList());
        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(100000));
        inv.setCreateBy(1);
        assertThrows(RuntimeException.class, () -> tranService.createTranInvoice(inv));
        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
    }

    @Test
    void createTranInvoice_partialAndMultipleInvoices_shouldUseAvailableBalance() {
        TTran existing = newTran(1, TranStage.APPROVED);
        existing.setMoney(BigDecimal.valueOf(100000));
        TTranInvoice first = new TTranInvoice();
        first.setAmount(BigDecimal.valueOf(40000));
        first.setStatus("ISSUED");
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(List.of(first));
        when(tranInvoiceMapper.insertSelective(any())).thenReturn(1);

        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(60000));

        assertTrue(tranService.createTranInvoice(inv));
        verify(tranInvoiceMapper).insertSelective(any());
    }

    @Test
    void createTranInvoice_pendingInvoice_shouldConsumeAvailableBalance() {
        TTran existing = newTran(1, TranStage.APPROVED);
        existing.setMoney(BigDecimal.valueOf(100000));
        TTranInvoice pending = new TTranInvoice();
        pending.setAmount(BigDecimal.valueOf(80000));
        pending.setStatus("PENDING");
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(List.of(pending));

        TTranInvoice inv = new TTranInvoice();
        inv.setTranId(1);
        inv.setAmount(BigDecimal.valueOf(30000));

        BusinessException exception = assertThrows(BusinessException.class, () -> tranService.createTranInvoice(inv));
        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tranInvoiceMapper, never()).insertSelective(any());
    }

    @Test
    void updateTranInvoiceStatus_issued_shouldNotChangeTransactionStage() {
        TTranInvoice inv = new TTranInvoice();
        inv.setId(1);
        inv.setTranId(1);
        inv.setStatus("PENDING");
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.APPROVED));
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(newTran(1, TranStage.APPROVED));
        when(tranInvoiceMapper.updateStatusIfCurrent(eq(1), eq("PENDING"), eq("ISSUED"),
                any(Date.class), isNull(), any(Date.class), eq(7))).thenReturn(1);

        assertTrue(tranService.updateTranInvoiceStatus(1, "ISSUED", null));

        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
        verify(tranHistoryMapper, never()).insert(argThat(history ->
                history.getTranId().equals(1) && "PAYMENT".equals(history.getStage())));
    }

    @Test
    void updateTranInvoiceStatus_void_shouldNotChangeTransactionStage() {
        TTran existing = newTran(1, TranStage.PAYMENT);
        TTranInvoice inv = new TTranInvoice();
        inv.setId(1);
        inv.setTranId(1);
        inv.setStatus("ISSUED");
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        when(tranInvoiceMapper.updateStatusIfCurrent(eq(1), eq("ISSUED"), eq("VOIDED"),
                any(), eq("作废原因"), any(Date.class), eq(7))).thenReturn(1);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);

        assertTrue(tranService.updateTranInvoiceStatus(1, "VOIDED", "作废原因"));

        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
        verify(tranHistoryMapper, never()).insert(argThat(history ->
                history.getTranId().equals(1) && "APPROVED".equals(history.getStage())));
    }

    @Test
    void updateTranInvoiceStatus_voidWithoutReason_shouldReject() {
        TTranInvoice inv = new TTranInvoice();
        inv.setId(1);
        inv.setTranId(1);
        inv.setStatus("ISSUED");
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(inv);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.PAYMENT));
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(newTran(1, TranStage.PAYMENT));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.updateTranInvoiceStatus(1, "VOIDED", " "));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tranInvoiceMapper, never()).updateStatusIfCurrent(anyInt(), anyString(), anyString(), any(), any(), any(), anyInt());
    }

    @Test
    void redReverseInvoice_shouldCreateNegativeInvoiceAndKeepOriginal() {
        TTran existing = newTran(1, TranStage.PAYMENT);
        TTranInvoice original = new TTranInvoice();
        original.setId(1);
        original.setTranId(1);
        original.setType("VAT_NORMAL");
        original.setTitle("深圳测试公司");
        original.setTaxNumber("91440300123456789X");
        original.setAmount(BigDecimal.valueOf(100000));
        original.setStatus("ISSUED");
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(original);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(List.of(original));
        when(tranInvoiceMapper.insertSelective(any())).thenReturn(1);
        when(tranInvoiceMapper.updateStatusIfCurrent(eq(1), eq("ISSUED"), eq("PARTIAL_RED_REVERSED"),
                any(), eq("红冲原因：客户退票"), any(Date.class), eq(7))).thenReturn(1);

        TTranInvoice redInvoice = tranService.redReverseInvoice(1, BigDecimal.valueOf(30000), "客户退票");

        assertEquals(1, redInvoice.getOriginalInvoiceId());
        assertEquals(new BigDecimal("-30000.00"), redInvoice.getAmount());
        assertEquals("RED_REVERSED", redInvoice.getStatus());
        verify(tranInvoiceMapper).updateStatusIfCurrent(eq(1), eq("ISSUED"), eq("PARTIAL_RED_REVERSED"),
                any(), eq("红冲原因：客户退票"), any(Date.class), eq(7));
        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
    }

    @Test
    void reissueInvoice_shouldCreateLinkedPendingInvoice() {
        TTran existing = newTran(1, TranStage.PAYMENT);
        TTranInvoice source = new TTranInvoice();
        source.setId(1);
        source.setTranId(1);
        source.setType("VAT_SPECIAL");
        source.setTitle("原发票抬头");
        source.setTaxNumber("91440300123456789X");
        source.setAmount(BigDecimal.valueOf(50000));
        source.setStatus("VOIDED");
        when(tranInvoiceMapper.selectByPrimaryKey(1)).thenReturn(source);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(List.of(source));
        when(tranInvoiceMapper.insertSelective(any())).thenReturn(1);

        TTranInvoice request = new TTranInvoice();
        request.setAmount(BigDecimal.valueOf(50000));
        TTranInvoice reissued = tranService.reissueInvoice(1, request, "抬头修正");

        assertEquals(1, reissued.getOriginalInvoiceId());
        assertEquals("PENDING", reissued.getStatus());
        assertEquals("原发票抬头", reissued.getTitle());
        assertEquals(new BigDecimal("50000.00"), reissued.getAmount());
        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
    }

    @Test
    void getTranInvoices_withoutSensitivePermission_shouldMaskTaxFields() {
        TTran existing = newTran(1, TranStage.PAYMENT);
        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(1);
        invoice.setTranId(1);
        invoice.setTitle("深圳市远景建筑设计有限公司");
        invoice.setTaxNumber("91440300MA5F8X2K7R");
        invoice.setBankAccount("755900010012345");
        invoice.setAddress("深圳市南山区科苑路88号");
        invoice.setPhone("0755-86661234");
        invoice.setAmount(BigDecimal.valueOf(455800));
        invoice.setStatus("ISSUED");
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(List.of(invoice));

        List<TTranInvoice> invoices = tranService.getTranInvoices(1);

        assertEquals(1, invoices.size());
        assertNotEquals("91440300MA5F8X2K7R", invoices.get(0).getTaxNumber());
        assertTrue(invoices.get(0).getTaxNumber().contains("****"));
        assertTrue(invoices.get(0).getBankAccount().contains("****"));
        assertTrue(invoices.get(0).getPhone().contains("****"));
    }

    @Test
    void deleteTransaction_shouldRejectPhysicalDelete() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.deleteTransaction(1));
        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(tranMapper, never()).deleteByPrimaryKey(anyInt());
    }

    @Test
    void cancelTransaction_shouldWriteHistoryAndKeepFacts() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.CANCELLED), eq(TranStage.QUOTATION), eq(7)))
                .thenReturn(1);

        assertTrue(tranService.cancelTransaction(1, "客户取消"));

        verify(tranMapper).updateStageAtomic(1, TranStage.CANCELLED, TranStage.QUOTATION, 7);
        verify(tranHistoryMapper).insert(argThat(history ->
                history.getTranId().equals(1)
                        && "CANCELLED".equals(history.getStage())
                        && "客户取消".equals(history.getReason())));
        verify(tranInvoiceMapper, never()).deleteByTranId(anyInt());
        verify(tranApproveMapper, never()).deleteByTranId(anyInt());
    }

    @Test
    void cancelTransaction_withIssuedInvoice_shouldReject() {
        TTran existing = newTran(1, TranStage.PENDING);
        TTranInvoice invoice = new TTranInvoice();
        invoice.setStatus("ISSUED");
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranInvoiceMapper.selectByTranId(1)).thenReturn(List.of(invoice));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.cancelTransaction(1, "客户取消"));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
    }

    @Test
    void closeTransaction_shouldWriteClosedHistory() {
        TTran existing = newTran(1, TranStage.LOST);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(existing);
        when(tranMapper.updateStageAtomic(eq(1), eq(TranStage.CLOSED), eq(TranStage.LOST), eq(7)))
                .thenReturn(1);

        assertTrue(tranService.closeTransaction(1, "长期未成交"));

        verify(tranHistoryMapper).insert(argThat(history ->
                "CLOSED".equals(history.getStage())
                        && "长期未成交".equals(history.getReason())));
    }

    @Test
    void deleteTransactionProducts_quotationShouldNotRestoreStock() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);

        assertTrue(tranService.deleteTransactionProducts(1));

        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(tranProductMapper).deleteByTranId(1);
    }

    @Test
    void batchDeleteTransactions_shouldFailEntireBatchForNonQuotation() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.batchDeleteTransactions(Arrays.asList(1, 2)));
        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
        verify(tranMapper, never()).deleteByPrimaryKey(anyInt());
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
        verify(tranApproveMapper, never()).deleteByTranId(1);
        verify(tranHistoryMapper).insert(argThat(history ->
                history.getTranId().equals(1) && "QUOTATION".equals(history.getStage())));
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
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class)))
                .thenReturn(1);
        assertTrue(tranService.updateTransactionWithProducts(newTran(1, null), null));
        verify(tranProductMapper, never()).selectByTranId(anyInt());
        verify(tranProductMapper, never()).deleteByTranId(anyInt());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void addTransactionProducts_quotationShouldNotDeductStock() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setSku("SKU-ADD");
        dbProduct.setName("测试商品");
        dbProduct.setSpecification("增配版");
        dbProduct.setPrice(BigDecimal.TEN);
        dbProduct.setStatus("ON_SALE");
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class)))
                .thenReturn(1);
        TTranProduct product = new TTranProduct();
        product.setProductId(1L);
        product.setQuantity(2);

        assertTrue(tranService.addTransactionProducts(1, List.of(product)));

        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        assertEquals(new BigDecimal("10"), product.getPrice());
        assertEquals("SKU-ADD", product.getProductSku());
        assertEquals("测试商品", product.getProductName());
        assertEquals("增配版", product.getProductSpecification());
        assertEquals(BigDecimal.TEN, product.getGuidePrice());
    }

    @Test
    void updateTransactionWithProducts_emptyProducts_shouldRejectWithoutDeletingExistingProducts() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class)))
                .thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.updateTransactionWithProducts(newTran(1, null), Collections.emptyList()));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(tranProductMapper, never()).deleteByTranId(anyInt());
    }

    @Test
    void updateTransactionWithProducts_moneyUpdateFailure_shouldThrow() {
        TTran existing = newTran(1, TranStage.QUOTATION);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        when(tranMapper.updateScopedByPrimaryKeySelective(any(TTran.class), anyBoolean(), nullable(Integer.class)))
                .thenReturn(1, 0);

        TProduct dbProduct = new TProduct();
        dbProduct.setId(1L);
        dbProduct.setName("测试商品");
        dbProduct.setPrice(BigDecimal.TEN);
        dbProduct.setStatus("ON_SALE");
        when(productMapper.selectById(1L)).thenReturn(dbProduct);
        when(tranProductMapper.insertSelective(any())).thenReturn(1);

        TTranProduct product = new TTranProduct();
        product.setProductId(1L);
        product.setQuantity(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.updateTransactionWithProducts(newTran(1, null), List.of(product)));
        assertEquals(CodeEnum.OPERATION_FAILED, exception.getCodeEnum());
        assertEquals("交易金额更新失败", exception.getMessage());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void updateTransactionWithProducts_nonQuotation_shouldThrow() {
        TTran existing = newTran(1, TranStage.PENDING);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> tranService.updateTransactionWithProducts(newTran(1, null), null));
    }

    @Test
    void recordPayment_shouldCreatePendingPaymentWithServerCalculatedAmount() {
        TTran tran = newTran(1, TranStage.PAYMENT);
        TPayment payment = new TPayment();
        payment.setTranId(1);
        payment.setAmount(BigDecimal.ONE);
        payment.setPaymentMethod("CASH");

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.insertSelective(payment)).thenReturn(1);
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        TPayment result = tranService.recordPayment(payment);

        assertEquals("PENDING", result.getPaymentStatus());
        assertEquals(tran.getMoney().setScale(2), result.getAmount());
        assertEquals("FULL", result.getPaymentType());
        verify(tranMapper, never()).updateStageAtomic(eq(1), eq(TranStage.COMPLETED), any(), anyInt());
        verify(tranHistoryMapper, never()).insert(argThat(history ->
                history.getTranId().equals(1) && "COMPLETED".equals(history.getStage())));
    }

    @Test
    void recordPayment_approvedTransaction_shouldCreatePendingPayment() {
        TTran tran = newTran(1, TranStage.APPROVED);
        TPayment payment = new TPayment();
        payment.setTranId(1);
        payment.setPaymentMethod("CASH");

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.insertSelective(payment)).thenReturn(1);
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.emptyList());

        TPayment result = tranService.recordPayment(payment);

        assertEquals("PENDING", result.getPaymentStatus());
        assertEquals(tran.getMoney().setScale(2), result.getAmount());
        verify(tranMapper, never()).updateStageAtomic(anyInt(), any(), any(), anyInt());
    }

    @Test
    void recordPayment_sameTransactionRefDifferentChannel_shouldConflict() {
        TTran tran = newTran(1, TranStage.PAYMENT);
        TPayment existing = new TPayment();
        existing.setId(10);
        existing.setTranId(1);
        existing.setAmount(tran.getMoney().setScale(2));
        existing.setPaymentMethod("BANK_TRANSFER");
        existing.setPaymentType("FULL");
        existing.setPaymentStatus("PENDING");
        existing.setTransactionRef("PAY-REF-001");

        TPayment payment = new TPayment();
        payment.setTranId(1);
        payment.setPaymentMethod("WECHAT");
        payment.setTransactionRef("PAY-REF-001");

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.selectByTransactionRef("PAY-REF-001")).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.recordPayment(payment));

        assertEquals(CodeEnum.DUPLICATE, exception.getCodeEnum());
        verify(paymentMapper, never()).insertSelective(any());
    }

    @Test
    void recordPayment_manualDuplicate_shouldReturnExistingPayment() {
        TTran tran = newTran(1, TranStage.PAYMENT);
        TPayment existing = new TPayment();
        existing.setId(10);
        existing.setTranId(1);
        existing.setAmount(tran.getMoney().setScale(2));
        existing.setPaymentMethod("CASH");
        existing.setPaymentType("FULL");
        existing.setPaymentStatus("PENDING");
        existing.setIdempotencyKey("PAYMENT:MANUAL:1:CASH:100000.00");

        TPayment payment = new TPayment();
        payment.setTranId(1);
        payment.setPaymentMethod("CASH");

        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.selectByTranId(1)).thenReturn(List.of(existing));

        TPayment result = tranService.recordPayment(payment);

        assertSame(existing, result);
        verify(paymentMapper, never()).insertSelective(any());
    }

    @Test
    void confirmPayment_fullAmount_shouldMoveTransactionToDeliveryOnly() {
        TTran tran = newTran(1, TranStage.PAYMENT);
        TPayment payment = new TPayment();
        payment.setId(10);
        payment.setTranId(1);
        payment.setAmount(tran.getMoney());
        payment.setPaymentStatus("PENDING");
        payment.setPaymentType("FULL");

        when(paymentMapper.selectByPrimaryKeyForUpdate(10)).thenReturn(payment);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.updateStatusIfCurrent(eq(10), eq("PENDING"), eq("COMPLETED"),
                any(Date.class), any(), any(Date.class), eq(7))).thenReturn(1);
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.singletonList(payment));
        when(tranMapper.updateStageAtomic(1, TranStage.DELIVERY, TranStage.PAYMENT, 7)).thenReturn(1);

        TPayment result = tranService.confirmPayment(10, true, "到账");

        assertEquals("COMPLETED", result.getPaymentStatus());
        verify(tranMapper).updateStageAtomic(1, TranStage.DELIVERY, TranStage.PAYMENT, 7);
        verify(tranMapper, never()).updateStageAtomic(eq(1), eq(TranStage.COMPLETED), any(), anyInt());
        verify(tranHistoryMapper).insert(argThat(history ->
                history.getTranId().equals(1) && "DELIVERY".equals(history.getStage())));
    }

    @Test
    void confirmPayment_approvedTransaction_shouldMoveTransactionToDeliveryOnly() {
        TTran tran = newTran(1, TranStage.APPROVED);
        TPayment payment = new TPayment();
        payment.setId(10);
        payment.setTranId(1);
        payment.setAmount(tran.getMoney());
        payment.setPaymentStatus("PENDING");
        payment.setPaymentType("FULL");

        when(paymentMapper.selectByPrimaryKeyForUpdate(10)).thenReturn(payment);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(paymentMapper.updateStatusIfCurrent(eq(10), eq("PENDING"), eq("COMPLETED"),
                any(Date.class), any(), any(Date.class), eq(7))).thenReturn(1);
        when(paymentMapper.selectByTranId(1)).thenReturn(Collections.singletonList(payment));
        when(tranMapper.updateStageAtomic(1, TranStage.DELIVERY, TranStage.APPROVED, 7)).thenReturn(1);

        TPayment result = tranService.confirmPayment(10, true, "到账");

        assertEquals("COMPLETED", result.getPaymentStatus());
        verify(tranMapper).updateStageAtomic(1, TranStage.DELIVERY, TranStage.APPROVED, 7);
        verify(tranMapper, never()).updateStageAtomic(eq(1), eq(TranStage.COMPLETED), any(), anyInt());
        verify(tranHistoryMapper).insert(argThat(history ->
                history.getTranId().equals(1) && "DELIVERY".equals(history.getStage())));
    }

    @Test
    void refundPayment_directCall_shouldRejectWorkflowBypass() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.refundPayment(10));

        assertEquals(CodeEnum.OPERATION_FAILED, exception.getCodeEnum());
        verify(paymentMapper, never()).insertSelective(any());
    }

    @Test
    void createRefundRequest_shouldRequireReasonAndCreatePendingApproval() {
        TPayment original = new TPayment();
        original.setId(10);
        original.setTranId(1);
        original.setAmount(BigDecimal.valueOf(100000));
        original.setPaymentStatus("COMPLETED");
        original.setPaymentType("FULL");

        TRefundRequest request = new TRefundRequest();
        request.setRefundType("ORDER_CANCEL");
        request.setAmount(BigDecimal.valueOf(100000));
        request.setReason("客户取消订单");

        when(paymentMapper.selectByPrimaryKeyForUpdate(10)).thenReturn(original);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.DELIVERY));
        when(refundRequestMapper.sumExecutedAmountByOriginalPaymentId(10)).thenReturn(BigDecimal.ZERO);
        when(refundRequestMapper.sumOpenAmountByOriginalPaymentId(10)).thenReturn(BigDecimal.ZERO);
        when(refundRequestMapper.insertSelective(any(TRefundRequest.class))).thenAnswer(inv -> {
            TRefundRequest inserted = inv.getArgument(0);
            inserted.setId(99);
            return 1;
        });

        TRefundRequest created = tranService.createRefundRequest(10, request);

        assertEquals("PENDING_APPROVAL", created.getStatus());
        assertEquals("ORDER_CANCEL", created.getRefundType());
        assertEquals(10, created.getOriginalPaymentId());
    }

    @Test
    void approveRefundRequest_sameRequester_shouldRejectSelfApproval() {
        TRefundRequest request = new TRefundRequest();
        request.setId(99);
        request.setTranId(1);
        request.setRequestedBy(7);
        request.setStatus("PENDING_APPROVAL");

        when(refundRequestMapper.selectByPrimaryKeyForUpdate(99)).thenReturn(request);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.DELIVERY));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tranService.approveRefundRequest(99, true, "同意"));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
        verify(refundRequestMapper, never()).updateApprovalIfPending(anyInt(), anyString(), anyInt(), any(), any(), anyInt(), any());
    }

    @Test
    void approveRefundRequest_approved_shouldMoveToPendingExecution() {
        TRefundRequest request = new TRefundRequest();
        request.setId(99);
        request.setTranId(1);
        request.setRequestedBy(6);
        request.setStatus("PENDING_APPROVAL");

        when(refundRequestMapper.selectByPrimaryKeyForUpdate(99)).thenReturn(request);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(newTran(1, TranStage.DELIVERY));
        when(refundRequestMapper.updateApprovalIfPending(eq(99), eq("PENDING_EXECUTION"), eq(7),
                any(Date.class), eq("同意"), eq(7), any(Date.class))).thenReturn(1);

        TRefundRequest approved = tranService.approveRefundRequest(99, true, "同意");

        assertEquals("PENDING_EXECUTION", approved.getStatus());
        assertEquals(7, approved.getApprovedBy());
    }

    @Test
    void executeRefundRequest_orderCancelFullRefund_shouldPreserveOriginalPaymentAndTransactionStage() {
        TPayment original = new TPayment();
        original.setId(10);
        original.setTranId(1);
        original.setAmount(BigDecimal.valueOf(100000));
        original.setPaymentStatus("COMPLETED");
        original.setPaymentType("FULL");
        original.setPaymentMethod("CASH");

        TRefundRequest request = new TRefundRequest();
        request.setId(99);
        request.setTranId(1);
        request.setOriginalPaymentId(10);
        request.setAmount(BigDecimal.valueOf(100000));
        request.setRefundType("ORDER_CANCEL");
        request.setReason("客户取消订单");
        request.setStatus("PENDING_EXECUTION");

        TTran tran = newTran(1, TranStage.DELIVERY);

        when(refundRequestMapper.selectByPrimaryKeyForUpdate(99)).thenReturn(request);
        when(paymentMapper.selectByPrimaryKeyForUpdate(10)).thenReturn(original);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(refundRequestMapper.sumExecutedAmountByOriginalPaymentId(10)).thenReturn(BigDecimal.ZERO);
        when(paymentMapper.insertSelective(any(TPayment.class))).thenAnswer(inv -> {
            TPayment refund = inv.getArgument(0);
            refund.setId(20);
            return 1;
        });
        when(refundRequestMapper.markExecutingIfPendingExecution(eq(99), eq(7), any(Date.class),
                eq("RF001"), eq("已原路退回"), eq(7), any(Date.class))).thenReturn(1);
        when(refundRequestMapper.markCompletedIfExecuting(eq(99), eq(20), any(Date.class), eq(7), any(Date.class))).thenReturn(1);

        TRefundRequest executed = tranService.executeRefundRequest(99, "RF001", "已原路退回", true, null);

        assertEquals("COMPLETED", executed.getStatus());
        assertEquals(20, executed.getRefundPaymentId());
        assertEquals("COMPLETED", original.getPaymentStatus());
        verify(tranMapper, never()).updateStageAtomic(eq(1), eq(TranStage.CANCELLED), any(), anyInt());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        verify(tranHistoryMapper, never()).insert(argThat(history ->
                history.getTranId().equals(1) && "CANCELLED".equals(history.getStage())));
    }

    @Test
    void executeRefundRequest_completedRequest_shouldReturnCurrentResultWithoutDuplicatePayment() {
        TRefundRequest request = new TRefundRequest();
        request.setId(99);
        request.setTranId(1);
        request.setOriginalPaymentId(10);
        request.setRefundPaymentId(20);
        request.setStatus("COMPLETED");

        when(refundRequestMapper.selectByPrimaryKeyForUpdate(99)).thenReturn(request);

        TRefundRequest result = tranService.executeRefundRequest(99, "RF001", "重复请求", true, null);

        assertSame(request, result);
        verify(paymentMapper, never()).insertSelective(any());
    }

    @Test
    void executeRefundRequest_failedResult_shouldMarkFailedAndCreateNoRefundPayment() {
        TRefundRequest request = new TRefundRequest();
        request.setId(99);
        request.setTranId(1);
        request.setOriginalPaymentId(10);
        request.setAmount(BigDecimal.valueOf(5000));
        request.setRefundType("OVERPAY");
        request.setReason("多收退款");
        request.setStatus("PENDING_EXECUTION");

        TPayment original = new TPayment();
        original.setId(10);
        original.setTranId(1);
        original.setAmount(BigDecimal.valueOf(100000));
        original.setPaymentStatus("COMPLETED");
        original.setPaymentType("FULL");

        TTran tran = newTran(1, TranStage.DELIVERY);

        when(refundRequestMapper.selectByPrimaryKeyForUpdate(99)).thenReturn(request);
        when(paymentMapper.selectByPrimaryKeyForUpdate(10)).thenReturn(original);
        when(tranMapper.selectByPrimaryKey(1)).thenReturn(tran);
        when(tranMapper.selectByPrimaryKeyForUpdate(1)).thenReturn(tran);
        when(refundRequestMapper.markFailedIfExecutable(eq(99), eq(7), any(Date.class), eq("银行退回"),
                eq("RF-FAILED"), eq("失败备注"), eq(7), any(Date.class))).thenReturn(1);

        TRefundRequest failed = tranService.executeRefundRequest(99, "RF-FAILED", "失败备注", false, "银行退回");

        assertEquals("FAILED", failed.getStatus());
        assertEquals("银行退回", failed.getFailureReason());
        verify(paymentMapper, never()).insertSelective(any());
    }
}
