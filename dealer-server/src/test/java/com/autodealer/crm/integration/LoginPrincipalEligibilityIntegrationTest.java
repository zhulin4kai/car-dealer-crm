package com.autodealer.crm.integration;

import com.autodealer.crm.modules.identity.application.api.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class LoginPrincipalEligibilityIntegrationTest extends BackendIntegrationTestBase {
    @Autowired private UserService userService;

    @Test
    void activeEmployeeWithRealPrimaryAssignmentCanLoadForLoginAndTokenRefresh() {
        Subject subject = insertSubject(9810, "ACTIVE", true, true, true, false);

        assertEquals(subject.loginAct(), userService.loadUserByUsername(subject.loginAct()).getUsername());
        assertNotNull(userService.getLoginUserById(subject.userId()));
    }

    @Test
    void missingEmployeeFailsClosedForLoginAndTokenRefresh() {
        Subject subject = insertAccount(9811);
        assertRejected(subject);
    }

    @Test
    void departedEmployeeFailsClosedForLoginAndTokenRefresh() {
        Subject subject = insertSubject(9812, "LEFT", true, true, true, false);
        assertRejected(subject);
    }

    @Test
    void missingCurrentPrimaryAssignmentFailsClosedForLoginAndTokenRefresh() {
        Subject subject = insertSubject(9813, "ACTIVE", false, true, true, false);
        assertRejected(subject);
    }

    @Test
    void disabledOrganizationFailsClosedForLoginAndTokenRefresh() {
        Subject subject = insertSubject(9814, "ACTIVE", true, false, true, false);
        assertRejected(subject);
    }

    @Test
    void disabledPositionFailsClosedForLoginAndTokenRefresh() {
        Subject subject = insertSubject(9815, "ACTIVE", true, true, false, false);
        assertRejected(subject);
    }

    @Test
    void placeholderPositionFailsClosedForLoginAndTokenRefresh() {
        Subject subject = insertSubject(9816, "ACTIVE", true, true, true, true);
        assertRejected(subject);
    }

    private void assertRejected(Subject subject) {
        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(subject.loginAct()));
        assertNull(userService.getLoginUserById(subject.userId()));
    }

    private Subject insertSubject(int userId, String employmentStatus, boolean withAssignment,
                                  boolean organizationEnabled, boolean positionEnabled,
                                  boolean placeholderPosition) {
        Subject subject = insertAccount(userId);
        int employeeId = userId;
        jdbcTemplate.update("""
                INSERT INTO t_employee(id,user_id,employee_no,name,employment_status,profile_completed,
                  hire_date,version,phone_verified,email_verified,create_time,create_by)
                VALUES(?,?,?,? ,?,1,CURRENT_DATE,0,0,0,CURRENT_TIMESTAMP,1)
                """, employeeId, userId, "EMP-LOGIN-" + userId, "认证资格用户" + userId, employmentStatus);
        if (!withAssignment) return subject;

        int organizationId = userId;
        int positionId = placeholderPosition ? 1 : userId;
        jdbcTemplate.update("""
                INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,
                  enabled,version,create_time,create_by)
                VALUES(?,?,?,'TEAM',1,100,0,?,0,CURRENT_TIMESTAMP,1)
                """, organizationId, "LOGIN_ORG_" + userId, "认证资格组织" + userId,
                organizationEnabled ? 1 : 0);
        if (!placeholderPosition) {
            jdbcTemplate.update("""
                    INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by)
                    VALUES(?,?,?,10,0,?,0,CURRENT_TIMESTAMP,1)
                    """, positionId, "LOGIN_POSITION_" + userId, "认证资格岗位" + userId,
                    positionEnabled ? 1 : 0);
        }
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,
                  status,active_primary_marker,effective_from,reason,version,create_time,create_by)
                VALUES(?,?,?,'PRIMARY','ACTIVE',1,DATEADD('MINUTE',-1,CURRENT_TIMESTAMP),
                  '认证资格测试',0,CURRENT_TIMESTAMP,1)
                """, employeeId, organizationId, positionId);
        return subject;
    }

    private Subject insertAccount(int userId) {
        String loginAct = "login_eligibility_" + userId;
        jdbcTemplate.update("""
                INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,
                  account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,
                  auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,
                  create_time,create_by)
                VALUES(?,?,?,? ,1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)
                """, userId, loginAct, "unused", "认证资格用户" + userId);
        return new Subject(userId, loginAct);
    }

    private record Subject(int userId, String loginAct) {}
}
