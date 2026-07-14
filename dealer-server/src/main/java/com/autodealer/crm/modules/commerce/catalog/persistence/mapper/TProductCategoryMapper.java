package com.autodealer.crm.modules.commerce.catalog.persistence.mapper;

import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TProductCategoryMapper {
    List<TProductCategory> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    Integer selectCount();

    TProductCategory selectById(@Param("id") Long id);

    TProductCategory selectByCode(@Param("code") String code);

    int insert(TProductCategory category);

    int update(TProductCategory category);

    int deleteById(@Param("id") Long id);
}
