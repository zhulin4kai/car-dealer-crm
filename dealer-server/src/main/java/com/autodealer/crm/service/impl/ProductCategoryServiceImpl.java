package com.autodealer.crm.service.impl;

import com.autodealer.crm.mapper.ProductCategoryMapper;
import com.autodealer.crm.model.ProductCategory;
import com.autodealer.crm.service.ProductCategoryService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {
    
    @Autowired
    private ProductCategoryMapper categoryMapper;
    
    @Override
    public PageInfo<ProductCategory> getCategoryList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductCategory> categories = categoryMapper.selectList((pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(categories);
    }
    
    @Override
    public ProductCategory getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }
    
    @Override
    public ProductCategory getCategoryByCode(String code) {
        return categoryMapper.selectByCode(code);
    }
    
    @Override
    @Transactional
    public void addCategory(ProductCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(category);
    }
    
    @Override
    @Transactional
    public void updateCategory(ProductCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }
    
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }
}