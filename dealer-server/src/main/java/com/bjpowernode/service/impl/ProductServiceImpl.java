package com.bjpowernode.service.impl;

import com.bjpowernode.mapper.ProductMapper;
import com.bjpowernode.mapper.ProductStockRecordMapper;
import com.bjpowernode.model.Product;
import com.bjpowernode.model.ProductStockRecord;
import com.bjpowernode.model.TProduct;
import com.bjpowernode.service.ProductService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private ProductStockRecordMapper stockRecordMapper;
    
    @Override
    public PageInfo<Product> getProductList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> products = productMapper.selectList((pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(products);
    }
    
    @Override
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }
    
    @Override
    public Product getProductBySku(String sku) {
        return productMapper.selectBySku(sku);
    }
    
    @Override
    @Transactional
    public void addProduct(Product product) {
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
    }
    
    @Override
    @Transactional
    public void updateProduct(Product product) {
        product.setUpdateTime(LocalDateTime.now());
        productMapper.update(product);
    }
    
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }
    
    @Override
    public PageInfo<Product> getStockAlerts(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> products = productMapper.selectStockAlerts(null, null);
        return new PageInfo<>(products);
    }
    
    @Override
    public PageInfo<Product> getStockAlerts(Integer pageNum, Integer pageSize, String sku, String name, String category) {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> products = productMapper.selectStockAlertsWithFilter(
            null, 
            null, 
            sku, 
            name, 
            category
        );
        return new PageInfo<>(products);
    }
    
    @Override
    @Transactional
    public void restock(Long productId, Integer quantity, String remark) {
        // 更新库存
        productMapper.updateStock(productId, quantity);
        
        // 记录库存变动
        ProductStockRecord record = new ProductStockRecord();
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setType("入库");
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        stockRecordMapper.insert(record);
    }
    
    @Override
    @Transactional
    public void updateStock(Long id, Integer quantity) {
        productMapper.updateStock(id, quantity);
    }
    
    @Override
    public List<TProduct> getAllOnSaleProduct() {
        List<Product> products = productMapper.selectAllOnSale();
        return products.stream()
                .map(this::convertToTProduct)
                .collect(Collectors.toList());
    }
    
    private TProduct convertToTProduct(Product product) {
        TProduct tProduct = new TProduct();
        tProduct.setId(product.getId().intValue());
        tProduct.setName(product.getName());
        tProduct.setGuidePriceS(product.getPrice());
        tProduct.setGuidePriceE(product.getPrice());
        tProduct.setQuotation(product.getPrice());
        tProduct.setState("on_sale".equals(product.getStatus()) ? 0 : 1);
        tProduct.setCreateTime(Date.from(product.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        tProduct.setEditTime(Date.from(product.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant()));
        return tProduct;
    }
}