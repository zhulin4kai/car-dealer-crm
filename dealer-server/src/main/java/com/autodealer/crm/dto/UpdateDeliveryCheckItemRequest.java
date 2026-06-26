package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDeliveryCheckItemRequest {
    @NotBlank(message = "准备项状态不能为空")
    private String status;

    private String remark;
}
