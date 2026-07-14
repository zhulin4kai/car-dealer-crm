package com.autodealer.crm.modules.sales.testdrive.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTestDriveRequest {
    @NotNull(message = "客户ID不能为空")
    private Integer customerId;

    private Long opportunityId;

    @NotNull(message = "试驾车辆不能为空")
    private Long vehicleId;

    @NotNull(message = "预约开始时间不能为空")
    private LocalDateTime plannedStartTime;

    @NotNull(message = "预约结束时间不能为空")
    private LocalDateTime plannedEndTime;

    @NotBlank(message = "客户联系人不能为空")
    private String contactName;

    @NotBlank(message = "客户联系电话不能为空")
    private String contactPhone;

    private String remark;
}
