package com.autodealer.crm.modules.commerce.promotion.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.commerce.promotion.application.api.dto.CreateProductPromotionRequest;
import com.autodealer.crm.modules.commerce.promotion.application.api.dto.ProductPromotionLifecycleRequest;
import com.autodealer.crm.modules.commerce.promotion.application.api.dto.UpdateProductPromotionRequest;
import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotion;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.commerce.promotion.application.api.ProductPromotionService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-promotions")
public class ProductPromotionController {

    @Autowired
    private ProductPromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_LIST + "')")
    public Result<PageInfo<TProductPromotion>> getPromotionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.OK(promotionService.getPromotionList(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_VIEW + "')")
    public Result<TProductPromotion> getPromotionById(@PathVariable Long id) {
        return Result.OK(promotionService.getPromotionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_ADD + "')")
    public Result<Void> addPromotion(@Valid @RequestBody CreateProductPromotionRequest req) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setProductId(req.getProductId());
        promotion.setCode(req.getCode());
        promotion.setName(req.getName());
        promotion.setType(req.getType());
        promotion.setDiscount(req.getDiscount());
        promotion.setRuleSummary(req.getRuleSummary());
        promotion.setApplicableStore(req.getApplicableStore());
        promotion.setCustomerType(req.getCustomerType());
        promotion.setApplicableChannel(req.getApplicableChannel());
        promotion.setInventoryScope(req.getInventoryScope());
        promotion.setStackable(req.getStackable());
        promotion.setPriority(req.getPriority());
        promotion.setBudgetLimit(req.getBudgetLimit());
        promotion.setUsageLimit(req.getUsageLimit());
        promotion.setStartTime(req.getStartTime());
        promotion.setEndTime(req.getEndTime());
        promotionService.addPromotion(promotion);
        return Result.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_EDIT + "')")
    public Result<Void> updatePromotion(@PathVariable Long id, @Valid @RequestBody UpdateProductPromotionRequest req) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(id);
        promotion.setProductId(req.getProductId());
        promotion.setCode(req.getCode());
        promotion.setName(req.getName());
        promotion.setType(req.getType());
        promotion.setDiscount(req.getDiscount());
        promotion.setRuleSummary(req.getRuleSummary());
        promotion.setApplicableStore(req.getApplicableStore());
        promotion.setCustomerType(req.getCustomerType());
        promotion.setApplicableChannel(req.getApplicableChannel());
        promotion.setInventoryScope(req.getInventoryScope());
        promotion.setStackable(req.getStackable());
        promotion.setPriority(req.getPriority());
        promotion.setBudgetLimit(req.getBudgetLimit());
        promotion.setUsageLimit(req.getUsageLimit());
        promotion.setStartTime(req.getStartTime());
        promotion.setEndTime(req.getEndTime());
        promotionService.updatePromotion(promotion);
        return Result.OK();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_STATUS + "')")
    public Result<TProductPromotion> publishPromotion(@PathVariable Long id) {
        return Result.OK(promotionService.publishPromotion(id));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_STATUS + "')")
    public Result<TProductPromotion> activatePromotion(@PathVariable Long id) {
        return Result.OK(promotionService.activatePromotion(id));
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_STATUS + "')")
    public Result<TProductPromotion> pausePromotion(@PathVariable Long id,
                                               @Valid @RequestBody ProductPromotionLifecycleRequest req) {
        return Result.OK(promotionService.pausePromotion(id, req.getReason()));
    }

    @PutMapping("/{id}/end")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_STATUS + "')")
    public Result<TProductPromotion> endPromotion(@PathVariable Long id,
                                             @Valid @RequestBody ProductPromotionLifecycleRequest req) {
        return Result.OK(promotionService.endPromotion(id, req.getReason()));
    }

    @PutMapping("/{id}/void")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_STATUS + "')")
    public Result<TProductPromotion> voidPromotion(@PathVariable Long id,
                                              @Valid @RequestBody ProductPromotionLifecycleRequest req) {
        return Result.OK(promotionService.voidPromotion(id, req.getReason()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_DELETE + "')")
    public Result<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return Result.OK();
    }
}
