package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateFollowTaskRequest {
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 128, message = "任务标题不能超过128个字符")
    private String title;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "关联对象类型不能为空")
    private String relatedObjectType;

    @NotNull(message = "关联对象ID不能为空")
    private Long relatedObjectId;

    @NotNull(message = "负责人不能为空")
    private Integer ownerId;

    private String priority;

    @NotNull(message = "计划时间不能为空")
    private LocalDateTime dueTime;

    private LocalDateTime remindTime;
}
