package com.autodealer.crm.service;

import com.autodealer.crm.model.TProductPromotion;
import com.github.pagehelper.PageInfo;

public interface ProductPromotionService {
    PageInfo<TProductPromotion> getPromotionList(Integer pageNum, Integer pageSize);
    
    TProductPromotion getPromotionById(Long id);
    
    void addPromotion(TProductPromotion promotion);
    
    void updatePromotion(TProductPromotion promotion);
    
    void deletePromotion(Long id);
}