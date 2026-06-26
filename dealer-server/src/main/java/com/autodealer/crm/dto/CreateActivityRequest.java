package com.autodealer.crm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreateActivityRequest {
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 128, message = "活动名称不能超过128个字符")
    private String name;

    @NotBlank(message = "活动渠道不能为空")
    @Size(max = 64, message = "活动渠道不能超过64个字符")
    private String channel;

    @Size(max = 128, message = "目标车型不能超过128个字符")
    private String targetModel;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date endTime;

    @NotNull(message = "活动预算不能为空")
    @DecimalMin(value = "0.00", message = "活动预算不能小于0")
    private BigDecimal cost;

    @Size(max = 255, message = "活动描述不能超过255个字符")
    private String description;
}
