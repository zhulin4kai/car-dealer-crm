package com.autodealer.crm.modules.sales.testdrive.application.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompleteTestDriveRequest {
    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    @AssertTrue(message = "试驾完成前必须完成安全确认")
    private Boolean safetyConfirmed;

    @NotBlank(message = "试驾结果不能为空")
    private String result;

    @NotBlank(message = "客户反馈不能为空")
    private String customerFeedback;

    @NotBlank(message = "下一步动作不能为空")
    private String nextAction;
}
