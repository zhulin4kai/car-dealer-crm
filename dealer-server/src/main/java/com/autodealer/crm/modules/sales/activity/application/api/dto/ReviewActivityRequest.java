package com.autodealer.crm.modules.sales.activity.application.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewActivityRequest {
    @NotNull(message = "实际成本不能为空")
    @DecimalMin(value = "0.00", message = "实际成本不能小于0")
    private BigDecimal actualCost;

    @NotBlank(message = "复盘结果不能为空")
    @Size(max = 500, message = "复盘结果不能超过500个字符")
    private String resultSummary;

    @NotBlank(message = "复盘结论不能为空")
    @Size(max = 500, message = "复盘结论不能超过500个字符")
    private String reviewConclusion;
}
