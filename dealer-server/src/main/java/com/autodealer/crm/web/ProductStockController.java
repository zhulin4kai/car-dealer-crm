package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.RestockRequest;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductService;
import com.autodealer.crm.service.ProductStockRecordService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
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
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_ADJUST + "')")
    public R<Void> restock(@Valid @RequestBody RestockRequest request) {
        productService.restock(request.getProductId(), request.getQuantity(), request.getRemark());
        return R.OK();
    }

    @GetMapping("/records/{productId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_VIEW + "')")
    public R<PageInfo<TProductStockRecord>> getStockRecords(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(stockRecordService.getStockRecordsByProductId(productId, page, size));
    }
}
