package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建线索备注/跟踪记录请求 DTO。
 */
public class CreateClueRemarkRequest {

    @NotNull(message = "线索ID不能为空")
    private Integer clueId;

    @NotNull(message = "跟踪方式不能为空")
    private Integer noteWay;

    @NotBlank(message = "跟踪内容不能为空")
    @Size(max = 255, message = "跟踪内容不能超过255个字符")
    private String noteContent;

    public Integer getClueId() {
        return clueId;
    }

    public void setClueId(Integer clueId) {
        this.clueId = clueId;
    }

    public Integer getNoteWay() {
        return noteWay;
    }

    public void setNoteWay(Integer noteWay) {
        this.noteWay = noteWay;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }
}
