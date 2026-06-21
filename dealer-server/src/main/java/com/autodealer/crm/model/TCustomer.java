package com.autodealer.crm.model;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 客户表
 * t_customer
 */
@Data
public class TCustomer implements Serializable {

    /**
     * 主键，自动增长，客户ID
     */
    private Integer id;

    /**
     * 线索ID
     */
    private Integer clueId;

    /**
     * 选购产品
     */
    private Long product;

    /**
     * 客户描述
     */
    private String description;

    /**
     * 下次联系时间
     */
    private Date nextContactTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 编辑人
     */
    private Integer editBy;

    /**
     * 一对一关联
     */
    private TClue clueDO;

    private static final long serialVersionUID = 1L;
}
