package com.bjpowernode.web;

import com.bjpowernode.model.ProductCategory;
import com.bjpowernode.result.R;
import com.bjpowernode.service.ProductCategoryService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-categories")
public class ProductCategoryController {
    
    @Autowired
    private ProductCategoryService categoryService;
    
    @GetMapping
    public R<PageInfo<ProductCategory>> getCategoryList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(categoryService.getCategoryList(page, size));
    }
    
    @GetMapping("/{id}")
    public R<ProductCategory> getCategoryById(@PathVariable Long id) {
        return R.OK(categoryService.getCategoryById(id));
    }
    
    @PostMapping
    public R<Void> addCategory(@RequestBody ProductCategory category) {
        categoryService.addCategory(category);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    public R<Void> updateCategory(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        categoryService.updateCategory(category);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return R.OK();
    }
}