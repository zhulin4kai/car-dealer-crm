package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TDeliveryCheckItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TDeliveryCheckItemMapper {
    TDeliveryCheckItem selectById(@Param("id") Long id);

    TDeliveryCheckItem selectByIdForUpdate(@Param("id") Long id);

    List<TDeliveryCheckItem> selectByDeliveryId(@Param("deliveryId") Long deliveryId);

    int countIncompleteByDeliveryId(@Param("deliveryId") Long deliveryId);

    int insert(TDeliveryCheckItem item);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("completedTime") LocalDateTime completedTime,
                     @Param("remark") String remark,
                     @Param("updateTime") LocalDateTime updateTime,
                     @Param("updateBy") Integer updateBy);
}
