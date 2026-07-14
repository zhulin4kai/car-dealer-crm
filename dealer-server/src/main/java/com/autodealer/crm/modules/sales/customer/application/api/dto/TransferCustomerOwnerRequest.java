package com.autodealer.crm.modules.sales.customer.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TransferCustomerOwnerRequest {

    @NotNull(message = "目标负责人不能为空")
    private Integer newOwnerId;

    @NotBlank(message = "转移原因不能为空")
    @Size(max = 255, message = "转移原因长度不能超过255个字符")
    private String reason;
}
