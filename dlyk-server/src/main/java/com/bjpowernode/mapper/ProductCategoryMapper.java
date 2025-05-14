package com.bjpowernode.mapper;

import com.bjpowernode.model.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductCategoryMapper {
    List<ProductCategory> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    Integer selectCount();
    
    ProductCategory selectById(@Param("id") Long id);
    
    ProductCategory selectByCode(@Param("code") String code);
    
    int insert(ProductCategory category);
    
    int update(ProductCategory category);
    
    int deleteById(@Param("id") Long id);
} 