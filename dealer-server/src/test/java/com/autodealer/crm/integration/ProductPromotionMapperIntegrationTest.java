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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        promotion.setCode("PROMO-MAPPER-PERSIST");
        promotion.setName("产品促销关联测试");
        promotion.setType("AMOUNT");
        promotion.setDiscount(new BigDecimal("1000.00"));
        promotion.setRuleSummary("每台直减1000元");
        promotion.setApplicableStore("ALL");
        promotion.setCustomerType("ALL");
        promotion.setApplicableChannel("ALL");
        promotion.setInventoryScope("ALL");
        promotion.setStackable(false);
        promotion.setPriority(0);
        promotion.setUsedBudget(BigDecimal.ZERO);
        promotion.setUsedCount(0);
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(1));
        promotion.setStatus("DRAFT");
        promotion.setCreateTime(LocalDateTime.now());
        promotion.setUpdateTime(LocalDateTime.now());

        assertEquals(1, promotionMapper.insert(promotion));
        assertEquals(1L, promotionMapper.selectById(promotion.getId()).getProductId());

        promotion.setProductId(2L);
        assertEquals(1, promotionMapper.update(promotion));
        assertEquals(2L, promotionMapper.selectById(promotion.getId()).getProductId());
    }

    @Test
    @DisplayName("可用促销查询必须按交易商品、状态和有效期过滤")
    void selectAvailableByProductIds_shouldFilterByProductStatusAndTime() {
        LocalDateTime now = LocalDateTime.now();
        TProductPromotion available = insertPromotion(1L, "可用促销", "ACTIVE",
                now.minusDays(1), now.plusDays(1));
        insertPromotion(1L, "过期促销", "ACTIVE", now.minusDays(5), now.minusDays(1));
        insertPromotion(1L, "未开始促销", "ACTIVE", now.plusDays(1), now.plusDays(5));
        insertPromotion(1L, "暂停促销", "PAUSED", now.minusDays(1), now.plusDays(1));
        TProductPromotion otherProduct = insertPromotion(2L, "其他商品促销", "ACTIVE",
                now.minusDays(1), now.plusDays(1));

        List<TProductPromotion> result = promotionMapper.selectAvailableByProductIds(
                List.of(1L), now, "ALL", "ALL", "ALL");

        assertTrue(result.stream().anyMatch(promotion -> promotion.getId().equals(available.getId())));
        assertTrue(result.stream().noneMatch(promotion -> promotion.getName().equals("过期促销")));
        assertTrue(result.stream().noneMatch(promotion -> promotion.getName().equals("未开始促销")));
        assertTrue(result.stream().noneMatch(promotion -> promotion.getName().equals("暂停促销")));
        assertTrue(result.stream().noneMatch(promotion -> promotion.getId().equals(otherProduct.getId())));
    }

    private TProductPromotion insertPromotion(Long productId, String name, String status,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setProductId(productId);
        promotion.setCode("PROMO-MAPPER-" + name.hashCode());
        promotion.setName(name);
        promotion.setType("AMOUNT");
        promotion.setDiscount(new BigDecimal("1000.00"));
        promotion.setRuleSummary("每台直减1000元");
        promotion.setApplicableStore("ALL");
        promotion.setCustomerType("ALL");
        promotion.setApplicableChannel("ALL");
        promotion.setInventoryScope("ALL");
        promotion.setStackable(false);
        promotion.setPriority(0);
        promotion.setUsedBudget(BigDecimal.ZERO);
        promotion.setUsedCount(0);
        promotion.setStartTime(startTime);
        promotion.setEndTime(endTime);
        promotion.setStatus(status);
        promotion.setCreateTime(LocalDateTime.now());
        promotion.setUpdateTime(LocalDateTime.now());
        assertEquals(1, promotionMapper.insert(promotion));
        return promotion;
    }
}
