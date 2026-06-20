package com.autodealer.crm.web;

import com.autodealer.crm.model.TProductPromotion;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProductPromotionService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-promotions")
public class ProductPromotionController {
    
    @Autowired
    private ProductPromotionService promotionService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('product:list')")
    public R<PageInfo<TProductPromotion>> getPromotionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(promotionService.getPromotionList(page, size));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:view')")
    public R<TProductPromotion> getPromotionById(@PathVariable Long id) {
        return R.OK(promotionService.getPromotionById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    public R<Void> addPromotion(@RequestBody TProductPromotion promotion) {
        promotionService.addPromotion(promotion);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> updatePromotion(@PathVariable Long id, @RequestBody TProductPromotion promotion) {
        promotion.setId(id);
        promotionService.updatePromotion(promotion);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public R<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return R.OK();
    }
}
