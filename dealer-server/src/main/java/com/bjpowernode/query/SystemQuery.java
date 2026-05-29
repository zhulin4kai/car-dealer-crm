package com.bjpowernode.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemQuery extends BaseQuery {
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
    private String isopen;
} 