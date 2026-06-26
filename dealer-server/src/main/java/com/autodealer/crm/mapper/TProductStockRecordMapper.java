package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TProductStockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TProductStockRecordMapper {
    List<TProductStockRecord> selectByProductId(@Param("productId") Long productId, 
                                              @Param("offset") Integer offset, 
                                              @Param("limit") Integer limit);
    
    Integer selectCountByProductId(@Param("productId") Long productId);

    TProductStockRecord selectById(@Param("id") Long id);

    TProductStockRecord selectReleaseByRelatedRecordId(@Param("relatedRecordId") Long relatedRecordId);
    
    int insert(TProductStockRecord record);
}
