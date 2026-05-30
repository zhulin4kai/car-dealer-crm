package com.bjpowernode.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 客户查询条件
 */
@Data
public class CustomerQuery {
    /** 客户名称（线索名称） */
    private String customerName;
    
    /** 产品ID */
    private Integer productId;
    
    /** 创建人 */
    private Integer createBy;

    private Integer clueId;

    private Integer product;

    private String description;

    private Date nextContactTime;

    /** 购买数量，默认为1 */
    private Integer quantity = 1;
}