package com.autodealer.crm.modules.sales.activity.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivityLifecycleRequest {
    @NotBlank(message = "原因不能为空")
    @Size(max = 500, message = "原因不能超过500个字符")
    private String reason;
}
