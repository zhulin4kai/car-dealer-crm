package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.CreateProductRequest;
import com.autodealer.crm.dto.UpdateProductRequest;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductService;
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
    public R<PageInfo<TProduct>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(productService.getProductList(page, size));
    }

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_VIEW + "')")
    public R<TProduct> getProductById(@PathVariable Long id) {
        return R.OK(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_ADD + "')")
    public R<Void> addProduct(@Valid @RequestBody CreateProductRequest req) {
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
        return R.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_EDIT + "')")
    public R<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest req) {
        TProduct product = new TProduct();
        product.setId(id);
        product.setSku(req.getSku());
        product.setName(req.getName());
        product.setCategoryId(req.getCategoryId());
        product.setSpecification(req.getSpecification());
        product.setPrice(req.getPrice());
        product.setStock(req.getStock());
        product.setMinStock(req.getMinStock());
        product.setStatus(req.getStatus());
        productService.updateProduct(product);
        return R.OK();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_DELETE + "')")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return R.OK();
    }

    @GetMapping("/stockalerts")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_VIEW + "')")
    public R<PageInfo<TProduct>> getStockAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {
        return R.OK(productService.getStockAlerts(page, size, sku, name, categoryId));
    }
}
