package com.autodealer.crm.modules.sales.customer.application.api.dto;

import lombok.Data;

import java.util.Date;

/**
 * 客户详情响应，不暴露 createBy/editBy 审计字段和内部关联。
 */
@Data
public class CustomerDetailResponse {

    private Integer id;

    private Integer clueId;

    private String customerName;

    private String phone;

    private String weixin;

    private String qq;

    private String email;

    private Integer age;

    private String job;

    private String yearIncome;

    private String address;

    private String ownerName;

    private String activityName;

    private String appellationName;

    private String needLoanName;

    private String intentionStateName;

    private String stateName;

    private String sourceName;

    private String originalSourceName;

    private String customerStatus;

    private String customerStatusName;

    private String productName;

    private Long product;

    private String description;

    private Date nextContactTime;

    private Date createTime;
}
