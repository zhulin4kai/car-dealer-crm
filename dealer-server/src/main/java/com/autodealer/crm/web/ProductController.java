package com.autodealer.crm.web;

import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('product:list')")
    public R<PageInfo<TProduct>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(productService.getProductList(page, size));
    }
    
    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('product:view')")
    public R<TProduct> getProductById(@PathVariable Long id) {
        return R.OK(productService.getProductById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    public R<Void> addProduct(@Valid @RequestBody TProduct product) {
        productService.addProduct(product);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody TProduct product) {
        product.setId(id);
        productService.updateProduct(product);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return R.OK();
    }
    
    @GetMapping("/stockalerts")
    @PreAuthorize("hasAuthority('product:view')")
    public R<PageInfo<TProduct>> getStockAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {
        return R.OK(productService.getStockAlerts(page, size, sku, name, categoryId));
    }
    
    @PostMapping("/stock/restock")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> restock(@RequestBody RestockRequest request) {
        productService.restock(request.getProductId(), request.getQuantity(), request.getRemark());
        return R.OK();
    }
    
    @Data
    public static class RestockRequest {
        private Long productId;
        private Integer quantity;
        private String remark;
    }
}
