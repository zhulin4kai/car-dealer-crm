package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.CreateProductCategoryRequest;
import com.autodealer.crm.dto.UpdateProductCategoryRequest;
import com.autodealer.crm.model.TProductCategory;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductCategoryService;
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
    public R<PageInfo<TProductCategory>> getCategoryList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(categoryService.getCategoryList(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_VIEW + "')")
    public R<TProductCategory> getCategoryById(@PathVariable Long id) {
        return R.OK(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_ADD + "')")
    public R<Void> addCategory(@Valid @RequestBody CreateProductCategoryRequest req) {
        TProductCategory category = new TProductCategory();
        category.setName(req.getName());
        category.setCode(req.getCode());
        category.setDescription(req.getDescription());
        category.setSort(req.getSort());
        category.setStatus(req.getStatus());
        categoryService.addCategory(category);
        return R.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_EDIT + "')")
    public R<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateProductCategoryRequest req) {
        TProductCategory category = new TProductCategory();
        category.setId(id);
        category.setName(req.getName());
        category.setCode(req.getCode());
        category.setDescription(req.getDescription());
        category.setSort(req.getSort());
        category.setStatus(req.getStatus());
        categoryService.updateCategory(category);
        return R.OK();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_CATEGORY_DELETE + "')")
    public R<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return R.OK();
    }
}
