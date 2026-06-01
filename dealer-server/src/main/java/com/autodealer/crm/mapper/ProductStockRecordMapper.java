package com.autodealer.crm.mapper;

import com.autodealer.crm.model.ProductStockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductStockRecordMapper {
    List<ProductStockRecord> selectByProductId(@Param("productId") Long productId, 
                                             @Param("offset") Integer offset, 
                                             @Param("limit") Integer limit);
    
    Integer selectCountByProductId(@Param("productId") Long productId);
    
    int insert(ProductStockRecord record);
} 