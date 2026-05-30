package com.bjpowernode.web;

import com.bjpowernode.model.Product;
import com.bjpowernode.result.Result;
import com.bjpowernode.service.ProductService;
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
    public Result<PageInfo<Product>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(productService.getProductList(page, size));
    }
    
    @GetMapping("/{id:[0-9]+}")
    public Result<Product> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }
    
    @PostMapping
    public Result<Void> addProduct(@Valid @RequestBody Product product) {
        productService.addProduct(product);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        product.setId(id);
        productService.updateProduct(product);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }
    
    @GetMapping("/stockalerts")
    public Result<PageInfo<Product>> getStockAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        return Result.success(productService.getStockAlerts(page, size, sku, name, category));
    }
    
    @PostMapping("/stock/restock")
    public Result<Void> restock(@RequestBody RestockRequest request) {
        productService.restock(request.getProductId(), request.getQuantity(), request.getRemark());
        return Result.success();
    }
    
    @Data
    public static class RestockRequest {
        private Long productId;
        private Integer quantity;
        private String remark;
    }
}