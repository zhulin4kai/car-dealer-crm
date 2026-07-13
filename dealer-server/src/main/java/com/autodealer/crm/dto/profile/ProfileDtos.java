package com.autodealer.crm.dto.profile;

import com.autodealer.crm.model.TPermission;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public final class ProfileDtos {
    private ProfileDtos() {}
    @Data public static class RoleItem { private Integer id;private String code;private String name;private String sourceDescription; }
    @Data public static class PermissionSourceOrganization {
        private Integer id;private String code;private String name;
    }
    @Data public static class PermissionSourceDetail {
        private String sourceType;private String sourceName;private String dataScopeCode;private String dataScopeLabel;
        private LocalDateTime effectiveFrom;private LocalDateTime effectiveTo;
        private List<PermissionSourceOrganization> organizations=new ArrayList<>();
    }
    @Data public static class PermissionSource {
        private String permissionCode;private String permissionName;
        /** @deprecated 兼容旧客户端；新客户端应读取 sources，避免来源与数据范围失去配对。 */
        @Deprecated private List<String> sourceNames=new ArrayList<>();
        /** @deprecated 兼容旧客户端；多个来源的范围不能压平成一个字符串。 */
        @Deprecated private String dataScopeLabel;
        private List<PermissionSourceDetail> sources=new ArrayList<>();private LocalDateTime effectiveTo;
    }
    @Data public static class Profile {
        private Integer id;private String loginAct;private String name;private String phone;private String email;private String avatarUrl;
        private Boolean phoneVerified;private Boolean emailVerified;
        private String employeeNo;private String employmentStatus;private String organizationName;private String positionName;private String managerName;
        private List<RoleItem> roles=new ArrayList<>();private List<PermissionSource> effectivePermissions=new ArrayList<>();private Integer profileVersion;
    }
    @Data public static class UpdateRequest {
        @NotNull @Min(0) private Integer profileVersion;
        @NotBlank @Size(max=50) private String name;
        @Size(max=18) private String phone;
        @Email @Size(max=64) private String email;
        @Size(max=500) @Pattern(regexp="^https?://.*$",message="头像地址必须使用 http(s)") private String avatarUrl;
        @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("个人资料不允许字段: "+name);}
    }
    @Data public static class LoginInfo {
        private Integer id;private String loginAct;private String name;private String phone;private String email;private String avatarUrl;
        private Boolean mustChangePassword;private Boolean protectedRecoveryAccount;private String userManagementGateState;
        private List<String> roleList;private List<String> permissionList;private List<TPermission> menuPermissionList;
    }
}
