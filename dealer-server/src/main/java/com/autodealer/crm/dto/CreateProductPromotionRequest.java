package com.autodealer.crm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建商品促销请求 DTO。
 */
@Data
public class CreateProductPromotionRequest {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotBlank(message = "促销编码不能为空")
    private String code;

    @NotBlank(message = "促销名称不能为空")
    private String name;

    @NotBlank(message = "促销类型不能为空")
    private String type;

    @NotNull(message = "优惠值不能为空")
    @DecimalMin(value = "0.00", message = "优惠值不能为负数")
    private BigDecimal discount;

    @NotBlank(message = "促销规则摘要不能为空")
    private String ruleSummary;

    private String applicableStore;

    private String customerType;

    private String applicableChannel;

    private String inventoryScope;

    private Boolean stackable;

    @PositiveOrZero(message = "优先级不能为负数")
    private Integer priority;

    @Positive(message = "预算上限必须大于0")
    private BigDecimal budgetLimit;

    @Positive(message = "使用名额必须大于0")
    private Integer usageLimit;

    @NotNull(message = "促销开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    @NotNull(message = "促销结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime endTime;
}
