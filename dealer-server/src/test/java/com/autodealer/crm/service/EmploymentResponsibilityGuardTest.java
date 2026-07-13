package com.autodealer.crm.service;

import com.autodealer.crm.enums.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.EmploymentResponsibilityGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploymentResponsibilityGuardTest {
    @Mock TEmployeeMapper employees; @Mock TUserMapper users;

    @Test void onlyActiveEmployeeWithUsableAccountCanReceiveNewResponsibility(){
        EmploymentResponsibilityGuard guard=new EmploymentResponsibilityGuard(employees,users);
        TEmployee employee=new TEmployee();employee.setUserId(2);employee.setEmploymentStatus(EmployeeStatus.ACTIVE);
        TUser user=activeUser();when(employees.selectByUserId(2)).thenReturn(employee);when(users.selectByPrimaryKey(2)).thenReturn(user);
        assertDoesNotThrow(()->guard.requireActiveOwner(2));

        employee.setEmploymentStatus(EmployeeStatus.HANDOVER);
        assertLifecycleConflict(guard);
        employee.setEmploymentStatus(EmployeeStatus.ACTIVE);user.setAccountStatus(AccountStatus.DISABLED);
        assertLifecycleConflict(guard);
        user.setAccountStatus(AccountStatus.ACTIVE);user.setManualLocked(true);
        assertLifecycleConflict(guard);
        user.setManualLocked(false);user.setAutoLockedUntil(LocalDateTime.now().plusMinutes(5));
        assertLifecycleConflict(guard);
    }

    private void assertLifecycleConflict(EmploymentResponsibilityGuard guard){BusinessException error=assertThrows(BusinessException.class,()->guard.requireActiveOwner(2));assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,error.getCodeEnum());}
    private TUser activeUser(){TUser user=new TUser();user.setId(2);user.setAccountStatus(AccountStatus.ACTIVE);user.setAccountEnabled(1);user.setAccountNoLocked(1);user.setAccountNoExpired(1);user.setCredentialsNoExpired(1);user.setManualLocked(false);return user;}
}
