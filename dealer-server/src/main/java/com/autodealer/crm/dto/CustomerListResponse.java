package com.autodealer.crm.dto;

import lombok.Data;

import java.util.Date;

/**
 * 客户列表响应，字段扁平化，不暴露 createBy/editBy 和内部关联实体。
 */
@Data
public class CustomerListResponse {

    private Integer id;

    private Integer clueId;

    private String customerName;

    private String phone;

    private String weixin;

    private String ownerName;

    private String activityName;

    private String appellationName;

    private String needLoanName;

    private String intentionStateName;

    private String stateName;

    private String sourceName;

    private String intentionProductName;

    private Integer product;

    private String description;

    private Date nextContactTime;

    private Date createTime;
}
