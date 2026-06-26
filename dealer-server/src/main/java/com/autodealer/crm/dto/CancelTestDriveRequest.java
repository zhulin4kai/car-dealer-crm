package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelTestDriveRequest {
    @NotBlank(message = "取消类型不能为空")
    private String cancelType;

    @NotBlank(message = "原因不能为空")
    private String reason;
}
