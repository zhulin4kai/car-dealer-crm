package com.autodealer.crm.dto;

import lombok.Data;

/**
 * 更新系统配置请求，字段均为可选，未提交字段不覆盖。
 * ID、审计字段、版本等服务端字段不可写。
 */
@Data
public class UpdateSystemRequest {

    private String systemCode;
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
}
