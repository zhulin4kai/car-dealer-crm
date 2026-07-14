package com.autodealer.crm.modules.fulfillment.transaction.application.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建交易请求。
 */
@Data
public class CreateTranRequest {

    @NotNull(message = "客户ID不能为空")
    private Integer customerId;

    @NotEmpty(message = "交易产品不能为空")
    @Valid
    private List<TranProductItemRequest> products;

    private String description;

    private LocalDateTime expectedDeliveryDate;
}
