package com.bjpowernode.service;

import com.bjpowernode.model.ProductPromotion;
import com.github.pagehelper.PageInfo;

public interface ProductPromotionService {
    PageInfo<ProductPromotion> getPromotionList(Integer pageNum, Integer pageSize);
    
    ProductPromotion getPromotionById(Long id);
    
    void addPromotion(ProductPromotion promotion);
    
    void updatePromotion(ProductPromotion promotion);
    
    void deletePromotion(Long id);
}