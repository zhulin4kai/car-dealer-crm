package com.autodealer.crm.dto;

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

    private String sourceName;

    private String productName;

    private Integer product;

    private String description;

    private Date nextContactTime;

    private Date createTime;
}
