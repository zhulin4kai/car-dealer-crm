package com.autodealer.crm.service;

import com.autodealer.crm.model.TProductCategory;
import com.github.pagehelper.PageInfo;

public interface ProductCategoryService {
    PageInfo<TProductCategory> getCategoryList(Integer pageNum, Integer pageSize);
    
    TProductCategory getCategoryById(Long id);
    
    TProductCategory getCategoryByCode(String code);
    
    void addCategory(TProductCategory category);
    
    void updateCategory(TProductCategory category);
    
    void deleteCategory(Long id);
} 