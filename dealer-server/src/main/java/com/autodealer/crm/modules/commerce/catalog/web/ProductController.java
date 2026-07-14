package com.autodealer.crm.modules.commerce.catalog.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.commerce.catalog.application.api.dto.CreateProductRequest;
import com.autodealer.crm.modules.commerce.catalog.application.api.dto.UpdateProductRequest;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.commerce.catalog.application.api.ProductService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_LIST + "')")
    public Result<PageInfo<TProduct>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.OK(productService.getProductList(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_VIEW + "')")
    public Result<TProduct> getProductById(@PathVariable Long id) {
        return Result.OK(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_ADD + "')")
    public Result<Void> addProduct(@Valid @RequestBody CreateProductRequest req) {
        TProduct product = new TProduct();
        product.setSku(req.getSku());
        product.setName(req.getName());
        product.setCategoryId(req.getCategoryId());
        product.setSpecification(req.getSpecification());
        product.setPrice(req.getPrice());
        product.setStock(req.getStock());
        product.setMinStock(req.getMinStock());
        product.setStatus(req.getStatus());
        productService.addProduct(product);
        return Result.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_EDIT + "')")
    public Result<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest req) {
        TProduct product = new TProduct();
        product.setId(id);
        product.setSku(req.getSku());
        product.setName(req.getName());
        product.setCategoryId(req.getCategoryId());
        product.setSpecification(req.getSpecification());
        product.setPrice(req.getPrice());
        product.setMinStock(req.getMinStock());
        product.setStatus(req.getStatus());
        productService.updateProduct(product);
        return Result.OK();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_DELETE + "')")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.OK();
    }

    @GetMapping("/stockalerts")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_VIEW + "')")
    public Result<PageInfo<TProduct>> getStockAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {
        return Result.OK(productService.getStockAlerts(page, size, sku, name, categoryId));
    }
}
