package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.model.TProductStockRecord;
import com.autodealer.crm.service.ProductService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;

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

    @Resource
    private OperationAuditRecorder auditRecorder;

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
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_IN, String.valueOf(product.getId()));
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
    public PageInfo<TProduct> getStockAlerts(Integer pageNum, Integer pageSize, String sku, String name, Long categoryId) {
        PageHelper.startPage(pageNum, pageSize);
        List<TProduct> products = productMapper.selectStockAlertsWithFilter(
            null,
            null,
            sku,
            name,
                categoryId
        );
        return new PageInfo<>(products);
    }

    @Override
    @Transactional
    public void restock(Long productId, Integer quantity, String remark) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("入库产品和正数数量不能为空");
        }
        if (productMapper.updateStock(productId, quantity) != 1) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "产品不存在，入库失败");
        }

        // 记录库存变动
        TProductStockRecord record = new TProductStockRecord();
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setType("入库");
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        if (stockRecordMapper.insert(record) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "库存变动记录创建失败");
        }
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_IN, String.valueOf(productId));
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
