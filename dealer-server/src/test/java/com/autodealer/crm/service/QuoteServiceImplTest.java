package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CreateQuoteRequest;
import com.autodealer.crm.dto.CreateQuoteVersionRequest;
import com.autodealer.crm.dto.QuoteItemRequest;
import com.autodealer.crm.dto.UpdateQuoteStatusRequest;
import com.autodealer.crm.enums.OpportunityStage;
import com.autodealer.crm.enums.QuoteStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TOpportunityMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TQuoteMapper;
import com.autodealer.crm.mapper.TQuoteStatusHistoryMapper;
import com.autodealer.crm.mapper.TQuoteVersionItemMapper;
import com.autodealer.crm.mapper.TQuoteVersionMapper;
import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TOpportunity;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TProductPromotion;
import com.autodealer.crm.model.TQuote;
import com.autodealer.crm.model.TQuoteStatusHistory;
import com.autodealer.crm.model.TQuoteVersion;
import com.autodealer.crm.model.TQuoteVersionItem;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.QuoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceImplTest {

    @InjectMocks
    private QuoteServiceImpl quoteService;

    @Mock
    private TQuoteMapper quoteMapper;
    @Mock
    private TQuoteVersionMapper versionMapper;
    @Mock
    private TQuoteVersionItemMapper itemMapper;
    @Mock
    private TQuoteStatusHistoryMapper historyMapper;
    @Mock
    private TProductMapper productMapper;
    @Mock
    private TCustomerMapper customerMapper;
    @Mock
    private TOpportunityMapper opportunityMapper;
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
        TCustomer customer = new TCustomer();
        customer.setId(10);
        lenient().when(customerMapper.selectScopedById(eq(10), nullable(Integer.class))).thenReturn(customer);
    }

    @Test
    void createQuote_shouldSnapshotProductAndNotDeductStock() {
        CreateQuoteRequest request = createQuoteRequest();
        when(productMapper.selectById(1L)).thenReturn(product());
        when(quoteMapper.insert(any())).thenAnswer(inv -> {
            TQuote quote = inv.getArgument(0);
            quote.setId(100L);
            return 1;
        });
        when(versionMapper.insert(any())).thenAnswer(inv -> {
            TQuoteVersion version = inv.getArgument(0);
            version.setId(200L);
            return 1;
        });
        when(itemMapper.insert(any())).thenReturn(1);
        when(quoteMapper.updateCurrentVersion(eq(100L), eq(200L), any(), eq(7))).thenReturn(1);
        when(historyMapper.insert(any())).thenReturn(1);
        TQuote persistedQuote = quote(100L, QuoteStatus.DRAFT, 200L);
        TQuoteVersion persistedVersion = version(200L, 1);
        when(quoteMapper.selectById(100L)).thenReturn(persistedQuote);
        when(versionMapper.selectById(200L)).thenReturn(persistedVersion);
        when(itemMapper.selectByVersionId(200L)).thenReturn(List.of());

        var detail = quoteService.createQuote(request);

        assertSame(persistedQuote, detail.getQuote());
        assertSame(persistedVersion, detail.getCurrentVersion());
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
        ArgumentCaptor<TQuoteVersionItem> itemCaptor = ArgumentCaptor.forClass(TQuoteVersionItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        TQuoteVersionItem item = itemCaptor.getValue();
        assertEquals("SKU-001", item.getProductSku());
        assertEquals("测试车型", item.getProductName());
        assertEquals(new BigDecimal("100000.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("200000.00"), item.getLineAmount());
        verify(auditRecorder).record(AuditActionEnum.QUOTE_CREATE, "100");
        verify(auditRecorder).record(AuditActionEnum.QUOTE_VERSION_CREATE, "200");
    }

    @Test
    void createQuote_inaccessibleCustomer_shouldRejectBeforeWrite() {
        when(customerMapper.selectScopedById(eq(10), nullable(Integer.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.createQuote(createQuoteRequest()));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(quoteMapper, never()).insert(any());
        verify(productMapper, never()).selectById(anyLong());
        verify(auditRecorder, never()).record(any(), anyString());
    }

    @Test
    void createQuote_withPromotion_shouldSnapshotPromotionFacts() {
        CreateQuoteRequest request = createQuoteRequest();
        request.getItems().get(0).setPromotionId(10L);
        TProductPromotion promotion = promotion();
        when(productMapper.selectById(1L)).thenReturn(product());
        when(promotionService.requireApplicablePromotion(eq(10L), eq(List.of(1L)))).thenReturn(promotion);
        when(promotionService.calculateDiscount(anyList(), eq(promotion))).thenReturn(new BigDecimal("2000.00"));
        when(quoteMapper.insert(any())).thenAnswer(inv -> {
            TQuote quote = inv.getArgument(0);
            quote.setId(100L);
            return 1;
        });
        when(versionMapper.insert(any())).thenAnswer(inv -> {
            TQuoteVersion version = inv.getArgument(0);
            version.setId(200L);
            return 1;
        });
        when(itemMapper.insert(any())).thenReturn(1);
        when(quoteMapper.updateCurrentVersion(eq(100L), eq(200L), any(), eq(7))).thenReturn(1);
        when(historyMapper.insert(any())).thenReturn(1);
        when(quoteMapper.selectById(100L)).thenReturn(quote(100L, QuoteStatus.DRAFT, 200L));
        when(versionMapper.selectById(200L)).thenReturn(version(200L, 1));
        when(itemMapper.selectByVersionId(200L)).thenReturn(List.of());

        quoteService.createQuote(request);

        ArgumentCaptor<TQuoteVersionItem> itemCaptor = ArgumentCaptor.forClass(TQuoteVersionItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        TQuoteVersionItem item = itemCaptor.getValue();
        assertEquals(10L, item.getPromotionId());
        assertEquals("PROMO-Q-001", item.getPromotionCode());
        assertEquals("报价促销", item.getPromotionName());
        assertEquals("每台直减2000元", item.getPromotionRuleSummary());
        assertEquals(new BigDecimal("2000.00"), item.getPromotionAmount());
        assertTrue(item.getPromotionSnapshot().contains("PROMO-Q-001"));
    }

    @Test
    void createQuote_inaccessibleOpportunity_shouldRejectBeforeWrite() {
        CreateQuoteRequest request = createQuoteRequest();
        request.setOpportunityId(900L);
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(opportunityMapper.selectById(900L)).thenReturn(opportunity(900L, 10, 8, OpportunityStage.NEEDS_ANALYSIS));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.createQuote(request));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(quoteMapper, never()).insert(any());
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    void createQuote_mismatchedOpportunityCustomer_shouldRejectBeforeWrite() {
        CreateQuoteRequest request = createQuoteRequest();
        request.setOpportunityId(900L);
        when(opportunityMapper.selectById(900L)).thenReturn(opportunity(900L, 11, 7, OpportunityStage.NEEDS_ANALYSIS));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.createQuote(request));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(quoteMapper, never()).insert(any());
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    void createQuote_terminalOpportunity_shouldRejectBeforeWrite() {
        CreateQuoteRequest request = createQuoteRequest();
        request.setOpportunityId(900L);
        when(opportunityMapper.selectById(900L)).thenReturn(opportunity(900L, 10, 7, OpportunityStage.WON));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.createQuote(request));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(quoteMapper, never()).insert(any());
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    void transitionStatus_chineseStatus_shouldRejectBeforeUpdate() {
        when(quoteMapper.selectById(100L)).thenReturn(quote(100L, QuoteStatus.DRAFT, 200L));
        UpdateQuoteStatusRequest request = statusRequest(QuoteStatus.DRAFT.name(), "待客户确认");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.transitionStatus(100L, request));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(quoteMapper, never()).updateStatusIfCurrent(anyLong(), anyString(), anyString(), any(), anyInt());
        verify(historyMapper, never()).insert(any());
    }

    @Test
    void transitionStatus_illegalJump_shouldRejectBeforeHistory() {
        when(quoteMapper.selectById(100L)).thenReturn(quote(100L, QuoteStatus.DRAFT, 200L));
        UpdateQuoteStatusRequest request = statusRequest(QuoteStatus.DRAFT.name(), QuoteStatus.ACCEPTED.name());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.transitionStatus(100L, request));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(quoteMapper, never()).updateStatusIfCurrent(anyLong(), anyString(), anyString(), any(), anyInt());
        verify(historyMapper, never()).insert(any());
    }

    @Test
    void transitionStatus_success_shouldUseCasAndWriteHistory() {
        TQuote draft = quote(100L, QuoteStatus.DRAFT, 200L);
        TQuote submitted = quote(100L, QuoteStatus.PENDING_CUSTOMER_CONFIRMATION, 200L);
        when(quoteMapper.selectById(100L)).thenReturn(draft, submitted);
        when(quoteMapper.updateStatusIfCurrent(eq(100L), eq("DRAFT"),
                eq("PENDING_CUSTOMER_CONFIRMATION"), any(), eq(7))).thenReturn(1);
        when(historyMapper.insert(any())).thenReturn(1);
        UpdateQuoteStatusRequest request = statusRequest(
                QuoteStatus.DRAFT.name(), QuoteStatus.PENDING_CUSTOMER_CONFIRMATION.name());

        TQuote result = quoteService.transitionStatus(100L, request);

        assertSame(submitted, result);
        ArgumentCaptor<TQuoteStatusHistory> historyCaptor = ArgumentCaptor.forClass(TQuoteStatusHistory.class);
        verify(historyMapper).insert(historyCaptor.capture());
        assertEquals("DRAFT", historyCaptor.getValue().getFromStatus());
        assertEquals("PENDING_CUSTOMER_CONFIRMATION", historyCaptor.getValue().getToStatus());
        verify(auditRecorder).record(AuditActionEnum.QUOTE_STATUS_CHANGE, "100");
    }

    @Test
    void transitionStatus_customerDecisionWithoutEvidence_shouldRejectBeforeUpdate() {
        when(quoteMapper.selectById(100L))
                .thenReturn(quote(100L, QuoteStatus.PENDING_CUSTOMER_CONFIRMATION, 200L));
        UpdateQuoteStatusRequest request = statusRequest(
                QuoteStatus.PENDING_CUSTOMER_CONFIRMATION.name(), QuoteStatus.ACCEPTED.name());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quoteService.transitionStatus(100L, request));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(quoteMapper, never()).updateStatusIfCurrent(anyLong(), anyString(), anyString(), any(), anyInt());
        verify(historyMapper, never()).insert(any());
    }

    @Test
    void transitionStatus_customerDecision_shouldPersistConfirmationEvidence() {
        TQuote pending = quote(100L, QuoteStatus.PENDING_CUSTOMER_CONFIRMATION, 200L);
        TQuote accepted = quote(100L, QuoteStatus.ACCEPTED, 200L);
        when(quoteMapper.selectById(100L)).thenReturn(pending, accepted);
        when(quoteMapper.updateStatusIfCurrent(eq(100L), eq("PENDING_CUSTOMER_CONFIRMATION"),
                eq("ACCEPTED"), any(), eq(7))).thenReturn(1);
        when(historyMapper.insert(any())).thenReturn(1);
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 6, 25, 10, 0);
        UpdateQuoteStatusRequest request = statusRequest(
                QuoteStatus.PENDING_CUSTOMER_CONFIRMATION.name(), QuoteStatus.ACCEPTED.name());
        request.setConfirmedByName("王先生");
        request.setConfirmedAt(confirmedAt);
        request.setConfirmationMethod("CUSTOMER_SIGNATURE");
        request.setConfirmationEvidence("contract://quote/100/signature");

        TQuote result = quoteService.transitionStatus(100L, request);

        assertSame(accepted, result);
        ArgumentCaptor<TQuoteStatusHistory> historyCaptor = ArgumentCaptor.forClass(TQuoteStatusHistory.class);
        verify(historyMapper).insert(historyCaptor.capture());
        TQuoteStatusHistory history = historyCaptor.getValue();
        assertEquals("PENDING_CUSTOMER_CONFIRMATION", history.getFromStatus());
        assertEquals("ACCEPTED", history.getToStatus());
        assertEquals("王先生", history.getConfirmedByName());
        assertEquals(confirmedAt, history.getConfirmedAt());
        assertEquals("CUSTOMER_SIGNATURE", history.getConfirmationMethod());
        assertEquals("contract://quote/100/signature", history.getConfirmationEvidence());
    }

    @Test
    void createVersion_submittedQuote_shouldCreateNextVersionAndResetDraft() {
        CreateQuoteVersionRequest request = createVersionRequest();
        when(quoteMapper.selectById(100L)).thenReturn(
                quote(100L, QuoteStatus.PENDING_CUSTOMER_CONFIRMATION, 200L),
                quote(100L, QuoteStatus.DRAFT, 201L));
        when(versionMapper.selectMaxVersionNo(100L)).thenReturn(1);
        when(productMapper.selectById(1L)).thenReturn(product());
        when(versionMapper.insert(any())).thenAnswer(inv -> {
            TQuoteVersion version = inv.getArgument(0);
            version.setId(201L);
            return 1;
        });
        when(itemMapper.insert(any())).thenReturn(1);
        when(quoteMapper.updateCurrentVersion(eq(100L), eq(201L), any(), eq(7))).thenReturn(1);
        when(quoteMapper.updateStatusIfCurrent(eq(100L), eq("PENDING_CUSTOMER_CONFIRMATION"),
                eq("DRAFT"), any(), eq(7))).thenReturn(1);
        when(historyMapper.insert(any())).thenReturn(1);
        when(versionMapper.selectById(201L)).thenReturn(version(201L, 2));
        when(itemMapper.selectByVersionId(201L)).thenReturn(List.of());

        var detail = quoteService.createVersion(100L, request);

        assertEquals(201L, detail.getQuote().getCurrentVersionId());
        verify(versionMapper).insert(any());
        verify(quoteMapper).updateStatusIfCurrent(eq(100L), eq("PENDING_CUSTOMER_CONFIRMATION"),
                eq("DRAFT"), any(), eq(7));
        verify(productMapper, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void createVersion_draftQuote_shouldOverwriteCurrentVersion() {
        CreateQuoteVersionRequest request = createVersionRequest();
        when(quoteMapper.selectById(100L)).thenReturn(quote(100L, QuoteStatus.DRAFT, 200L));
        when(versionMapper.selectById(200L)).thenReturn(version(200L, 1), version(200L, 1));
        when(productMapper.selectById(1L)).thenReturn(product());
        when(versionMapper.updateDraftVersion(any())).thenReturn(1);
        when(itemMapper.deleteByVersionId(200L)).thenReturn(1);
        when(itemMapper.insert(any())).thenReturn(1);
        when(itemMapper.selectByVersionId(200L)).thenReturn(List.of());

        var detail = quoteService.createVersion(100L, request);

        assertEquals(200L, detail.getCurrentVersion().getId());
        verify(versionMapper, never()).insert(any());
        verify(quoteMapper, never()).updateCurrentVersion(anyLong(), anyLong(), any(), anyInt());
        verify(itemMapper).deleteByVersionId(200L);
    }

    private CreateQuoteRequest createQuoteRequest() {
        CreateQuoteRequest request = new CreateQuoteRequest();
        request.setCustomerId(10);
        request.setValidUntil(LocalDateTime.now().plusDays(7));
        request.setRemark("初次报价");
        request.setItems(List.of(itemRequest()));
        return request;
    }

    private CreateQuoteVersionRequest createVersionRequest() {
        CreateQuoteVersionRequest request = new CreateQuoteVersionRequest();
        request.setValidUntil(LocalDateTime.now().plusDays(7));
        request.setRemark("调整报价");
        request.setItems(List.of(itemRequest()));
        return request;
    }

    private QuoteItemRequest itemRequest() {
        QuoteItemRequest item = new QuoteItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        return item;
    }

    private UpdateQuoteStatusRequest statusRequest(String expected, String target) {
        UpdateQuoteStatusRequest request = new UpdateQuoteStatusRequest();
        request.setExpectedStatus(expected);
        request.setTargetStatus(target);
        request.setReason("业务推进");
        return request;
    }

    private TQuote quote(Long id, QuoteStatus status, Long currentVersionId) {
        TQuote quote = new TQuote();
        quote.setId(id);
        quote.setCustomerId(10);
        quote.setStatus(status.name());
        quote.setCurrentVersionId(currentVersionId);
        return quote;
    }

    private TQuoteVersion version(Long id, Integer versionNo) {
        TQuoteVersion version = new TQuoteVersion();
        version.setId(id);
        version.setQuoteId(100L);
        version.setVersionNo(versionNo);
        version.setTotalAmount(new BigDecimal("200000.00"));
        return version;
    }

    private TProduct product() {
        TProduct product = new TProduct();
        product.setId(1L);
        product.setSku("SKU-001");
        product.setName("测试车型");
        product.setSpecification("2026款");
        product.setPrice(new BigDecimal("100000.00"));
        product.setStatus("ON_SALE");
        return product;
    }

    private TProductPromotion promotion() {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(10L);
        promotion.setProductId(1L);
        promotion.setCode("PROMO-Q-001");
        promotion.setName("报价促销");
        promotion.setType("AMOUNT");
        promotion.setDiscount(new BigDecimal("2000.00"));
        promotion.setRuleSummary("每台直减2000元");
        return promotion;
    }

    private TOpportunity opportunity(Long id, Integer customerId, Integer ownerId, OpportunityStage stage) {
        TOpportunity opportunity = new TOpportunity();
        opportunity.setId(id);
        opportunity.setCustomerId(customerId);
        opportunity.setOwnerId(ownerId);
        opportunity.setStage(stage.name());
        return opportunity;
    }
}
