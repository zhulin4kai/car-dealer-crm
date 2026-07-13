package com.autodealer.crm.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

import com.autodealer.crm.enums.AccountStatus;
import com.autodealer.crm.enums.AccountType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.ObjectUtils;

/**
 * 用户表
 *
 * t_user
 */
@Data
public class TUser implements UserDetails, Serializable {

    /**
     * 主键，自动增长，用户ID
     */
    private Integer id;

    /**
     * 登录账号
     */
    private String loginAct;

    /**
     * 登录密码
     */
    @JsonIgnore
    private String loginPwd;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户手机
     */
    private String phone;

    /**
     * 用户邮箱
     */
    private String email;
    private String avatarUrl;
    @JsonIgnore private Integer profileVersion;

    /**
     * 账户是否没有过期，0已过期 1正常
     */
    private Integer accountNoExpired;
    @JsonIgnore
    private LocalDateTime accountExpiresAt;

    /**
     * 密码是否没有过期，0已过期 1正常
     */
    private Integer credentialsNoExpired;

    /**
     * 账号是否没有锁定，0已锁定 1正常
     */
    private Integer accountNoLocked;

    /**
     * 账号是否启用，0禁用 1启用
     */
    private Integer accountEnabled;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 编辑人
     */
    private Integer editBy;

    /**
     * 最近登录时间
     */
    private Date lastLoginTime;

    /**
     * 账号类型：普通人员账号或受保护系统账号。
     */
    @JsonIgnore
    private AccountType accountType;

    /**
     * 是否为受保护恢复账号。
     */
    @JsonIgnore
    private Boolean protectedAccount;

    /**
     * 并发更新版本。
     */
    @JsonIgnore
    private Integer version;

    /**
     * 授权配置并发版本；仅角色、个人权限等授权事实变化时递增。
     */
    @JsonIgnore
    private Integer authorizationVersion;

    /**
     * 认证安全版本；密码、账号状态或授权变化时递增，使旧 Token 立即失效。
     */
    @JsonIgnore
    private Long authVersion;

    /** 会话列表命令的独立 CAS 版本，不等同于认证安全版本。 */
    @JsonIgnore
    private Long sessionRevision;

    /** 账号状态、首次改密和锁定事实不得再由四个历史布尔值推断。 */
    private AccountStatus accountStatus;
    private Boolean mustChangePassword;
    @JsonIgnore
    private Integer failedLoginCount;
    @JsonIgnore
    private LocalDateTime autoLockedUntil;
    @JsonIgnore
    private Boolean manualLocked;
    @JsonIgnore
    private String manualLockReason;
    @JsonIgnore
    private Integer manualLockedBy;
    @JsonIgnore
    private LocalDateTime manualLockedAt;
    @JsonIgnore
    private LocalDateTime passwordExpiresAt;

    /**
     * 角色List
     */
    private List<String> roleList;

    /**
     * 权限标识符List
     */
    private List<String> permissionList;

    /**
     * 菜单的List
     */
    private List<TPermission> menuPermissionList;

    /**
     * 一对一关联
     */
    private TUser createByDO;
    private TUser editByDO;

    private static final long serialVersionUID = 1L;

    //-------------------------实现UserDetails接口的7个方法------------------------------

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        //角色
        if (!ObjectUtils.isEmpty(this.getRoleList()))  {
            this.getRoleList().forEach(role -> {
                list.add(new SimpleGrantedAuthority(role));
            });
        }
        if (!ObjectUtils.isEmpty(this.getPermissionList()))  {
            //权限标识符
            this.getPermissionList().forEach(permission -> {
                list.add(new SimpleGrantedAuthority(permission));
            });
        }
        return list;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return this.getLoginPwd();
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return this.getLoginAct();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return Integer.valueOf(1).equals(this.getAccountNoExpired())
                && (this.getAccountExpiresAt() == null || this.getAccountExpiresAt().isAfter(LocalDateTime.now()));
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return Integer.valueOf(1).equals(this.getAccountNoLocked())
                && !Boolean.TRUE.equals(this.getManualLocked())
                && (this.getAutoLockedUntil() == null || !this.getAutoLockedUntil().isAfter(LocalDateTime.now()));
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return Integer.valueOf(1).equals(this.getCredentialsNoExpired())
                && (this.getPasswordExpiresAt() == null || this.getPasswordExpiresAt().isAfter(LocalDateTime.now()));
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(this.getAccountEnabled())
                && (this.getAccountStatus() == null || this.getAccountStatus() == AccountStatus.ACTIVE);
    }
}
