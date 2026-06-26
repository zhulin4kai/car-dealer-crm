package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryCancelRequest {
    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
