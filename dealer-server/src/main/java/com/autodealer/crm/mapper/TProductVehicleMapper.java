package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TProductVehicle;
import com.autodealer.crm.query.ProductVehicleQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TProductVehicleMapper {
    TProductVehicle selectById(@Param("id") Long id);

    TProductVehicle selectByIdForUpdate(@Param("id") Long id);

    TProductVehicle selectByVin(@Param("vin") String vin);

    TProductVehicle selectActiveBySource(@Param("sourceType") String sourceType,
                                         @Param("sourceId") Long sourceId);

    List<TProductVehicle> selectPage(@Param("query") ProductVehicleQuery query);

    int countByProductId(@Param("productId") Long productId);

    int insert(TProductVehicle vehicle);

    int reserveIfAvailable(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("holdType") String holdType,
                           @Param("sourceType") String sourceType,
                           @Param("sourceId") Long sourceId,
                           @Param("holdUntil") LocalDateTime holdUntil,
                           @Param("updateTime") LocalDateTime updateTime,
                           @Param("updateBy") Integer updateBy);

    int releaseIfCurrent(@Param("id") Long id,
                         @Param("currentStatus") String currentStatus,
                         @Param("updateTime") LocalDateTime updateTime,
                         @Param("updateBy") Integer updateBy);

    int outboundIfCurrent(@Param("id") Long id,
                          @Param("currentStatus") String currentStatus,
                          @Param("updateTime") LocalDateTime updateTime,
                          @Param("updateBy") Integer updateBy);
}
