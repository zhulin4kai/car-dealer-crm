package com.autodealer.crm.modules.commerce.promotion.persistence.mapper;

import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotionUsage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TProductPromotionUsageMapper {
    int insert(TProductPromotionUsage usage);
}
