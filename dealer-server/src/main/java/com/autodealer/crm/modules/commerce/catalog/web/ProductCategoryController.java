package com.autodealer.crm.modules.commerce.catalog.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.commerce.catalog.application.api.dto.CreateProductCategoryRequest;
import com.autodealer.crm.modules.commerce.catalog.application.api.dto.UpdateProductCategoryRequest;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProductCategory;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.commerce.catalog.application.api.ProductCategoryService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-categories")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_LIST + "')")
    public Result<PageInfo<TProductCategory>> getCategoryList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.OK(categoryService.getCategoryList(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_VIEW + "')")
    public Result<TProductCategory> getCategoryById(@PathVariable Long id) {
        return Result.OK(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_ADD + "')")
    public Result<Void> addCategory(@Valid @RequestBody CreateProductCategoryRequest req) {
        TProductCategory category = new TProductCategory();
        category.setName(req.getName());
        category.setCode(req.getCode());
        category.setDescription(req.getDescription());
        category.setSort(req.getSort());
        category.setStatus(req.getStatus());
        categoryService.addCategory(category);
        return Result.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_EDIT + "')")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateProductCategoryRequest req) {
        TProductCategory category = new TProductCategory();
        category.setId(id);
        category.setName(req.getName());
        category.setCode(req.getCode());
        category.setDescription(req.getDescription());
        category.setSort(req.getSort());
        category.setStatus(req.getStatus());
        categoryService.updateCategory(category);
        return Result.OK();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_DELETE + "')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.OK();
    }
}
