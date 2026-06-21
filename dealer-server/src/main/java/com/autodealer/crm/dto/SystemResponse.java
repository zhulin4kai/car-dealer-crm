package com.autodealer.crm.dto;

import com.autodealer.crm.model.TSystem;
import lombok.Data;

/**
 * 系统配置响应，只暴露接口契约需要的字段，不包含审计字段。
 */
@Data
public class SystemResponse {

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

    public static SystemResponse from(TSystem system) {
        if (system == null) {
            return null;
        }
        SystemResponse response = new SystemResponse();
        response.setId(system.getId());
        response.setSystemCode(system.getSystemCode());
        response.setName(system.getName());
        response.setSite(system.getSite());
        response.setLogo(system.getLogo());
        response.setTitle(system.getTitle());
        response.setDescription(system.getDescription());
        response.setKeywords(system.getKeywords());
        response.setShortcuticon(system.getShortcuticon());
        response.setTel(system.getTel());
        response.setWeixin(system.getWeixin());
        response.setEmail(system.getEmail());
        response.setAddress(system.getAddress());
        response.setVersion(system.getVersion());
        response.setCloseMsg(system.getCloseMsg());
        response.setIsopen(system.getIsopen());
        return response;
    }
}
