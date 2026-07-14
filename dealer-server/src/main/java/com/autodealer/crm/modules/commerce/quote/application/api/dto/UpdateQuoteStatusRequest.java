package com.autodealer.crm.modules.commerce.quote.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateQuoteStatusRequest {
    @NotBlank(message = "当前报价状态不能为空")
    private String expectedStatus;

    @NotBlank(message = "目标报价状态不能为空")
    private String targetStatus;

    @NotBlank(message = "状态变更原因不能为空")
    private String reason;

    @Size(max = 100, message = "确认人姓名不能超过100个字符")
    private String confirmedByName;

    private LocalDateTime confirmedAt;

    @Size(max = 50, message = "确认方式不能超过50个字符")
    private String confirmationMethod;

    @Size(max = 500, message = "确认凭证不能超过500个字符")
    private String confirmationEvidence;

    @Size(max = 500, message = "代确认原因不能超过500个字符")
    private String proxyConfirmReason;
}
