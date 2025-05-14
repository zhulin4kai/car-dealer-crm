package com.bjpowernode.mapper;

import com.bjpowernode.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {
    List<Product> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    Integer selectCount();
    
    Product selectById(@Param("id") Long id);
    
    Product selectBySku(@Param("sku") String sku);
    
    int insert(Product product);
    
    int update(Product product);
    
    int deleteById(@Param("id") Long id);
    
    List<Product> selectStockAlerts(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    List<Product> selectStockAlertsWithFilter(
        @Param("offset") Integer offset, 
        @Param("limit") Integer limit, 
        @Param("sku") String sku, 
        @Param("name") String name, 
        @Param("category") String category
    );
    
    Integer selectStockAlertsCount();
    
    int updateStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    List<Product> selectAllOnSale();
} 