package com.autodealer.crm.modules.commerce.inventory.application.api.port;

import com.autodealer.crm.modules.commerce.inventory.application.api.model.TProductStockRecord;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface StockRecordDataPort {
    List<TProductStockRecord> selectByProductId(@Param("productId") Long productId,
                                              @Param("offset") Integer offset,
                                              @Param("limit") Integer limit);

    Integer selectCountByProductId(@Param("productId") Long productId);

    TProductStockRecord selectById(@Param("id") Long id);

    TProductStockRecord selectReleaseByRelatedRecordId(@Param("relatedRecordId") Long relatedRecordId);

    TProductStockRecord selectLatestReserveByVehicle(@Param("vehicleId") Long vehicleId,
                                                     @Param("sourceType") String sourceType,
                                                     @Param("sourceId") Long sourceId);

    TProductStockRecord selectOutboundByDelivery(@Param("deliveryId") Long deliveryId);

    int insert(TProductStockRecord record);
}
