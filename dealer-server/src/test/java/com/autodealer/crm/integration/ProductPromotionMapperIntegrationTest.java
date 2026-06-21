package com.autodealer.crm.integration;

import com.autodealer.crm.mapper.TProductPromotionMapper;
import com.autodealer.crm.model.TProductPromotion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
class ProductPromotionMapperIntegrationTest extends BackendIntegrationTestBase {

    @Autowired
    private TProductPromotionMapper promotionMapper;

    @Test
    @DisplayName("促销列表必须读取关联产品ID")
    void listMustReadProductId() {
        List<TProductPromotion> promotions = promotionMapper.selectList(0, 10);

        assertFalse(promotions.isEmpty());
        assertNotNull(promotions.get(0).getProductId());
    }

    @Test
    @DisplayName("新增和更新促销必须持久化产品ID")
    void insertAndUpdateMustPersistProductId() {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setProductId(1L);
        promotion.setName("产品促销关联测试");
        promotion.setType("AMOUNT");
        promotion.setDiscount(new BigDecimal("1000.00"));
        promotion.setStatus("待开始");
        promotion.setCreateTime(LocalDateTime.now());
        promotion.setUpdateTime(LocalDateTime.now());

        assertEquals(1, promotionMapper.insert(promotion));
        assertEquals(1L, promotionMapper.selectById(promotion.getId()).getProductId());

        promotion.setProductId(2L);
        assertEquals(1, promotionMapper.update(promotion));
        assertEquals(2L, promotionMapper.selectById(promotion.getId()).getProductId());
    }
}
