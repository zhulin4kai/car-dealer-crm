package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostponeFollowTaskRequest {
    @NotNull(message = "新的计划时间不能为空")
    private LocalDateTime newDueTime;

    private LocalDateTime remindTime;

    @NotBlank(message = "延期原因不能为空")
    @Size(max = 500, message = "延期原因不能超过500个字符")
    private String reason;
}
