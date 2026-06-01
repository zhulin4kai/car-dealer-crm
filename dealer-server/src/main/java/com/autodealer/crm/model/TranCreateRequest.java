package com.autodealer.crm.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 交易创建/更新请求
 */
@Data
public class TranCreateRequest {
    /** 交易ID（更新时使用） */
    private Integer id;
    
    /** 客户ID */
    private Integer customerId;
    
    /** 客户名称 */
    private String customerName;
    
    /** 交易金额 */
    private BigDecimal amount;
    
    /** 产品详情列表 */
    private List<ProductDetail> products;
    
    /** 交易描述 */
    private String description;
    
    /** 预计交付日期 */
    private String expectedDeliveryDate;
    
    /**
     * 产品详情内部类
     */
    @Data
    public static class ProductDetail {
        /** 产品ID */
        private Integer productId;
        
        /** 数量 */
        private Integer quantity;
        
        /** 单价 */
        private BigDecimal price;
    }
} 