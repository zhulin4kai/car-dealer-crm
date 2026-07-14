package com.autodealer.crm.modules.commerce.catalog.application.api.port;

import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ProductCatalogDataPort {
    List<TProduct> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    Integer selectCount();

    TProduct selectById(@Param("id") Long id);

    TProduct selectBySku(@Param("sku") String sku);

    int insert(TProduct product);

    int update(TProduct product);

    int deleteById(@Param("id") Long id);

    List<TProduct> selectStockAlerts(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<TProduct> selectStockAlertsWithFilter(
        @Param("offset") Integer offset,
        @Param("limit") Integer limit,
        @Param("sku") String sku,
        @Param("name") String name,
        @Param("categoryId") Long categoryId
    );

    Integer selectStockAlertsCount();

    int updateStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    List<TProduct> selectAllOnSale();

    int countByCategoryId(@Param("categoryId") Long categoryId);
}
