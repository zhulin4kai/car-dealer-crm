package com.autodealer.crm.service.impl;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TProductCategoryMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.model.TProductCategory;
import com.autodealer.crm.result.CodeEnum;
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
    private TProductCategoryMapper categoryMapper;

    @Autowired
    private TProductMapper productMapper;
    
    @Override
    public PageInfo<TProductCategory> getCategoryList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProductCategory> categories = categoryMapper.selectList((pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(categories);
    }
    
    @Override
    public TProductCategory getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }
    
    @Override
    public TProductCategory getCategoryByCode(String code) {
        return categoryMapper.selectByCode(code);
    }
    
    @Override
    @Transactional
    public void addCategory(TProductCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(category);
    }
    
    @Override
    @Transactional
    public void updateCategory(TProductCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }
    
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商品分类不存在");
        }
        if (productMapper.countByCategoryId(id) > 0) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "商品分类已被商品引用，不能删除");
        }
        categoryMapper.deleteById(id);
    }
}
