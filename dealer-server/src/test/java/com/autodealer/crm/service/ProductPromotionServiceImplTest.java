package com.autodealer.crm.service;

import com.autodealer.crm.mapper.ProductPromotionMapper;
import com.autodealer.crm.model.ProductPromotion;
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
    private ProductPromotionMapper promotionMapper;

    @Test
    void testGetPromotionList() {
        List<ProductPromotion> promotions = Arrays.asList(
                createPromotion(1L, "Summer Sale"),
                createPromotion(2L, "Winter Discount")
        );
        when(promotionMapper.selectList(anyInt(), anyInt())).thenReturn(promotions);

        PageInfo<ProductPromotion> result = promotionService.getPromotionList(1, 10);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(promotionMapper).selectList(0, 10);
    }

    @Test
    void testGetPromotionListEmpty() {
        when(promotionMapper.selectList(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        PageInfo<ProductPromotion> result = promotionService.getPromotionList(1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetPromotionById() {
        ProductPromotion promotion = createPromotion(1L, "Summer Sale");
        when(promotionMapper.selectById(1L)).thenReturn(promotion);

        ProductPromotion result = promotionService.getPromotionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Summer Sale", result.getName());
    }

    @Test
    void testGetPromotionByIdNotFound() {
        when(promotionMapper.selectById(999L)).thenReturn(null);

        ProductPromotion result = promotionService.getPromotionById(999L);

        assertNull(result);
    }

    @Test
    void testAddPromotion() {
        ProductPromotion promotion = new ProductPromotion();
        promotion.setName("New Promotion");
        promotion.setType("DISCOUNT");
        promotion.setDiscount(BigDecimal.valueOf(0.15));
        when(promotionMapper.insert(any(ProductPromotion.class))).thenReturn(1);

        promotionService.addPromotion(promotion);

        assertNotNull(promotion.getCreateTime());
        assertNotNull(promotion.getUpdateTime());
        verify(promotionMapper).insert(promotion);
    }

    @Test
    void testUpdatePromotion() {
        ProductPromotion promotion = new ProductPromotion();
        promotion.setId(1L);
        promotion.setName("Updated Promotion");
        when(promotionMapper.update(any(ProductPromotion.class))).thenReturn(1);

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

    private ProductPromotion createPromotion(Long id, String name) {
        ProductPromotion promotion = new ProductPromotion();
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
