package com.autodealer.crm.modules.sales.activity.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建活动备注请求 DTO。
 */
public class CreateActivityRemarkRequest {

    @NotNull(message = "活动ID不能为空")
    private Integer activityId;

    @NotBlank(message = "备注内容不能为空")
    @Size(max = 255, message = "备注内容不能超过255个字符")
    private String noteContent;

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }
}
