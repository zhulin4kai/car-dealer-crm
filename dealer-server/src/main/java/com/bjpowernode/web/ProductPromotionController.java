package com.bjpowernode.web;

import com.bjpowernode.model.ProductPromotion;
import com.bjpowernode.result.R;
import com.bjpowernode.service.ProductPromotionService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-promotions")
public class ProductPromotionController {
    
    @Autowired
    private ProductPromotionService promotionService;
    
    @GetMapping
    public R<PageInfo<ProductPromotion>> getPromotionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(promotionService.getPromotionList(page, size));
    }
    
    @GetMapping("/{id}")
    public R<ProductPromotion> getPromotionById(@PathVariable Long id) {
        return R.OK(promotionService.getPromotionById(id));
    }
    
    @PostMapping
    public R<Void> addPromotion(@RequestBody ProductPromotion promotion) {
        promotionService.addPromotion(promotion);
        return R.OK();
    }
    
    @PutMapping("/{id}")
    public R<Void> updatePromotion(@PathVariable Long id, @RequestBody ProductPromotion promotion) {
        promotion.setId(id);
        promotionService.updatePromotion(promotion);
        return R.OK();
    }
    
    @DeleteMapping("/{id}")
    public R<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return R.OK();
    }
}