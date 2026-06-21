package com.autodealer.crm.result;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 线索 Excel 导入原始数据 DTO。
 *
 * <p>字典和商品相关列保留原始文本，不在此阶段做 ID 转换。
 * 转换和校验由 {@link com.autodealer.crm.service.ClueImportValidator} 统一完成。
 */
@ExcelIgnoreUnannotated
@Data
public class ClueExcelRaw {

    @ExcelProperty(value = "负责人")
    private String ownerName;

    @ExcelProperty(value = "所属活动")
    private String activityName;

    @ExcelProperty(value = "姓名")
    private String fullName;

    @ExcelProperty(value = "称呼")
    private String appellation;

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

    @ExcelProperty(value = "是否贷款")
    private String needLoan;

    @ExcelProperty(value = "意向状态")
    private String intentionState;

    @ExcelProperty(value = "意向产品")
    private String intentionProduct;

    @ExcelProperty(value = "线索状态")
    private String state;

    @ExcelProperty(value = "线索来源")
    private String source;

    @ExcelProperty(value = "线索描述")
    private String description;

    @ExcelProperty(value = "下次联系时间")
    private Date nextContactTime;
}
