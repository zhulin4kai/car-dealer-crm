package com.autodealer.crm.modules.sales.testdrive.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckInTestDriveRequest {
    private LocalDateTime arrivedAt;

    @NotBlank(message = "客户确认方式不能为空")
    private String customerConfirmMethod;
}
