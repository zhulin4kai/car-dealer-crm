package com.bjpowernode.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 交易创建请求
 */
@Data
public class TranCreateRequest {
    /** 客户名称 */
    private String customerName;
    
    /** 交易金额 */
    private BigDecimal amount;
    
    /** 产品ID列表 */
    private List<Integer> products;
    
    /** 交易描述 */
    private String description;
    
    /** 预计交付日期 */
    private Date expectedDeliveryDate;
} 