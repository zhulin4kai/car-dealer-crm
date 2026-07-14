package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.service.LoginSecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class LoginSecurityServiceImpl implements LoginSecurityService {
    private final TUserMapper mapper;
    private final TAuthorizationGraphLockMapper graphLock;
    private final OperationAuditRecorder audit;
    private final UserSecurityMutationCoordinator securityMutations;
    public LoginSecurityServiceImpl(TUserMapper mapper,TAuthorizationGraphLockMapper graphLock,
                                    OperationAuditRecorder audit,UserSecurityMutationCoordinator securityMutations){this.mapper=mapper;this.graphLock=graphLock;this.audit=audit;this.securityMutations=securityMutations;}
    @Override
    @Transactional
    public void recordFailure(String loginAct) {
        if (loginAct == null || loginAct.isBlank()) return;
        for (int attempt = 0; attempt < 3; attempt++) {
            TUser user = mapper.selectByLoginAct(loginAct);
            if (user == null || Boolean.TRUE.equals(user.getManualLocked())) return;
            int expected = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
            boolean threshold = expected + 1 >= 5;
            if (threshold) {
                lockGraph("REPORTING_GRAPH");
                lockGraph("AVAILABLE_ADMIN_GUARD");
                user = mapper.selectByPrimaryKeyForUpdate(user.getId());
                if (user == null || Boolean.TRUE.equals(user.getManualLocked())) return;
                expected = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
                threshold = expected + 1 >= 5;
            }
            boolean lockAccount = threshold && canAutoLock(user);
            if (mapper.recordLoginFailureByExpected(user.getId(), expected,
                    LocalDateTime.now().plusMinutes(15), lockAccount) != 1) continue;
            if (lockAccount) {
                securityMutations.accessChanged(user.getId(), null, "登录失败自动锁定");
                audit.recordAnonymous(AuditActionEnum.USER_LOGIN_AUTO_LOCK, String.valueOf(user.getId()),
                        "SUCCESS", "{\"threshold\":5,\"lockMinutes\":15}");
            } else if (threshold && expected + 1 == 5) {
                audit.recordAnonymous(AuditActionEnum.USER_LOGIN_AUTO_LOCK_BYPASSED,
                        String.valueOf(user.getId()), "SUCCESS",
                        "{\"threshold\":5,\"reason\":\"ADMIN_AVAILABILITY_PROTECTION\"}");
            }
            return;
        }
    }

    private void lockGraph(String name) {
        if (!name.equals(graphLock.lockByName(name))) {
            throw new IllegalStateException("登录安全图锁缺失: " + name);
        }
    }
    private boolean canAutoLock(TUser user){if(Boolean.TRUE.equals(user.getProtectedAccount()))return false;boolean admin=mapper.selectRolesByUserId(user.getId()).stream().map(TRole::getRole).anyMatch("admin"::equals);return !admin||mapper.countAvailableAdminUsersExcluding(user.getId())>0;}
    @Override @Transactional public void recordSuccess(Integer userId){if(mapper.recordLoginSuccess(userId,LocalDateTime.now())!=1)throw new IllegalStateException("登录安全状态保存失败");}
}
