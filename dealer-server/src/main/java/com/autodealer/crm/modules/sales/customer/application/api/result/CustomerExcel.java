package com.autodealer.crm.modules.sales.customer.application.api.result;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CustomerExcel {

    @ExcelProperty(value = "所属人")
    private String ownerName;

    @ExcelProperty(value = "所属活动")
    private String activityName;

    @ExcelProperty(value = "客户姓名")
    private String fullName;

    @ExcelProperty(value = "客户称呼")
    private String appellationName;

    @ExcelProperty(value = "客户手机")
    private String phone;

    @ExcelProperty(value = "客户微信")
    private String weixin;

    @ExcelProperty(value = "客户QQ")
    private String qq;

    @ExcelProperty(value = "客户邮箱")
    private String email;

    @ExcelProperty(value = "客户年龄")
    private int age;

    @ExcelProperty(value = "客户职业")
    private String job;

    @ExcelProperty(value = "客户年收入")
    private BigDecimal yearIncome;

    @ExcelProperty(value = "客户住址")
    private String address;

    @ExcelProperty(value = "是否贷款")
    private String needLoanName;

    @ExcelProperty(value = "客户产品")
    private String productName;

    @ExcelProperty(value = "客户来源")
    private String sourceName;

    @ExcelProperty(value = "客户描述")
    private String description;

    @ExcelProperty(value = "下次联系时间")
    private Date nextContactTime;

    public void setOwnerName(String v) { this.ownerName = sanitize(v); }
    public void setActivityName(String v) { this.activityName = sanitize(v); }
    public void setFullName(String v) { this.fullName = sanitize(v); }
    public void setAppellationName(String v) { this.appellationName = sanitize(v); }
    public void setPhone(String v) { this.phone = sanitize(v); }
    public void setWeixin(String v) { this.weixin = sanitize(v); }
    public void setQq(String v) { this.qq = sanitize(v); }
    public void setEmail(String v) { this.email = sanitize(v); }
    public void setJob(String v) { this.job = sanitize(v); }
    public void setAddress(String v) { this.address = sanitize(v); }
    public void setNeedLoanName(String v) { this.needLoanName = sanitize(v); }
    public void setProductName(String v) { this.productName = sanitize(v); }
    public void setSourceName(String v) { this.sourceName = sanitize(v); }
    public void setDescription(String v) { this.description = sanitize(v); }

    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) { return value; }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') { return "'" + value; }
        return value;
    }
}
