package com.autodealer.crm.modules.commerce.inventory.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReleaseProductVehicleRequest {
    @NotNull(message = "原占用流水ID不能为空")
    private Long reserveRecordId;

    @NotBlank(message = "释放原因不能为空")
    private String reason;
}
