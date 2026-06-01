package com.autodealer.crm.service;

import com.autodealer.crm.model.Product;
import com.autodealer.crm.model.TProduct;
import com.github.pagehelper.PageInfo;
import java.util.List;

public interface ProductService {
    PageInfo<Product> getProductList(Integer pageNum, Integer pageSize);
    
    Product getProductById(Long id);
    
    Product getProductBySku(String sku);
    
    void addProduct(Product product);
    
    void updateProduct(Product product);
    
    void deleteProduct(Long id);
    
    PageInfo<Product> getStockAlerts(Integer pageNum, Integer pageSize);
    
    PageInfo<Product> getStockAlerts(Integer pageNum, Integer pageSize, String sku, String name, String category);
    
    void restock(Long productId, Integer quantity, String remark);
    
    void updateStock(Long id, Integer quantity);
    
    List<TProduct> getAllOnSaleProduct();
}