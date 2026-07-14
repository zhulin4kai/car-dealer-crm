package com.autodealer.crm.modules.sales.followup.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VoidCommunicationRecordRequest {
    @NotBlank(message = "作废原因不能为空")
    @Size(max = 500, message = "作废原因不能超过500个字符")
    private String reason;
}
