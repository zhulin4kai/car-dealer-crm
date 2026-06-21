package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建系统配置请求，只暴露管理员允许修改的名称、站点、联系方式和展示配置。
 * ID、审计字段、版本等服务端字段不可写。
 */
@Data
public class CreateSystemRequest {

    @NotBlank(message = "系统代码不能为空")
    private String systemCode;

    @NotBlank(message = "系统名称不能为空")
    private String name;

    private String site;
    private String logo;
    private String title;
    private String description;
    private String keywords;
    private String shortcuticon;
    private String tel;
    private String weixin;
    private String email;
    private String address;
    private String version;
    private String closeMsg;
    private String isopen;
}
