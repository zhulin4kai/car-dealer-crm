package com.autodealer.crm.modules.identity.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HandoverUserResponsibilitiesRequest {

    @NotNull(message = "目标负责人不能为空")
    private Integer targetUserId;

    @NotBlank(message = "交接原因不能为空")
    @Size(max = 200, message = "交接原因不能超过200个字符")
    private String reason;
}
