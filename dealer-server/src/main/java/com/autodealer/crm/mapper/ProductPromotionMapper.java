package com.autodealer.crm.mapper;

import com.autodealer.crm.model.ProductPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductPromotionMapper {
    List<ProductPromotion> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    Integer selectCount();
    
    ProductPromotion selectById(@Param("id") Long id);
    
    int insert(ProductPromotion promotion);
    
    int update(ProductPromotion promotion);
    
    int deleteById(@Param("id") Long id);
} 