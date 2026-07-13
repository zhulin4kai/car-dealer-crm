package com.autodealer.crm.config.security;

import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.model.TUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录主体的统一人员资格门禁。
 *
 * <p>普通人员必须同时具备在职员工档案、当前有效主要任职、启用且非占位组织、
 * 启用且非占位岗位。固定恢复账号不绑定员工事实，继续交由专门的恢复门禁限制用途。</p>
 */
@Component
public class PrincipalEligibilityPolicy {
    private static final Logger log = LoggerFactory.getLogger(PrincipalEligibilityPolicy.class);

    private final TEmployeeMapper employees;
    private final UserManagementAccessGate userManagementAccessGate;

    public PrincipalEligibilityPolicy(TEmployeeMapper employees,
                                      UserManagementAccessGate userManagementAccessGate) {
        this.employees = employees;
        this.userManagementAccessGate = userManagementAccessGate;
    }

    public boolean isEligible(TUser user) {
        if (user == null) return false;
        if (userManagementAccessGate.isFixedRecoveryAccount(user)) return true;
        if (user.getAccountType() != AccountType.HUMAN || user.getId() == null) return false;
        try {
            return employees.countLoginEligibleByUserId(user.getId(), LocalDateTime.now()) == 1;
        } catch (RuntimeException exception) {
            log.error("登录主体人员资格查询失败 userId={}", user.getId(), exception);
            return false;
        }
    }
}
