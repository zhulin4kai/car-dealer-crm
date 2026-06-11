package com.autodealer.crm.service.impl;

import com.autodealer.crm.mapper.TProductPromotionMapper;
import com.autodealer.crm.model.TProductPromotion;
import com.autodealer.crm.service.ProductPromotionService;
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
    private TProductPromotionMapper promotionMapper;
    
    @Override
    public PageInfo<TProductPromotion> getPromotionList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProductPromotion> promotions = promotionMapper.selectList((pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(promotions);
    }
    
    @Override
    public TProductPromotion getPromotionById(Long id) {
        return promotionMapper.selectById(id);
    }
    
    @Override
    @Transactional
    public void addPromotion(TProductPromotion promotion) {
        promotion.setCreateTime(LocalDateTime.now());
        promotion.setUpdateTime(LocalDateTime.now());
        promotionMapper.insert(promotion);
    }
    
    @Override
    @Transactional
    public void updatePromotion(TProductPromotion promotion) {
        promotion.setUpdateTime(LocalDateTime.now());
        promotionMapper.update(promotion);
    }
    
    @Override
    @Transactional
    public void deletePromotion(Long id) {
        promotionMapper.deleteById(id);
    }
}