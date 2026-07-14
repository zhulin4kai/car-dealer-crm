package com.autodealer.crm.modules.commerce.catalog.application.api;

import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProductCategory;
import com.github.pagehelper.PageInfo;

public interface ProductCategoryService {
    PageInfo<TProductCategory> getCategoryList(Integer pageNum, Integer pageSize);

    TProductCategory getCategoryById(Long id);

    TProductCategory getCategoryByCode(String code);

    void addCategory(TProductCategory category);

    void updateCategory(TProductCategory category);

    void deleteCategory(Long id);
}
