package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TProductMapper {
    List<TProduct> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    Integer selectCount();
    
    TProduct selectById(@Param("id") Long id);
    
    TProduct selectBySku(@Param("sku") String sku);
    
    int insert(TProduct product);
    
    int update(TProduct product);
    
    int deleteById(@Param("id") Long id);
    
    List<TProduct> selectStockAlerts(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    List<TProduct> selectStockAlertsWithFilter(
        @Param("offset") Integer offset, 
        @Param("limit") Integer limit, 
        @Param("sku") String sku, 
        @Param("name") String name, 
        @Param("category") String category
    );
    
    Integer selectStockAlertsCount();
    
    int updateStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    List<TProduct> selectAllOnSale();
} 