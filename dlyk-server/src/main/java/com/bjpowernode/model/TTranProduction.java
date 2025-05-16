package com.bjpowernode.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 交易产品生产状态表
 * t_tran_production
 */
@Data
public class TTranProduction implements Serializable {
    /**
     * 主键，自动增长，生产状态ID
     */
    private Integer id;

    /**
     * 交易产品ID
     */
    private Integer tranProductId;

    /**
     * 生产状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 更新人
     */
    private Integer updateBy;

    private static final long serialVersionUID = 1L;
} 