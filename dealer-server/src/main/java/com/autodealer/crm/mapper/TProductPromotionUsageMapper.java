package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TProductPromotionUsage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TProductPromotionUsageMapper {
    int insert(TProductPromotionUsage usage);
}
