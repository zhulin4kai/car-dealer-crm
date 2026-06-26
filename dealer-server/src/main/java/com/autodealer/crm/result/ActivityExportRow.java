package com.autodealer.crm.result;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivityExportRow {
    @ExcelProperty("活动ID")
    private Integer activityId;

    @ExcelProperty("活动名称")
    private String activityName;

    @ExcelProperty("状态")
    private String statusName;

    @ExcelProperty("负责人")
    private String ownerName;

    @ExcelProperty("渠道")
    private String channel;

    @ExcelProperty("目标车型")
    private String targetModel;

    @ExcelProperty("预算")
    private BigDecimal plannedCost;

    @ExcelProperty("实际成本")
    private BigDecimal actualCost;

    @ExcelProperty("线索数")
    private Integer clueCount;

    @ExcelProperty("客户数")
    private Integer customerCount;

    @ExcelProperty("商机数")
    private Integer opportunityCount;

    @ExcelProperty("试驾数")
    private Integer testDriveCount;

    @ExcelProperty("报价数")
    private Integer quoteCount;

    @ExcelProperty("订单数")
    private Integer orderCount;

    @ExcelProperty("成交金额")
    private BigDecimal dealAmount;

    @ExcelProperty("ROI")
    private BigDecimal roi;

    @ExcelProperty("复盘结果")
    private String resultSummary;

    public void setActivityName(String value) { this.activityName = sanitize(value); }
    public void setStatusName(String value) { this.statusName = sanitize(value); }
    public void setOwnerName(String value) { this.ownerName = sanitize(value); }
    public void setChannel(String value) { this.channel = sanitize(value); }
    public void setTargetModel(String value) { this.targetModel = sanitize(value); }
    public void setResultSummary(String value) { this.resultSummary = sanitize(value); }

    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }
}
