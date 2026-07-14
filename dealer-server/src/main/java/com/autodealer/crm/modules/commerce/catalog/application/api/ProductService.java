package com.autodealer.crm.modules.commerce.catalog.application.api;

import com.autodealer.crm.modules.commerce.catalog.application.api.dto.ProductSimpleDTO;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.github.pagehelper.PageInfo;
import java.util.List;

public interface ProductService {
    PageInfo<TProduct> getProductList(Integer pageNum, Integer pageSize);

    TProduct getProductById(Long id);

    TProduct getProductBySku(String sku);

    void addProduct(TProduct product);

    void updateProduct(TProduct product);

    void deleteProduct(Long id);

    PageInfo<TProduct> getStockAlerts(Integer pageNum, Integer pageSize);

    PageInfo<TProduct> getStockAlerts(Integer pageNum, Integer pageSize, String sku, String name, Long categoryId);

    void restock(Long productId, Integer quantity, String remark);

    void updateStock(Long id, Integer quantity);

    List<ProductSimpleDTO> getAllOnSaleProduct();
}
