package com.autodealer.crm.modules.sales.customer.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MergeCustomerRequest {

    @NotNull(message = "被合并客户不能为空")
    private Integer sourceCustomerId;

    @NotBlank(message = "合并原因不能为空")
    @Size(max = 255, message = "合并原因长度不能超过255个字符")
    private String reason;
}
