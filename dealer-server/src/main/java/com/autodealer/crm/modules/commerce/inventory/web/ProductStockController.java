package com.autodealer.crm.modules.commerce.inventory.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.commerce.inventory.application.api.dto.CreateProductVehicleRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.dto.ReleaseProductVehicleRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.dto.RestockRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.dto.ReserveProductVehicleRequest;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductStockRecord;
import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductVehicle;
import com.autodealer.crm.modules.commerce.inventory.application.api.query.ProductVehicleQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.commerce.catalog.application.api.ProductService;
import com.autodealer.crm.modules.commerce.inventory.application.api.ProductStockRecordService;
import com.autodealer.crm.modules.commerce.inventory.application.api.ProductVehicleService;
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

    @Autowired
    private ProductVehicleService productVehicleService;

    @PostMapping("/restock")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_ADJUST + "')")
    public Result<Void> restock(@Valid @RequestBody RestockRequest request) {
        productService.restock(request.getProductId(), request.getQuantity(), request.getRemark());
        return Result.OK();
    }

    @GetMapping("/records/{productId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_VIEW + "')")
    public Result<PageInfo<TProductStockRecord>> getStockRecords(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.OK(stockRecordService.getStockRecordsByProductId(productId, page, size));
    }

    @GetMapping("/vehicles")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_VIEW + "')")
    public Result<PageInfo<TProductVehicle>> getVehicles(ProductVehicleQuery query) {
        return Result.OK(productVehicleService.getVehiclePage(query));
    }

    @PostMapping("/vehicles")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_ADJUST + "')")
    public Result<TProductVehicle> inboundVehicle(@Valid @RequestBody CreateProductVehicleRequest request) {
        return Result.OK(productVehicleService.inboundVehicle(request));
    }

    @PostMapping("/vehicles/{vehicleId}/reserve")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_ADJUST + "')")
    public Result<TProductVehicle> reserveVehicle(@PathVariable Long vehicleId,
                                             @Valid @RequestBody ReserveProductVehicleRequest request) {
        return Result.OK(productVehicleService.reserveVehicle(vehicleId, request));
    }

    @PostMapping("/vehicles/{vehicleId}/release")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_STOCK_ADJUST + "')")
    public Result<TProductVehicle> releaseVehicle(@PathVariable Long vehicleId,
                                             @Valid @RequestBody ReleaseProductVehicleRequest request) {
        return Result.OK(productVehicleService.releaseVehicle(vehicleId, request));
    }
}
