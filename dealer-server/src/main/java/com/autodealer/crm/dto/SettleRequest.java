package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class SettleRequest {

    private Long promotionId;

    @NotNull(message = "预期版本号不能为空")
    @PositiveOrZero(message = "预期版本号不能小于0")
    private Integer expectedVersion;

    @NotBlank(message = "计价指纹不能为空")
    private String pricingFingerprint;
}
