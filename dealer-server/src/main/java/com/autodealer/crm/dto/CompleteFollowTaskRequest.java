package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompleteFollowTaskRequest {
    @NotBlank(message = "沟通方式不能为空")
    private String communicationMethod;

    private LocalDateTime communicationTime;

    @NotBlank(message = "跟进摘要不能为空")
    @Size(max = 500, message = "跟进摘要不能超过500个字符")
    private String summary;

    @Size(max = 500, message = "客户反馈不能超过500个字符")
    private String customerFeedback;

    @NotBlank(message = "完成结果不能为空")
    @Size(max = 500, message = "完成结果不能超过500个字符")
    private String result;

    @Size(max = 500, message = "下一步动作不能超过500个字符")
    private String nextAction;

    private LocalDateTime nextFollowTime;
    private Boolean createNextTask;
    private String nextTaskType;
    private String nextTaskTitle;
    private String nextTaskPriority;
    private LocalDateTime nextTaskDueTime;
    private LocalDateTime nextTaskRemindTime;
}
