package com.autodealer.crm.modules.commerce.promotion.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.commerce.promotion.application.api.dto.PromotionProductLine;
import com.autodealer.crm.modules.commerce.promotion.application.api.enums.ProductPromotionStatus;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.commerce.catalog.persistence.mapper.TProductMapper;
import com.autodealer.crm.modules.commerce.promotion.persistence.mapper.TProductPromotionMapper;
import com.autodealer.crm.modules.commerce.promotion.persistence.mapper.TProductPromotionUsageMapper;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotion;
import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotionUsage;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.commerce.promotion.application.internal.ProductPromotionServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductPromotionServiceImplTest {

    @InjectMocks
    private ProductPromotionServiceImpl promotionService;

    @Mock
    private TProductPromotionMapper promotionMapper;
    @Mock
    private TProductPromotionUsageMapper promotionUsageMapper;
    @Mock
    private TProductMapper productMapper;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private OperationAuditRecorder auditRecorder;

    @BeforeEach
    void setUp() {
        lenient().when(productMapper.selectById(1L)).thenReturn(product());
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(7);
    }

    @Test
    void getPromotionList_shouldUseStablePageAndOrderMapper() {
        List<TProductPromotion> promotions = Arrays.asList(
                createPromotion(1L, "Summer Sale"),
                createPromotion(2L, "Winter Discount")
        );
        when(promotionMapper.selectList(anyInt(), anyInt())).thenReturn(promotions);

        PageInfo<TProductPromotion> result = promotionService.getPromotionList(1, 10);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(promotionMapper).selectList(0, 10);
    }

    @Test
    void getPromotionList_oversizedPageSize_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.getPromotionList(1, 101));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(promotionMapper, never()).selectList(anyInt(), anyInt());
    }

    @Test
    void getAvailablePromotions_shouldDeduplicateAndQueryActiveContextOnly() {
        List<TProductPromotion> promotions = List.of(createPromotion(1L, "可用促销"));
        when(promotionMapper.selectAvailableByProductIds(eq(List.of(1L, 2L)), any(LocalDateTime.class),
                eq("ALL"), eq("ALL"), eq("ALL"))).thenReturn(promotions);

        List<TProductPromotion> result = promotionService.getAvailablePromotions(
                Arrays.asList(1L, null, 2L, 1L));

        assertEquals(promotions, result);
        verify(promotionMapper).selectAvailableByProductIds(eq(List.of(1L, 2L)),
                any(LocalDateTime.class), eq("ALL"), eq("ALL"), eq("ALL"));
    }

    @Test
    void getAvailablePromotions_emptyProductIds_shouldNotQueryMapper() {
        assertTrue(promotionService.getAvailablePromotions(Collections.emptyList()).isEmpty());
        assertTrue(promotionService.getAvailablePromotions(Collections.singletonList(null)).isEmpty());

        verify(promotionMapper, never()).selectAvailableByProductIds(anyList(), any(), any(), any(), any());
    }

    @Test
    void addPromotion_shouldIgnoreClientStatusAndCreateDraft() {
        TProductPromotion promotion = createPromotion(null, "新促销");
        promotion.setStatus(ProductPromotionStatus.ACTIVE.name());
        when(promotionMapper.selectByCode("PROMO-NEW")).thenReturn(null);
        when(promotionMapper.insert(any(TProductPromotion.class))).thenAnswer(inv -> {
            TProductPromotion saved = inv.getArgument(0);
            saved.setId(10L);
            return 1;
        });

        promotionService.addPromotion(promotion);

        assertEquals(ProductPromotionStatus.DRAFT.name(), promotion.getStatus());
        assertEquals("ALL", promotion.getApplicableStore());
        verify(promotionMapper).insert(promotion);
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_PROMOTION_CREATE, "10");
    }

    @Test
    void addPromotion_percentageUsingEight_shouldReject() {
        TProductPromotion promotion = createPromotion(null, "错误折扣");
        promotion.setType("PERCENTAGE");
        promotion.setDiscount(new BigDecimal("8.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.addPromotion(promotion));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(promotionMapper, never()).insert(any());
    }

    @Test
    void updatePromotion_terminalStatus_shouldReject() {
        TProductPromotion existing = createPromotion(1L, "已结束促销");
        existing.setStatus(ProductPromotionStatus.ENDED.name());
        when(promotionMapper.selectById(1L)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.updatePromotion(createPromotion(1L, "已结束促销")));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(promotionMapper, never()).update(any());
    }

    @Test
    void publishPromotion_shouldUseStatusCasAndReturnLatest() {
        TProductPromotion draft = createPromotion(1L, "草稿促销");
        draft.setStatus(ProductPromotionStatus.DRAFT.name());
        TProductPromotion active = createPromotion(1L, "草稿促销");
        active.setStatus(ProductPromotionStatus.ACTIVE.name());
        when(promotionMapper.selectById(1L)).thenReturn(draft, active);
        when(promotionMapper.updateStatusAtomic(eq(1L), eq(List.of(ProductPromotionStatus.DRAFT.name())),
                eq(ProductPromotionStatus.ACTIVE.name()), isNull(), isNull(), isNull(), any())).thenReturn(1);

        TProductPromotion result = promotionService.publishPromotion(1L);

        assertEquals(ProductPromotionStatus.ACTIVE.name(), result.getStatus());
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_PROMOTION_STATUS_CHANGE, "1");
    }

    @Test
    void deletePromotion_referenced_shouldRejectWithoutPhysicalDelete() {
        TProductPromotion draft = createPromotion(1L, "已引用草稿");
        draft.setStatus(ProductPromotionStatus.DRAFT.name());
        when(promotionMapper.selectById(1L)).thenReturn(draft);
        when(promotionMapper.countPromotionReferences(1L)).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.deletePromotion(1L));

        assertEquals(CodeEnum.RESOURCE_IN_USE, ex.getCodeEnum());
        verify(promotionMapper, never()).deleteById(anyLong());
    }

    @Test
    void requireApplicablePromotion_nonActive_shouldReject() {
        TProductPromotion paused = createPromotion(1L, "暂停促销");
        paused.setStatus(ProductPromotionStatus.PAUSED.name());
        when(promotionMapper.selectById(1L)).thenReturn(paused);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.requireApplicablePromotion(1L, List.of(1L)));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
    }

    @Test
    void calculateDiscount_percentage_shouldDiscountMatchedProductOnly() {
        TProductPromotion promotion = createPromotion(1L, "折扣促销");
        promotion.setType("PERCENTAGE");
        promotion.setDiscount(new BigDecimal("0.90"));

        BigDecimal result = promotionService.calculateDiscount(List.of(
                new PromotionProductLine(1L, new BigDecimal("100.00"), 2),
                new PromotionProductLine(2L, new BigDecimal("50.00"), 1)
        ), promotion);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void calculateDiscount_amount_shouldCapAtMatchedAmount() {
        TProductPromotion promotion = createPromotion(1L, "金额促销");
        promotion.setType("AMOUNT");
        promotion.setDiscount(new BigDecimal("150.00"));

        BigDecimal result = promotionService.calculateDiscount(List.of(
                new PromotionProductLine(1L, new BigDecimal("100.00"), 1),
                new PromotionProductLine(2L, new BigDecimal("50.00"), 1)
        ), promotion);

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void reserveUsage_shouldConsumeBudgetAndWriteUsage() {
        when(promotionMapper.consumeBudgetAtomic(eq(1L), eq(new BigDecimal("100.00")), any()))
                .thenReturn(1);
        when(promotionUsageMapper.insert(any())).thenReturn(1);

        promotionService.reserveUsage(1L, new BigDecimal("100.00"), "TRAN", 99L);

        ArgumentCaptor<TProductPromotionUsage> usageCaptor = ArgumentCaptor.forClass(TProductPromotionUsage.class);
        verify(promotionUsageMapper).insert(usageCaptor.capture());
        assertEquals(1L, usageCaptor.getValue().getPromotionId());
        assertEquals("TRAN", usageCaptor.getValue().getSourceType());
        assertEquals(99L, usageCaptor.getValue().getSourceId());
        verify(auditRecorder).record(AuditActionEnum.PRODUCT_PROMOTION_USE, "1");
    }

    @Test
    void reserveUsage_exhausted_shouldRejectBeforeUsageInsert() {
        when(promotionMapper.consumeBudgetAtomic(eq(1L), any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.reserveUsage(1L, BigDecimal.TEN, "TRAN", 99L));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(promotionUsageMapper, never()).insert(any());
    }

    private TProductPromotion createPromotion(Long id, String name) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(id);
        promotion.setProductId(1L);
        promotion.setCode(id == null ? "PROMO-NEW" : "PROMO-" + id);
        promotion.setName(name);
        promotion.setType("AMOUNT");
        promotion.setDiscount(new BigDecimal("100.00"));
        promotion.setRuleSummary("每台直减100元");
        promotion.setApplicableStore("ALL");
        promotion.setCustomerType("ALL");
        promotion.setApplicableChannel("ALL");
        promotion.setInventoryScope("ALL");
        promotion.setStackable(false);
        promotion.setPriority(0);
        promotion.setUsedBudget(BigDecimal.ZERO);
        promotion.setUsedCount(0);
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(30));
        promotion.setStatus(ProductPromotionStatus.ACTIVE.name());
        return promotion;
    }

    private TProduct product() {
        TProduct product = new TProduct();
        product.setId(1L);
        product.setName("测试车型");
        product.setStatus("ON_SALE");
        return product;
    }
}
