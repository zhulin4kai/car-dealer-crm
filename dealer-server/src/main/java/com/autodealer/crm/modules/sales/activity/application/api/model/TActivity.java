package com.autodealer.crm.modules.sales.activity.application.api.model;

import com.autodealer.crm.modules.identity.application.api.model.TUser;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 市场活动表
 * t_activity
 */
@Data
public class TActivity implements Serializable {

    /**
     * 主键，自动增长，活动ID
     */
    private Integer id;

    /**
     * 活动所属人ID
     */
    private Integer ownerId;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动状态稳定编码
     */
    private String status;

    /**
     * 活动渠道
     */
    private String channel;

    /**
     * 目标车型
     */
    private String targetModel;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 活动预算
     */
    private BigDecimal cost;

    /**
     * 活动实际成本
     */
    private BigDecimal actualCost;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 复盘结果摘要
     */
    private String resultSummary;

    /**
     * 复盘结论
     */
    private String reviewConclusion;

    /**
     * 复盘人
     */
    private Integer reviewedBy;

    /**
     * 复盘时间
     */
    private Date reviewedTime;

    /**
     * 关闭原因
     */
    private String closedReason;

    /**
     * 取消原因
     */
    private String canceledReason;

    /**
     * 活动创建时间
     */
    private Date createTime;

    /**
     * 活动创建人
     */
    private Integer createBy;

    /**
     * 活动编辑时间
     */
    private Date editTime;

    /**
     * 活动编辑人
     */
    private Integer editBy;

    /**
     * 一对一关联
     */
    private TUser ownerDO;
    private TUser createByDO;
    private TUser editByDO;
    private TUser reviewedByDO;

    private static final long serialVersionUID = 1L;
}
