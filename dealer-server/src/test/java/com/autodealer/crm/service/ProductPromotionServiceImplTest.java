package com.autodealer.crm.service;

import com.autodealer.crm.mapper.TProductPromotionMapper;
import com.autodealer.crm.model.TProductPromotion;
import com.autodealer.crm.service.impl.ProductPromotionServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void testGetPromotionList() {
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
    void testGetPromotionListEmpty() {
        when(promotionMapper.selectList(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        PageInfo<TProductPromotion> result = promotionService.getPromotionList(1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetPromotionById() {
        TProductPromotion promotion = createPromotion(1L, "Summer Sale");
        when(promotionMapper.selectById(1L)).thenReturn(promotion);

        TProductPromotion result = promotionService.getPromotionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Summer Sale", result.getName());
    }

    @Test
    void testGetPromotionByIdNotFound() {
        when(promotionMapper.selectById(999L)).thenReturn(null);

        TProductPromotion result = promotionService.getPromotionById(999L);

        assertNull(result);
    }

    @Test
    void testAddPromotion() {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setName("New Promotion");
        promotion.setType("DISCOUNT");
        promotion.setDiscount(BigDecimal.valueOf(0.15));
        when(promotionMapper.insert(any(TProductPromotion.class))).thenReturn(1);

        promotionService.addPromotion(promotion);

        assertNotNull(promotion.getCreateTime());
        assertNotNull(promotion.getUpdateTime());
        verify(promotionMapper).insert(promotion);
    }

    @Test
    void testUpdatePromotion() {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(1L);
        promotion.setName("Updated Promotion");
        when(promotionMapper.update(any(TProductPromotion.class))).thenReturn(1);

        promotionService.updatePromotion(promotion);

        assertNotNull(promotion.getUpdateTime());
        verify(promotionMapper).update(promotion);
    }

    @Test
    void testDeletePromotion() {
        when(promotionMapper.deleteById(1L)).thenReturn(1);

        promotionService.deletePromotion(1L);

        verify(promotionMapper).deleteById(1L);
    }

    private TProductPromotion createPromotion(Long id, String name) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(id);
        promotion.setName(name);
        promotion.setType("DISCOUNT");
        promotion.setDiscount(BigDecimal.valueOf(0.1));
        promotion.setStartTime(LocalDateTime.now());
        promotion.setEndTime(LocalDateTime.now().plusDays(30));
        promotion.setStatus("ACTIVE");
        return promotion;
    }
}
