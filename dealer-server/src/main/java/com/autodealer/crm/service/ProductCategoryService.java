package com.autodealer.crm.service;

import com.autodealer.crm.model.ProductCategory;
import com.github.pagehelper.PageInfo;

public interface ProductCategoryService {
    PageInfo<ProductCategory> getCategoryList(Integer pageNum, Integer pageSize);
    
    ProductCategory getCategoryById(Long id);
    
    ProductCategory getCategoryByCode(String code);
    
    void addCategory(ProductCategory category);
    
    void updateCategory(ProductCategory category);
    
    void deleteCategory(Long id);
} 