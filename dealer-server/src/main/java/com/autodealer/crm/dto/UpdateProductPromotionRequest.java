package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新商品促销请求 DTO。
 */
@Data
public class UpdateProductPromotionRequest {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotBlank(message = "促销名称不能为空")
    private String name;

    private String type;

    private BigDecimal discount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;
}
