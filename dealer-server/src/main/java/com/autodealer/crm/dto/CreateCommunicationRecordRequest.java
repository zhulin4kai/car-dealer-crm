package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCommunicationRecordRequest {
    private Long followTaskId;

    @NotBlank(message = "关联对象类型不能为空")
    private String relatedObjectType;

    @NotNull(message = "关联对象ID不能为空")
    private Long relatedObjectId;

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
    private Boolean createNextTask;
    private String nextTaskType;
    private String nextTaskTitle;
    private String nextTaskPriority;
    private LocalDateTime nextTaskDueTime;
    private LocalDateTime nextTaskRemindTime;
}
