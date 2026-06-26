package com.autodealer.crm.service;

import com.autodealer.crm.dto.PromotionProductLine;
import com.autodealer.crm.model.TProductPromotion;
import com.github.pagehelper.PageInfo;

import java.math.BigDecimal;
import java.util.List;

public interface ProductPromotionService {
    PageInfo<TProductPromotion> getPromotionList(Integer pageNum, Integer pageSize);

    List<TProductPromotion> getAvailablePromotions(List<Long> productIds);
    
    TProductPromotion getPromotionById(Long id);
    
    void addPromotion(TProductPromotion promotion);
    
    void updatePromotion(TProductPromotion promotion);
    
    void deletePromotion(Long id);

    TProductPromotion publishPromotion(Long id);

    TProductPromotion activatePromotion(Long id);

    TProductPromotion pausePromotion(Long id, String reason);

    TProductPromotion endPromotion(Long id, String reason);

    TProductPromotion voidPromotion(Long id, String reason);

    TProductPromotion requireApplicablePromotion(Long promotionId, List<Long> productIds);

    BigDecimal calculateDiscount(List<PromotionProductLine> lines, TProductPromotion promotion);

    void reserveUsage(Long promotionId, BigDecimal discountAmount, String sourceType, Long sourceId);
}
