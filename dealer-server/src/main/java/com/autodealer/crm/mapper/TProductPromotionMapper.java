package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TProductPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TProductPromotionMapper {
    List<TProductPromotion> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<TProductPromotion> selectAvailableByProductIds(@Param("productIds") List<Long> productIds,
                                                        @Param("now") LocalDateTime now);
    
    Integer selectCount();
    
    TProductPromotion selectById(@Param("id") Long id);
    
    int insert(TProductPromotion promotion);
    
    int update(TProductPromotion promotion);
    
    int deleteById(@Param("id") Long id);
}
