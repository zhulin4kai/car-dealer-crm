package com.autodealer.crm.config.security;

import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.enums.AccountType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;

/**
 * 用户管理初始化门禁与受保护恢复账号用途门禁。
 */
@Component
public class UserManagementAccessGate {
    private final TUserMapper users;
    private final TOrganizationUnitMapper organizations;
    private final boolean enabled;

    public UserManagementAccessGate(TUserMapper users,TOrganizationUnitMapper organizations,
                                    @Value("${security.user-management-bootstrap-gate.enabled:true}") boolean enabled) {
        this.users = users;
        this.organizations = organizations;
        this.enabled = enabled;
    }

    public Decision evaluate(TUser user, HttpServletRequest request) {
        if (!enabled) return Decision.allow();
        if (SecurityPaths.isLogoutPath(request)) return Decision.allow();
        boolean recovery = isFixedRecoveryAccount(user);
        BootstrapState state=state();
        if(state==BootstrapState.READY)return recovery&&!isRecoverySessionPath(request)
                ?Decision.deny(CodeEnum.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN,"受保护恢复账号只能执行独立恢复流程，不能进入常规用户治理或日常业务")
                :Decision.allow();
        if(state==BootstrapState.UNINITIALIZED)return recovery&&isBootstrapPath(request)
                ?Decision.allow():Decision.deny(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,"首个普通管理员尚未创建");
        if(recovery&&isDegradedRecoveryPath(request))return Decision.allow();
        if(state==BootstrapState.PENDING_FIRST_CHANGE&&!recovery&&isPendingFirstChangePath(request))return Decision.allow();
        return Decision.deny(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,state==BootstrapState.DEGRADED
                ?"普通管理员全部失效，只允许固定恢复账号执行独立管理员入口恢复"
                :"首个普通管理员完成激活和首次改密前不能访问日常业务");
    }

    public BootstrapState state(){
        if(organizations.countInitializedRootOrganizations()==0)return BootstrapState.UNINITIALIZED;
        if(users.countAdminUsers()>0)return BootstrapState.READY;
        if(users.countPendingAdminUsers()>0)return BootstrapState.PENDING_FIRST_CHANGE;
        return BootstrapState.DEGRADED;
    }

    private boolean isBootstrapPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/api/login/info".equals(path) || "/api/logout".equals(path)) return true;
        if ("/api/users".equals(path)) return HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.POST.matches(request.getMethod());
        return "/api/users/filter-options".equals(path) && HttpMethod.GET.matches(request.getMethod());
    }

    private boolean isRecoverySessionPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/login/info".equals(path) && HttpMethod.GET.matches(request.getMethod());
    }

    private boolean isDegradedRecoveryPath(HttpServletRequest request){return isRecoverySessionPath(request)
            ||("/api/recovery/admin-access".equals(request.getRequestURI())&&HttpMethod.POST.matches(request.getMethod()));}

    private boolean isPendingFirstChangePath(HttpServletRequest request){String path=request.getRequestURI();return isRecoverySessionPath(request)
            ||("/api/logout".equals(path)&&HttpMethod.POST.matches(request.getMethod()))
            ||("/api/credentials/first-password-change".equals(path)&&HttpMethod.PUT.matches(request.getMethod()))
            ||("/api/profile".equals(path)&&(HttpMethod.GET.matches(request.getMethod())||HttpMethod.PUT.matches(request.getMethod())))
            ||("/api/profile/contact-verification".equals(path)&&HttpMethod.POST.matches(request.getMethod()))
            ||("/api/credentials/verify-contact".equals(path)&&HttpMethod.POST.matches(request.getMethod()));}

    public boolean isFixedRecoveryAccount(TUser user) {
        return user != null && Objects.equals(user.getId(), 1)
                && "admin".equals(user.getLoginAct())
                && user.getAccountType() == AccountType.SYSTEM
                && Boolean.TRUE.equals(user.getProtectedAccount());
    }

    public record Decision(boolean allowed, CodeEnum code, String message) {
        public static Decision allow() { return new Decision(true, null, null); }
        public static Decision deny(CodeEnum code, String message) { return new Decision(false, code, message); }
    }
    public enum BootstrapState {UNINITIALIZED,PENDING_FIRST_CHANGE,READY,DEGRADED}
}
