package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.dto.ProductSimpleDTO;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TProductMapper;
import com.autodealer.crm.mapper.TProductPromotionMapper;
import com.autodealer.crm.mapper.TProductStockRecordMapper;
import com.autodealer.crm.mapper.TProductVehicleMapper;
import com.autodealer.crm.mapper.TTranProductMapper;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Set<String> VALID_PRODUCT_STATUSES = Set.of("ON_SALE", "OFF_SALE");

    @Autowired
    private TProductMapper productMapper;

    @Autowired
    private TProductStockRecordMapper stockRecordMapper;

    @Autowired
    private TProductVehicleMapper productVehicleMapper;

    @Autowired
    private TTranProductMapper tranProductMapper;

    @Autowired
    private TProductPromotionMapper promotionMapper;

    @Autowired
    private TCustomerMapper customerMapper;

    @Autowired
    private TClueMapper clueMapper;

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
        requireValidProductStatus(product);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        auditRecorder.record(AuditActionEnum.PRODUCT_STOCK_IN, String.valueOf(product.getId()));
    }

    @Override
    @Transactional
    public void updateProduct(TProduct product) {
        requireValidProductStatus(product);
        product.setStock(null);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.update(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (productMapper.selectById(id) == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商品不存在");
        }
        if (hasProductReferences(id)) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "商品已被业务引用，不能删除");
        }
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

        TProductStockRecord record = new TProductStockRecord();
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setType("INBOUND");
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
        dto.setState("ON_SALE".equals(product.getStatus()) ? 0 : 1);
        dto.setCreateTime(Date.from(product.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        dto.setEditTime(Date.from(product.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant()));
        return dto;
    }

    private void requireValidProductStatus(TProduct product) {
        if (product == null || product.getStatus() == null
                || !VALID_PRODUCT_STATUSES.contains(product.getStatus())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "商品状态必须使用 ON_SALE 或 OFF_SALE");
        }
    }

    private boolean hasProductReferences(Long productId) {
        return tranProductMapper.countByProductId(productId) > 0
                || safeCount(stockRecordMapper.selectCountByProductId(productId)) > 0
                || productVehicleMapper.countByProductId(productId) > 0
                || promotionMapper.countByProductId(productId) > 0
                || customerMapper.countByProductId(productId) > 0
                || clueMapper.countByIntentionProductId(productId) > 0;
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }
}
