package com.autodealer.crm.web;

import com.autodealer.crm.model.Product;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    public R<PageInfo<Product>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(productService.getProductList(page, size));
    }
    
    @GetMapping("/{id:[0-9]+}")
    public R<Product> getProductById(@PathVariable Long id) {
        return R.OK(productService.getProductById(id));
    }
    
    @PostMapping
    public R<Void> addProduct(@Valid @RequestBody Product product) {
        productService.addProduct(product);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    public R<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        product.setId(id);
        productService.updateProduct(product);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return R.OK();
    }
    
    @GetMapping("/stockalerts")
    public R<PageInfo<Product>> getStockAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        return R.OK(productService.getStockAlerts(page, size, sku, name, category));
    }
    
    @PostMapping("/stock/restock")
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