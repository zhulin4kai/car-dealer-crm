package com.bjpowernode.result;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.bjpowernode.config.converter.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@ExcelIgnoreUnannotated
@Data
public class ClueExcel {

    @ExcelProperty(value = "负责人")
    private Integer ownerId;

    @ExcelProperty(value = "所属活动")
    private Integer activityId;

    @ExcelProperty(value = "姓名")
    private String fullName;

    @ExcelProperty(value = "称呼", converter = AppellationConverter.class)
    private Integer appellation;

    @ExcelProperty(value = "手机号")
    private String phone;

    @ExcelProperty(value = "微信号")
    private String weixin;

    @ExcelProperty(value = "QQ号")
    private String qq;

    @ExcelProperty(value = "邮箱")
    private String email;

    @ExcelProperty(value = "年龄")
    private Integer age;

    @ExcelProperty(value = "职业")
    private String job;

    @ExcelProperty(value = "年收入")
    private BigDecimal yearIncome;

    @ExcelProperty(value = "地址")
    private String address;

    @ExcelProperty(value = "是否贷款", converter = NeedLoanConverter.class)
    private Integer needLoan;

    @ExcelProperty(value = "意向状态", converter = IntentionStateConverter.class)
    private Integer intentionState;

    @ExcelProperty(value = "意向产品", converter = IntentionProductConverter.class)
    private Integer intentionProduct;

    @ExcelProperty(value = "线索状态", converter = StateConverter.class)
    private Integer state;

    @ExcelProperty(value = "线索来源", converter = SourceConverter.class)
    private Integer source;

    @ExcelProperty(value = "线索描述")
    private String description;

    @ExcelProperty(value = "下次联系时间")
    private Date nextContactTime;
}
