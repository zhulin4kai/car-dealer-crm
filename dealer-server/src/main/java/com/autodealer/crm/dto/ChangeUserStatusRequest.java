package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeUserStatusRequest {

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotNull(message = "目标状态不能为空")
    private Integer enabled;
}
