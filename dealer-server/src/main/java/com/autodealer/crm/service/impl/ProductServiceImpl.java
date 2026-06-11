package com.autodealer.crm.service.impl;

import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.service.ProductService;
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
    private TProductMapper productMapper;
    
    @Autowired
    private TProductStockRecordMapper stockRecordMapper;
    
    @Override
    public PageInfo<TProduct> getProductList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProduct> products = productMapper.selectList((pageNum - 1) * pageSize, pageSize);
        return new PageInfo<>(products);
    }
    
    @Override
    public TProduct getProductById(Long id) {
        return productMapper.selectById(id);
    }
    
    @Override
    public TProduct getProductBySku(String sku) {
        return productMapper.selectBySku(sku);
    }
    
    @Override
    @Transactional
    public void addProduct(TProduct product) {
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
    }
    
    @Override
    @Transactional
    public void updateProduct(TProduct product) {
        product.setUpdateTime(LocalDateTime.now());
        productMapper.update(product);
    }
    
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }
    
    @Override
    public PageInfo<TProduct> getStockAlerts(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProduct> products = productMapper.selectStockAlerts(null, null);
        return new PageInfo<>(products);
    }
    
    @Override
    public PageInfo<TProduct> getStockAlerts(Integer pageNum, Integer pageSize, String sku, String name, String category) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProduct> products = productMapper.selectStockAlertsWithFilter(
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
        TProductStockRecord record = new TProductStockRecord();
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
    public List<ProductSimpleDTO> getAllOnSaleProduct() {
        List<TProduct> products = productMapper.selectAllOnSale();
        return products.stream()
                .map(this::convertToProductSimpleDTO)
                .collect(Collectors.toList());
    }
    
    private ProductSimpleDTO convertToProductSimpleDTO(TProduct product) {
        ProductSimpleDTO dto = new ProductSimpleDTO();
        dto.setId(product.getId().intValue());
        dto.setName(product.getName());
        dto.setGuidePriceS(product.getPrice());
        dto.setGuidePriceE(product.getPrice());
        dto.setQuotation(product.getPrice());
        dto.setState("on_sale".equals(product.getStatus()) ? 0 : 1);
        dto.setCreateTime(Date.from(product.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        dto.setEditTime(Date.from(product.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant()));
        return dto;
    }
}