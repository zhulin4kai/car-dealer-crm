package com.autodealer.crm.web;

import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductService;
import com.autodealer.crm.service.ProductStockRecordService;
import com.github.pagehelper.PageInfo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productstock")
public class ProductStockController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductStockRecordService stockRecordService;
    
    @PostMapping("/restock")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> restock(@RequestBody RestockRequest request) {
        productService.restock(request.getProductId(), request.getQuantity(), request.getRemark());
        return R.OK();
    }
    
    @GetMapping("/records/{productId}")
    @PreAuthorize("hasAuthority('product:view')")
    public R<PageInfo<TProductStockRecord>> getStockRecords(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(stockRecordService.getStockRecordsByProductId(productId, page, size));
    }
    
    @Data
    public static class RestockRequest {
        private Long productId;
        private Integer quantity;
        private String remark;
    }
} 
