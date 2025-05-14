package com.bjpowernode.web;

import com.bjpowernode.model.ProductPromotion;
import com.bjpowernode.result.Result;
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
    public Result<PageInfo<ProductPromotion>> getPromotionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(promotionService.getPromotionList(page, size));
    }
    
    @GetMapping("/{id}")
    public Result<ProductPromotion> getPromotionById(@PathVariable Long id) {
        return Result.success(promotionService.getPromotionById(id));
    }
    
    @PostMapping
    public Result<Void> addPromotion(@RequestBody ProductPromotion promotion) {
        promotionService.addPromotion(promotion);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> updatePromotion(@PathVariable Long id, @RequestBody ProductPromotion promotion) {
        promotion.setId(id);
        promotionService.updatePromotion(promotion);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return Result.success();
    }
}