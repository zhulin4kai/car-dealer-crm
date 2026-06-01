package com.autodealer.crm.service;

import com.autodealer.crm.model.ProductPromotion;
import com.github.pagehelper.PageInfo;

public interface ProductPromotionService {
    PageInfo<ProductPromotion> getPromotionList(Integer pageNum, Integer pageSize);
    
    ProductPromotion getPromotionById(Long id);
    
    void addPromotion(ProductPromotion promotion);
    
    void updatePromotion(ProductPromotion promotion);
    
    void deletePromotion(Long id);
}