package com.autodealer.crm.service;

import com.autodealer.crm.model.TProductPromotion;
import com.github.pagehelper.PageInfo;
import java.util.List;

public interface ProductPromotionService {
    PageInfo<TProductPromotion> getPromotionList(Integer pageNum, Integer pageSize);

    List<TProductPromotion> getAvailablePromotions(List<Long> productIds);
    
    TProductPromotion getPromotionById(Long id);
    
    void addPromotion(TProductPromotion promotion);
    
    void updatePromotion(TProductPromotion promotion);
    
    void deletePromotion(Long id);
}
