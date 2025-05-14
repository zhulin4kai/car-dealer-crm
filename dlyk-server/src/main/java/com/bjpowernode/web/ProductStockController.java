package com.bjpowernode.web;

import com.bjpowernode.model.ProductStockRecord;
import com.bjpowernode.result.Result;
import com.bjpowernode.service.ProductService;
import com.bjpowernode.service.ProductStockRecordService;
import com.github.pagehelper.PageInfo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productstock")
public class ProductStockController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductStockRecordService stockRecordService;
    
    @PostMapping("/restock")
    public Result<Void> restock(@RequestBody RestockRequest request) {
        productService.restock(request.getProductId(), request.getQuantity(), request.getRemark());
        return Result.success();
    }
    
    @GetMapping("/records/{productId}")
    public Result<PageInfo<ProductStockRecord>> getStockRecords(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(stockRecordService.getStockRecordsByProductId(productId, page, size));
    }
    
    @Data
    public static class RestockRequest {
        private Long productId;
        private Integer quantity;
        private String remark;
    }
} 