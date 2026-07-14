package com.autodealer.crm.modules.sales.customer.application.api.model;

import com.autodealer.crm.modules.sales.activity.application.api.model.TActivity;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.identity.application.api.model.TUser;

import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;
import com.autodealer.crm.modules.commerce.catalog.application.api.dto.ProductSimpleDTO;
import java.io.Serializable;
import java.math.BigDecimal;
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
     * 当前客户负责人ID
     */
    private Integer ownerId;

    /**
     * 来源活动ID
     */
    private Integer activityId;

    /**
     * 来源活动名称快照
     */
    private String activityNameSnapshot;

    /**
     * 客户姓名或组织名称
     */
    private String customerName;

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
     * 是否需要贷款
     */
    private Integer needLoan;

    /**
     * 意向状态
     */
    private Integer intentionState;

    /**
     * 当前客户来源
     */
    private Integer source;

    /**
     * 原始线索来源快照
     */
    private Integer originalClueSource;

    /**
     * 选购产品
     */
    private Long product;

    /**
     * 客户经营状态
     */
    private String customerStatus;

    /**
     * 合并目标客户ID
     */
    private Integer mergedToCustomerId;

    /**
     * 合并原因
     */
    private String mergeReason;

    /**
     * 合并时间
     */
    private Date mergeTime;

    /**
     * 合并操作人
     */
    private Integer mergeBy;

    /**
     * 客户描述
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
    private TClue clueDO;
    private TUser ownerDO;
    private TActivity activityDO;
    private TDicValue appellationDO;
    private TDicValue needLoanDO;
    private TDicValue intentionStateDO;
    private TDicValue sourceDO;
    private TDicValue originalClueSourceDO;
    private ProductSimpleDTO productDO;

    private static final long serialVersionUID = 1L;
}
