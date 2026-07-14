package com.autodealer.crm.modules.sales.lead.application.api.model;

import com.autodealer.crm.modules.sales.activity.application.api.model.TActivity;
import com.autodealer.crm.modules.identity.application.api.model.TUser;

import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;
import com.autodealer.crm.modules.commerce.catalog.application.api.dto.ProductSimpleDTO;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 线索表
 * t_clue
 */
@Data
public class TClue implements Serializable {

    /**
     * 主键，自动增长，线索ID
     */
    private Integer id;

    /**
     * 线索所属人ID
     */
    private Integer ownerId;

    /**
     * 活动ID
     */
    private Integer activityId;

    /**
     * 来源活动名称快照
     */
    private String activityNameSnapshot;

    /**
     * 姓名
     */
    private String fullName;

    /**
     * 称呼
     */
    private Integer appellation;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 微信号
     */
    private String weixin;

    /**
     * QQ号
     */
    private String qq;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 职业
     */
    private String job;

    /**
     * 年收入
     */
    private BigDecimal yearIncome;

    /**
     * 地址
     */
    private String address;

    /**
     * 是否需要贷款（0不需要，1需要）
     */
    private Integer needLoan;

    /**
     * 意向状态
     */
    private Integer intentionState;

    /**
     * 意向产品
     */
    private Integer intentionProduct;

    /**
     * 线索状态
     */
    private Integer state;

    /**
     * 线索来源
     */
    private Integer source;

    /**
     * 线索描述
     */
    private String description;

    /**
     * 下次联系时间
     */
    private Date nextContactTime;

    /**
     * 最近跟进时间
     */
    private Date lastFollowTime;

    /**
     * 最近跟进摘要
     */
    private String lastFollowSummary;

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
    private TUser ownerDO;
    private TActivity activityDO;
    private TDicValue appellationDO;
    private TDicValue needLoanDO;
    private TDicValue intentionStateDO;
    private ProductSimpleDTO intentionProductDO;
    private TDicValue stateDO;
    private TDicValue sourceDO;

    private static final long serialVersionUID = 1L;
}
