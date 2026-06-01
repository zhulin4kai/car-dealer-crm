package com.autodealer.crm.model;

import lombok.Data;

/**
 * 客户选项DTO（用于下拉选择）
 */
@Data
public class CustomerOption {
    /** 客户ID */
    private Integer customerId;
    
    /** 客户名称（来自线索表的full_name） */
    private String customerName;
    
    /** 线索ID */
    private Integer clueId;
} 