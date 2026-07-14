package com.autodealer.crm.modules.commerce.promotion.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductPromotionLifecycleRequest {

    @NotBlank(message = "原因不能为空")
    private String reason;
}
