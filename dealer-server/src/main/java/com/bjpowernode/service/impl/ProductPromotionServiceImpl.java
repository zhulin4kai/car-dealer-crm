package com.bjpowernode.service.impl;

import com.bjpowernode.mapper.ProductPromotionMapper;
import com.bjpowernode.model.ProductPromotion;
import com.bjpowernode.service.ProductPromotionService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductPromotionServiceImpl implements ProductPromotionService {
    
    @Autowired
    private ProductPromotionMapper promotionMapper;
    
    @Override
    public PageInfo<ProductPromotion> getPromotionList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductPromotion> promotions = promotionMapper.selectList((pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(promotions);
    }
    
    @Override
    public ProductPromotion getPromotionById(Long id) {
        return promotionMapper.selectById(id);
    }
    
    @Override
    @Transactional
    public void addPromotion(ProductPromotion promotion) {
        promotion.setCreateTime(LocalDateTime.now());
        promotion.setUpdateTime(LocalDateTime.now());
        promotionMapper.insert(promotion);
    }
    
    @Override
    @Transactional
    public void updatePromotion(ProductPromotion promotion) {
        promotion.setUpdateTime(LocalDateTime.now());
        promotionMapper.update(promotion);
    }
    
    @Override
    @Transactional
    public void deletePromotion(Long id) {
        promotionMapper.deleteById(id);
    }
}