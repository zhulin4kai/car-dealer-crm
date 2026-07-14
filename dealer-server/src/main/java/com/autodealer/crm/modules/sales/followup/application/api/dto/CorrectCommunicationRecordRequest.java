package com.autodealer.crm.modules.sales.followup.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CorrectCommunicationRecordRequest {
    @NotBlank(message = "沟通方式不能为空")
    private String communicationMethod;

    private LocalDateTime communicationTime;

    @NotBlank(message = "沟通摘要不能为空")
    @Size(max = 500, message = "沟通摘要不能超过500个字符")
    private String summary;

    @Size(max = 500, message = "客户反馈不能超过500个字符")
    private String customerFeedback;

    @Size(max = 500, message = "下一步动作不能超过500个字符")
    private String nextAction;

    private LocalDateTime nextFollowTime;

    @NotBlank(message = "更正原因不能为空")
    @Size(max = 500, message = "更正原因不能超过500个字符")
    private String correctionReason;
}
