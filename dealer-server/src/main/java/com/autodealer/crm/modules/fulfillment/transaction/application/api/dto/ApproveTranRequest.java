package com.autodealer.crm.modules.fulfillment.transaction.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 交易审批请求。
 */
@Data
public class ApproveTranRequest {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @NotBlank(message = "审批意见不能为空")
    private String comment;
}
