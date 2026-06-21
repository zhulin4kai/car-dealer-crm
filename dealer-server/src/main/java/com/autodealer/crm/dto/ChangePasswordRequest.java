package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 16, message = "登录密码长度为6-16位")
    private String newPassword;
}
