package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleTestDriveRequest {
    private Long vehicleId;

    @NotNull(message = "新预约开始时间不能为空")
    private LocalDateTime plannedStartTime;

    @NotNull(message = "新预约结束时间不能为空")
    private LocalDateTime plannedEndTime;

    @NotBlank(message = "改期原因不能为空")
    private String reason;
}
