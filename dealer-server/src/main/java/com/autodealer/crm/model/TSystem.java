package com.autodealer.crm.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TSystem {
    private Integer id;
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
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;
} 