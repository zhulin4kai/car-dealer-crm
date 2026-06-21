package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.CreateProductPromotionRequest;
import com.autodealer.crm.dto.UpdateProductPromotionRequest;
import com.autodealer.crm.model.TProductPromotion;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductPromotionService;
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
    public R<PageInfo<TProductPromotion>> getPromotionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(promotionService.getPromotionList(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_VIEW + "')")
    public R<TProductPromotion> getPromotionById(@PathVariable Long id) {
        return R.OK(promotionService.getPromotionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_ADD + "')")
    public R<Void> addPromotion(@Valid @RequestBody CreateProductPromotionRequest req) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setProductId(req.getProductId());
        promotion.setName(req.getName());
        promotion.setType(req.getType());
        promotion.setDiscount(req.getDiscount());
        promotion.setStartTime(req.getStartTime());
        promotion.setEndTime(req.getEndTime());
        promotion.setStatus(req.getStatus());
        promotionService.addPromotion(promotion);
        return R.OK();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_EDIT + "')")
    public R<Void> updatePromotion(@PathVariable Long id, @Valid @RequestBody UpdateProductPromotionRequest req) {
        TProductPromotion promotion = new TProductPromotion();
        promotion.setId(id);
        promotion.setProductId(req.getProductId());
        promotion.setName(req.getName());
        promotion.setType(req.getType());
        promotion.setDiscount(req.getDiscount());
        promotion.setStartTime(req.getStartTime());
        promotion.setEndTime(req.getEndTime());
        promotion.setStatus(req.getStatus());
        promotionService.updatePromotion(promotion);
        return R.OK();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRODUCT_PROMOTION_DELETE + "')")
    public R<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return R.OK();
    }
}
