package com.bjpowernode.web;

import com.bjpowernode.model.ProductCategory;
import com.bjpowernode.result.Result;
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
    public Result<PageInfo<ProductCategory>> getCategoryList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(categoryService.getCategoryList(page, size));
    }
    
    @GetMapping("/{id}")
    public Result<ProductCategory> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }
    
    @PostMapping
    public Result<Void> addCategory(@RequestBody ProductCategory category) {
        categoryService.addCategory(category);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        categoryService.updateCategory(category);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}