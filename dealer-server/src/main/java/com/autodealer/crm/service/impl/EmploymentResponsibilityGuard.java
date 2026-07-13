package com.autodealer.crm.service.impl;

import com.autodealer.crm.enums.EmployeeStatus;
import com.autodealer.crm.enums.AccountStatus;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/** 新增长期 owner 事实的统一人员生命周期边界；历史责任快照不调用本守卫。 */
@Component
public class EmploymentResponsibilityGuard {
    private final TEmployeeMapper employees;
    private final TUserMapper users;
    public EmploymentResponsibilityGuard(TEmployeeMapper employees,TUserMapper users){this.employees=employees;this.users=users;}
    public void requireActiveOwner(Integer userId){TEmployee employee=userId==null?null:employees.selectByUserId(userId);
        TUser user=userId==null?null:users.selectByPrimaryKey(userId);LocalDateTime now=LocalDateTime.now();
        if(employee==null||employee.getEmploymentStatus()!=EmployeeStatus.ACTIVE||user==null||user.getAccountStatus()!=AccountStatus.ACTIVE
                ||!Integer.valueOf(1).equals(user.getAccountEnabled())||!Integer.valueOf(1).equals(user.getAccountNoLocked())
                ||!Integer.valueOf(1).equals(user.getAccountNoExpired())
                ||(user.getAccountExpiresAt()!=null&&!user.getAccountExpiresAt().isAfter(now))
                ||!Integer.valueOf(1).equals(user.getCredentialsNoExpired())
                ||(user.getPasswordExpiresAt()!=null&&!user.getPasswordExpiresAt().isAfter(now))
                ||Boolean.TRUE.equals(user.getManualLocked())||(user.getAutoLockedUntil()!=null&&user.getAutoLockedUntil().isAfter(now)))
            throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT,"只有在职且账号可用的员工可以成为新的长期业务负责人");}
}
