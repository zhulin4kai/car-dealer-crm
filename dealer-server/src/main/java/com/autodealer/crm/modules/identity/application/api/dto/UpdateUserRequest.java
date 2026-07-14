package com.autodealer.crm.modules.identity.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotNull(message = "用户ID不能为空")
    private Integer id;

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 1, max = 32, message = "登录账号长度为1-32位")
    private String loginAct;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 1, max = 32, message = "姓名长度为1-32位")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式有误")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式有误")
    private String email;
}
