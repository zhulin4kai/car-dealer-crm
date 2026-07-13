package com.autodealer.crm.integration;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.organization.OrganizationChangeHistoryResponse;
import com.autodealer.crm.dto.user.UserLifecycleDtos.*;
import com.autodealer.crm.service.AuthorizationDataScope;
import com.autodealer.crm.service.DataScopeResolver;
import com.autodealer.crm.service.OrganizationService;
import com.autodealer.crm.service.UserLifecycleService;
import com.autodealer.crm.service.UserSessionService;
import com.autodealer.crm.service.impl.UserAuthorizationPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserLifecycleIntegrationTest {
    private static final int USER_ID=97,MANAGER_USER_ID=98,EMPLOYEE_ID=97,MANAGER_EMPLOYEE_ID=98;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserLifecycleService service;
    @Autowired OrganizationService organizationService;
    @MockBean CurrentUserProvider current;
    @MockBean UserAuthorizationPolicy policy;
    @MockBean DataScopeResolver dataScopes;
    @MockBean UserSessionService sessions;
    @MockBean OperationAuditRecorder operationAudit;

    @BeforeEach
    void seed(){
        cleanup();when(current.getCurrentUserId()).thenReturn(1);when(current.hasAuthority(anyString())).thenReturn(true);when(policy.isGlobalOperator()).thenReturn(true);
        when(dataScopes.resolve(anyInt(),anyString())).thenReturn(AuthorizationDataScope.global(Set.of()));
        jdbc.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,manual_locked,version,authorization_version,auth_version,session_revision) VALUES(97,'lifecycle97','x','生命周期员工',1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0)");
        jdbc.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,manual_locked,version,authorization_version,auth_version,session_revision) VALUES(98,'manager98','x','生命周期主管',1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0)");
        jdbc.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(970,'LIFECYCLE_ORG_A','生命周期组织A','DEPARTMENT',1,1,0,1,0,CURRENT_TIMESTAMP,1),(971,'LIFECYCLE_ORG_B','生命周期组织B','DEPARTMENT',1,2,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbc.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(970,'LIFECYCLE_POSITION_A','生命周期岗位A',10,0,1,0,CURRENT_TIMESTAMP,1),(971,'LIFECYCLE_POSITION_B','生命周期岗位B',10,0,1,0,CURRENT_TIMESTAMP,1),(972,'LIFECYCLE_MANAGER','生命周期主管岗位',20,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbc.update("INSERT INTO t_employee(id,user_id,employee_no,name,employment_status,profile_completed,hire_date,version,phone_verified,email_verified,create_time,create_by) VALUES(97,97,'EMP-000097','生命周期员工','ACTIVE',1,CURRENT_DATE,0,0,0,CURRENT_TIMESTAMP,1),(98,98,'EMP-000098','生命周期主管','ACTIVE',1,CURRENT_DATE,0,0,0,CURRENT_TIMESTAMP,1)");
        jdbc.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(97,970,970,'PRIMARY','ACTIVE',1,DATEADD('DAY',-1,CURRENT_TIMESTAMP),'初始任职',0,CURRENT_TIMESTAMP,1),(98,1,972,'PRIMARY','ACTIVE',1,DATEADD('DAY',-1,CURRENT_TIMESTAMP),'根组织主管任职',0,CURRENT_TIMESTAMP,1)");
        jdbc.update("INSERT INTO t_employee_reporting(subordinate_employee_id,manager_employee_id,relation_type,status,active_direct_marker,effective_from,reason,version,create_time,create_by) VALUES(97,98,'DIRECT','ACTIVE',1,DATEADD('DAY',-1,CURRENT_TIMESTAMP),'初始汇报',0,CURRENT_TIMESTAMP,1)");
        jdbc.update("INSERT INTO t_employee_reporting(subordinate_employee_id,manager_employee_id,relation_type,status,effective_from,effective_to,reason,version,create_time,create_by) VALUES(97,98,'ACTING','ACTIVE',DATEADD('HOUR',-1,CURRENT_TIMESTAMP),DATEADD('DAY',7,CURRENT_TIMESTAMP),'临时代理汇报',0,CURRENT_TIMESTAMP,1)");
    }

    @AfterEach void clean(){cleanup();}

    @Test
    void transferDepartureCompleteAndRecoverRehirePreserveHistoryWithoutRestoringAuthorization(){
        AssignmentCommand transfer=new AssignmentCommand();transfer.setEmployeeVersion(0);transfer.setOrganizationUnitId(971);transfer.setPositionId(971);transfer.setManagerEmployeeId(MANAGER_EMPLOYEE_ID);transfer.setEffectiveFrom(OffsetDateTime.now());transfer.setReason("跨组织调岗");
        Context transferred=service.transfer(USER_ID,transfer);
        assertEquals(1,transferred.getEmployeeVersion());assertEquals("ACTIVE",transferred.getEmploymentStatus());
        assertEquals(971,jdbc.queryForObject("SELECT organization_unit_id FROM t_employee_assignment WHERE employee_id=97 AND active_primary_marker=1",Integer.class));

        DeparturePrecheckRequest startPrecheckRequest=new DeparturePrecheckRequest();startPrecheckRequest.setEmployeeVersion(1);startPrecheckRequest.setReason("启动离职");
        DeparturePrecheck startPrecheck=service.precheckDeparture(USER_ID,startPrecheckRequest);
        StartDepartureRequest start=new StartDepartureRequest();start.setEmployeeVersion(1);start.setSnapshotToken(startPrecheck.getSnapshotToken());start.setReason("启动离职");
        Context handover=service.startDeparture(USER_ID,start);
        assertEquals("HANDOVER",handover.getEmploymentStatus());assertEquals(2,handover.getEmployeeVersion());

        DeparturePrecheckRequest completePrecheckRequest=new DeparturePrecheckRequest();completePrecheckRequest.setEmployeeVersion(2);completePrecheckRequest.setReason("完成离职");
        DeparturePrecheck completePrecheck=service.precheckDeparture(USER_ID,completePrecheckRequest);
        assertTrue(completePrecheck.isReadyToComplete());assertFalse(completePrecheck.isHandoverRequired());
        CompleteDepartureRequest complete=new CompleteDepartureRequest();complete.setEmployeeVersion(2);complete.setSnapshotToken(completePrecheck.getSnapshotToken());complete.setReason("完成离职");
        Context left=service.completeDeparture(USER_ID,complete);
        assertEquals("LEFT",left.getEmploymentStatus());assertEquals(3,left.getEmployeeVersion());
        assertEquals("DISABLED",jdbc.queryForObject("SELECT account_status FROM t_user WHERE id=97",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_employee_assignment WHERE employee_id=97 AND active_primary_marker=1",Integer.class));

        RehireRequest rehire=new RehireRequest();rehire.setEmployeeVersion(3);rehire.setOrganizationUnitId(970);rehire.setPositionId(970);rehire.setManagerEmployeeId(MANAGER_EMPLOYEE_ID);rehire.setEffectiveFrom(OffsetDateTime.now());rehire.setReason("恢复现有凭证返聘");rehire.setAccountActivationMode(AccountActivationMode.RECOVER);
        RehireResult rehired=service.rehire(USER_ID,rehire);
        assertEquals("ACTIVE",rehired.getContext().getEmploymentStatus());assertEquals(4,rehired.getContext().getEmployeeVersion());assertEquals(0,rehired.getRestoredLegacyAuthorizationCount());assertEquals("NOT_REQUIRED",rehired.getCredentialDeliveryStatus());
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT account_status FROM t_user WHERE id=97",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_employee_assignment WHERE employee_id=97 AND active_primary_marker=1",Integer.class));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM t_employee_assignment WHERE employee_id=97",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_lifecycle_event WHERE user_id=97",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=97 AND active_marker=1",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_permission WHERE user_id=97 AND active_marker=1",Integer.class));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE subject_id='97' AND subject_type='ORGANIZATION_ASSIGNMENT'",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE subject_id='97' AND subject_type='REPORTING_RELATION'",Integer.class));
        assertEquals(7,jdbc.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE subject_id='97' AND target_user_id=97 AND operator_id=1 AND occurred_time IS NOT NULL",Integer.class));
        String transferBefore=jdbc.queryForObject("SELECT before_value FROM t_authorization_history WHERE subject_id='97' AND subject_type='ORGANIZATION_ASSIGNMENT' AND reason='跨组织调岗'",String.class);
        String transferAfter=jdbc.queryForObject("SELECT after_value FROM t_authorization_history WHERE subject_id='97' AND subject_type='ORGANIZATION_ASSIGNMENT' AND reason='跨组织调岗'",String.class);
        assertTrue(transferBefore.contains("LIFECYCLE_ORG_A"));assertTrue(transferAfter.contains("LIFECYCLE_ORG_B"));
        String departureAfter=jdbc.queryForObject("SELECT after_value FROM t_authorization_history WHERE subject_id='97' AND subject_type='ORGANIZATION_ASSIGNMENT' AND reason='完成离职'",String.class);
        assertTrue(departureAfter.contains("\"primaryAssignment\":{}"));assertTrue(departureAfter.contains("\"additionalAssignments\":[]"));
        String departureActingBefore=jdbc.queryForObject("SELECT before_value FROM t_authorization_history WHERE subject_id='97' AND subject_type='REPORTING_RELATION' AND reason='完成离职' AND before_value LIKE '%ACTING%'",String.class);
        String departureActingAfter=jdbc.queryForObject("SELECT after_value FROM t_authorization_history WHERE subject_id='97' AND subject_type='REPORTING_RELATION' AND reason='完成离职' AND before_value LIKE '%ACTING%'",String.class);
        assertTrue(departureActingBefore.contains("EMP-000098"));assertEquals("[]",departureActingAfter);
        String rehireAfter=jdbc.queryForObject("SELECT after_value FROM t_authorization_history WHERE subject_id='97' AND subject_type='ORGANIZATION_ASSIGNMENT' AND reason='恢复现有凭证返聘'",String.class);
        assertTrue(rehireAfter.contains("LIFECYCLE_ORG_A"));assertTrue(rehireAfter.contains("LIFECYCLE_POSITION_A"));
        List<OrganizationChangeHistoryResponse> organizationHistory=organizationService.getOrganizationHistory(EMPLOYEE_ID);
        assertEquals(7,organizationHistory.size());assertTrue(organizationHistory.stream().allMatch(value->value.getCreateTime()!=null&&value.getOperatorName()!=null));
        assertEquals(2,organizationHistory.stream().filter(value->"跨组织调岗".equals(value.getReason())).count());
        assertEquals(3,organizationHistory.stream().filter(value->"完成离职".equals(value.getReason())).count());
        assertEquals(2,organizationHistory.stream().filter(value->"恢复现有凭证返聘".equals(value.getReason())).count());
        verify(sessions,atLeast(3)).revokeAllForSecurityChange(anyInt(),anyInt(),anyString());
    }

    @Test
    void transferOrganizationHistoryFailureRollsBackAssignmentReportingAndLifecycleEvent(){
        doThrow(new IllegalStateException("组织历史审计失败")).when(operationAudit)
                .record(eq(AuditActionEnum.EMPLOYEE_ASSIGNMENT_CHANGE),eq("97"),eq("SUCCESS"),anyString());
        AssignmentCommand transfer=new AssignmentCommand();transfer.setEmployeeVersion(0);transfer.setOrganizationUnitId(971);transfer.setPositionId(971);transfer.setManagerEmployeeId(MANAGER_EMPLOYEE_ID);transfer.setEffectiveFrom(OffsetDateTime.now());transfer.setReason("历史失败整单回滚");

        IllegalStateException failure=assertThrows(IllegalStateException.class,()->service.transfer(USER_ID,transfer));

        assertTrue(failure.getMessage().contains("组织历史审计失败"));
        assertEquals(970,jdbc.queryForObject("SELECT organization_unit_id FROM t_employee_assignment WHERE employee_id=97 AND active_primary_marker=1",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM t_employee_reporting WHERE subordinate_employee_id=97 AND status='ACTIVE'",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT version FROM t_employee WHERE id=97",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT authorization_version FROM t_user WHERE id=97",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE subject_id='97'",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_lifecycle_event WHERE user_id=97",Integer.class));
        verify(sessions,never()).revokeAllForSecurityChange(anyInt(),anyInt(),anyString());
    }

    private void cleanup(){
        jdbc.update("DELETE FROM t_authorization_history WHERE subject_id='97' OR target_user_id=97");
        jdbc.update("DELETE FROM t_user_lifecycle_event WHERE user_id=97");jdbc.update("DELETE FROM t_user_lifecycle_snapshot WHERE user_id=97");
        jdbc.update("DELETE FROM t_user_session WHERE user_id IN (97,98)");jdbc.update("DELETE FROM t_account_credential WHERE user_id IN (97,98)");jdbc.update("DELETE FROM t_password_history WHERE user_id IN (97,98)");
        jdbc.update("DELETE FROM t_user_permission_organization WHERE user_permission_id IN (SELECT id FROM t_user_permission WHERE user_id IN (97,98))");jdbc.update("DELETE FROM t_user_permission WHERE user_id IN (97,98)");jdbc.update("DELETE FROM t_user_role WHERE user_id IN (97,98)");
        jdbc.update("DELETE FROM t_employee_reporting WHERE subordinate_employee_id IN (97,98) OR manager_employee_id IN (97,98)");jdbc.update("DELETE FROM t_employee_assignment WHERE employee_id IN (97,98)");
        jdbc.update("DELETE FROM t_employee WHERE id IN (97,98)");jdbc.update("DELETE FROM t_user WHERE id IN (97,98)");jdbc.update("DELETE FROM t_position WHERE id IN (970,971,972)");jdbc.update("DELETE FROM t_organization_unit WHERE id IN (970,971)");
    }
}
