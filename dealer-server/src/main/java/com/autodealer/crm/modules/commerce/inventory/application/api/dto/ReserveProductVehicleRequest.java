package com.autodealer.crm.modules.commerce.inventory.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReserveProductVehicleRequest {
    @NotBlank(message = "占用类型不能为空")
    private String holdType;

    @NotBlank(message = "业务来源类型不能为空")
    private String sourceType;

    @NotNull(message = "业务来源ID不能为空")
    private Long sourceId;

    private LocalDateTime holdUntil;

    @NotBlank(message = "占用原因不能为空")
    private String remark;
}
