package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TDelivery;
import com.autodealer.crm.query.DeliveryQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TDeliveryMapper {
    TDelivery selectById(@Param("id") Long id);

    TDelivery selectByIdForUpdate(@Param("id") Long id);

    TDelivery selectActiveByTranId(@Param("tranId") Integer tranId);

    List<TDelivery> selectByTranId(@Param("tranId") Integer tranId);

    List<TDelivery> selectPage(@Param("query") DeliveryQuery query);

    int insert(TDelivery delivery);

    int markPreparing(@Param("id") Long id,
                      @Param("updateTime") LocalDateTime updateTime,
                      @Param("updateBy") Integer updateBy);

    int signIfCurrent(@Param("id") Long id,
                      @Param("currentStatus") String currentStatus,
                      @Param("actualDeliveryTime") LocalDateTime actualDeliveryTime,
                      @Param("signerName") String signerName,
                      @Param("signedAt") LocalDateTime signedAt,
                      @Param("signMethod") String signMethod,
                      @Param("signEvidence") String signEvidence,
                      @Param("updateTime") LocalDateTime updateTime,
                      @Param("updateBy") Integer updateBy);

    int markExceptionIfNotTerminal(@Param("id") Long id,
                                   @Param("exceptionType") String exceptionType,
                                   @Param("exceptionReason") String exceptionReason,
                                   @Param("updateTime") LocalDateTime updateTime,
                                   @Param("updateBy") Integer updateBy);

    int cancelIfNotTerminal(@Param("id") Long id,
                            @Param("reason") String reason,
                            @Param("updateTime") LocalDateTime updateTime,
                            @Param("updateBy") Integer updateBy);
}
