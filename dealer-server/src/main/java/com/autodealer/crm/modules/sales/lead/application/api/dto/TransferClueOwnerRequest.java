package com.autodealer.crm.modules.sales.lead.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferClueOwnerRequest {

    @NotNull(message = "目标负责人不能为空")
    private Integer newOwnerId;

    @NotBlank(message = "转派原因不能为空")
    private String reason;
}
