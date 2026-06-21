package com.autodealer.crm.dto;
import lombok.Data;
import java.util.Date;
import java.util.List;
@Data
public class UserDetailResponse {
    private Integer id;
    private String loginAct;
    private String name;
    private String phone;
    private String email;
    private Integer accountNoExpired;
    private Integer credentialsNoExpired;
    private Integer accountNoLocked;
    private Integer accountEnabled;
    private Date createTime;
    private Integer createBy;
    private Date editTime;
    private Integer editBy;
    private Date lastLoginTime;
    private List<String> roleList;
    private List<String> permissionList;
    private UserRef createByDO;
    private UserRef editByDO;
    @Data
    public static class UserRef {
        private Integer id;
        private String name;
    }
}
