package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.internal.OwnerCandidateCacheInvalidator;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ActingReportingInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.AssignmentInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ReportingInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ReplaceActingReportingsRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateEmployeeOrganizationRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.StatusRequest;
import com.autodealer.crm.modules.identity.application.api.enums.AssignmentType;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.CredentialService;
import com.autodealer.crm.modules.identity.application.api.ManagedUserAccountService;
import com.autodealer.crm.modules.identity.application.api.OrganizationService;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import com.autodealer.crm.modules.identity.application.internal.UserAuthorizationPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DealerCRMApplication.class)
@ActiveProfiles("test")
class SupervisorStatusReportingConcurrencyTest {
    private static final int ORGANIZATION_ID = 9960;
    private static final int POSITION_ID = 9960;
    private static final int MANAGER_USER_ID = 9961;
    private static final int MANAGER_EMPLOYEE_ID = 9961;
    private static final int SUBORDINATE_USER_ID = 9962;
    private static final int SUBORDINATE_EMPLOYEE_ID = 9962;

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired ManagedUserAccountService accountService;
    @Autowired OrganizationService organizationService;
    @Autowired TEmployeeMapper employeeMapper;
    @Autowired TEmployeeReportingMapper reportingMapper;

    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean UserAuthorizationPolicy authorizationPolicy;
    @MockitoBean OperationAuditRecorder operationAuditRecorder;
    @MockitoBean AuthorizationAuditRecorder authorizationAuditRecorder;
    @MockitoBean UserSessionService userSessionService;
    @MockitoBean CredentialService credentialService;
    @MockitoBean RedisManager redisManager;
    @MockitoBean OwnerCandidateCacheInvalidator ownerCandidateCacheInvalidator;

    @BeforeEach
    void seed() {
        cleanup();
        when(currentUser.getCurrentUserId()).thenReturn(1);
        when(currentUser.hasAuthority(anyString())).thenReturn(true);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        jdbc.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,placeholder,enabled,version,create_time,create_by) VALUES(?, 'SUPERVISOR_RACE_ORG', '主管并发组织', 'DEPARTMENT', 1, 1, 0, 1, 0, CURRENT_TIMESTAMP, 1)", ORGANIZATION_ID);
        jdbc.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?, 'SUPERVISOR_RACE_POS', '主管并发岗位', 100, 0, 1, 0, CURRENT_TIMESTAMP, 1)", POSITION_ID);
        insertEmployee(MANAGER_USER_ID, MANAGER_EMPLOYEE_ID, "supervisor_race_manager", "MGR-9961", "并发主管");
        insertEmployee(SUBORDINATE_USER_ID, SUBORDINATE_EMPLOYEE_ID, "supervisor_race_subordinate", "SUB-9962", "并发下属");
    }

    @AfterEach
    void clean() {
        cleanup();
    }

    @Test
    void disableCommitBeforeDirectWriteRejectsCandidateWithoutResidualRelation() throws Exception {
        assertInvalidationWins("DISABLE", ReportingType.DIRECT);
    }

    @Test
    void disableCommitBeforeActingWriteRejectsCandidateWithoutResidualRelation() throws Exception {
        assertInvalidationWins("DISABLE", ReportingType.ACTING);
    }

    @Test
    void directWriteCommitBeforeDisableCompletesWithoutDeadlockAndManagerBecomesIneffective() throws Exception {
        assertRelationWins(ReportingType.DIRECT, "DISABLE");
    }

    @Test
    void actingWriteCommitBeforeManualLockCompletesWithoutDeadlockAndManagerBecomesIneffective() throws Exception {
        assertRelationWins(ReportingType.ACTING, "LOCK");
    }

    @Test
    void managerWithoutCurrentPrimaryAssignmentIsNotEffective() {
        writeRelation(ReportingType.DIRECT);
        assertEffectiveReporting(true);
        jdbc.update("UPDATE t_employee_assignment SET status='ENDED', active_primary_marker=NULL, effective_to=CURRENT_TIMESTAMP WHERE employee_id=? AND assignment_type='PRIMARY' AND status='ACTIVE'",
                MANAGER_EMPLOYEE_ID);
        assertEffectiveReporting(false);
    }

    @Test
    void managerInDisabledOrganizationIsNotEffective() {
        writeRelation(ReportingType.DIRECT);
        assertEffectiveReporting(true);
        jdbc.update("UPDATE t_organization_unit SET enabled=0 WHERE id=?", ORGANIZATION_ID);
        assertEffectiveReporting(false);
    }

    @Test
    void managerInPlaceholderOrganizationIsNotEffective() {
        writeRelation(ReportingType.DIRECT);
        assertEffectiveReporting(true);
        jdbc.update("UPDATE t_organization_unit SET placeholder=1 WHERE id=?", ORGANIZATION_ID);
        assertEffectiveReporting(false);
    }

    @Test
    void managerInDisabledPositionIsNotEffective() {
        writeRelation(ReportingType.DIRECT);
        assertEffectiveReporting(true);
        jdbc.update("UPDATE t_position SET enabled=0 WHERE id=?", POSITION_ID);
        assertEffectiveReporting(false);
    }

    @Test
    void managerInPlaceholderPositionIsNotEffective() {
        writeRelation(ReportingType.DIRECT);
        assertEffectiveReporting(true);
        Integer placeholderPositionId = jdbc.queryForObject(
                "SELECT id FROM t_position WHERE code='UNASSIGNED_POSITION'", Integer.class);
        jdbc.update("UPDATE t_employee_assignment SET position_id=? WHERE employee_id=? AND assignment_type='PRIMARY' AND status='ACTIVE'",
                placeholderPositionId, MANAGER_EMPLOYEE_ID);
        assertEffectiveReporting(false);
    }

    private void assertInvalidationWins(String command, ReportingType relationType) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch invalidationLocked = new CountDownLatch(1);
        CountDownLatch releaseInvalidation = new CountDownLatch(1);
        CountDownLatch relationStarted = new CountDownLatch(1);
        CountDownLatch relationFinished = new CountDownLatch(1);
        try {
            Future<?> invalidation = executor.submit(() -> transactions.executeWithoutResult(status -> {
                lockGraph("REPORTING_GRAPH");
                invalidationLocked.countDown();
                await(releaseInvalidation, "禁用事务未获准提交");
                accountService.changeStatus(MANAGER_USER_ID, status(command));
            }));
            Future<CodeEnum> relation = executor.submit(() -> {
                await(invalidationLocked, "禁用事务未持有汇报图锁");
                relationStarted.countDown();
                try {
                    writeRelation(relationType);
                    return CodeEnum.OK;
                } catch (BusinessException exception) {
                    return exception.getCodeEnum();
                } finally {
                    relationFinished.countDown();
                }
            });

            assertTrue(invalidationLocked.await(3, TimeUnit.SECONDS));
            assertTrue(relationStarted.await(3, TimeUnit.SECONDS));
            assertFalse(relationFinished.await(200, TimeUnit.MILLISECONDS),
                    "关系事务不应越过禁用事务持有的 REPORTING_GRAPH");
            releaseInvalidation.countDown();

            invalidation.get(5, TimeUnit.SECONDS);
            assertEquals(CodeEnum.INVALID_MANAGER, relation.get(5, TimeUnit.SECONDS));
            assertEquals(0, relationCount(relationType));
            assertEquals(0, jdbc.queryForObject("SELECT version FROM t_employee WHERE id=?", Integer.class,
                    SUBORDINATE_EMPLOYEE_ID));
            assertManagerUnavailable();
        } finally {
            releaseInvalidation.countDown();
            executor.shutdownNow();
        }
    }

    private void assertRelationWins(ReportingType relationType, String command) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch relationLocked = new CountDownLatch(1);
        CountDownLatch releaseRelation = new CountDownLatch(1);
        CountDownLatch invalidationStarted = new CountDownLatch(1);
        CountDownLatch invalidationFinished = new CountDownLatch(1);
        try {
            Future<?> relation = executor.submit(() -> transactions.executeWithoutResult(status -> {
                lockGraph("ORGANIZATION_HIERARCHY");
                lockGraph("REPORTING_GRAPH");
                relationLocked.countDown();
                await(releaseRelation, "关系事务未获准提交");
                writeRelation(relationType);
            }));
            Future<?> invalidation = executor.submit(() -> {
                await(relationLocked, "关系事务未持有汇报图锁");
                invalidationStarted.countDown();
                try {
                    accountService.changeStatus(MANAGER_USER_ID, status(command));
                } finally {
                    invalidationFinished.countDown();
                }
            });

            assertTrue(relationLocked.await(3, TimeUnit.SECONDS));
            assertTrue(invalidationStarted.await(3, TimeUnit.SECONDS));
            assertFalse(invalidationFinished.await(200, TimeUnit.MILLISECONDS),
                    "账号失效事务不应越过关系事务持有的 REPORTING_GRAPH");
            releaseRelation.countDown();

            relation.get(5, TimeUnit.SECONDS);
            invalidation.get(5, TimeUnit.SECONDS);
            assertEquals(1, relationCount(relationType), "锁定后保留不可改写的组织关系事实");
            assertManagerUnavailable();
            assertEffectiveReporting(false);
        } finally {
            releaseRelation.countDown();
            executor.shutdownNow();
        }
    }

    private void writeRelation(ReportingType relationType) {
        if (relationType == ReportingType.DIRECT) {
            UpdateEmployeeOrganizationRequest request = new UpdateEmployeeOrganizationRequest();
            request.setExpectedVersion(0);
            request.setPrimaryAssignment(primaryAssignment());
            request.setAdditionalAssignments(List.of());
            ReportingInput reporting = new ReportingInput();
            reporting.setManagerEmployeeId(MANAGER_EMPLOYEE_ID);
            reporting.setRelationType(ReportingType.DIRECT);
            reporting.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
            request.setReporting(reporting);
            request.setReason("主管并发 DIRECT 验证");
            organizationService.updateEmployeeOrganizationMembership(SUBORDINATE_EMPLOYEE_ID, request);
            return;
        }
        ReplaceActingReportingsRequest request = new ReplaceActingReportingsRequest();
        request.setExpectedEmployeeVersion(0);
        ActingReportingInput relation = new ActingReportingInput();
        relation.setManagerEmployeeId(MANAGER_EMPLOYEE_ID);
        relation.setEffectiveTo(OffsetDateTime.now().plusDays(7));
        request.setRelations(List.of(relation));
        request.setReason("主管并发 ACTING 验证");
        organizationService.replaceActingReportings(SUBORDINATE_EMPLOYEE_ID, request);
    }

    private AssignmentInput primaryAssignment() {
        AssignmentInput assignment = new AssignmentInput();
        assignment.setOrganizationUnitId(ORGANIZATION_ID);
        assignment.setPositionId(POSITION_ID);
        assignment.setAssignmentType(AssignmentType.PRIMARY);
        assignment.setEffectiveFrom(OffsetDateTime.now().minusDays(1));
        return assignment;
    }

    private StatusRequest status(String command) {
        StatusRequest request = new StatusRequest();
        request.setAccountVersion(0);
        request.setCommand(command);
        request.setReason("主管并发账号失效验证");
        return request;
    }

    private int relationCount(ReportingType relationType) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM t_employee_reporting WHERE subordinate_employee_id=? AND manager_employee_id=? AND relation_type=? AND status='ACTIVE'",
                Integer.class, SUBORDINATE_EMPLOYEE_ID, MANAGER_EMPLOYEE_ID, relationType.name());
    }

    private void assertManagerUnavailable() {
        assertEquals(0, employeeMapper.selectEligibleManagerCandidates(
                SUBORDINATE_EMPLOYEE_ID, LocalDateTime.now(), List.of(ORGANIZATION_ID)).size());
    }

    private void assertEffectiveReporting(boolean expected) {
        LocalDateTime effectiveAt = LocalDateTime.now();
        assertEquals(expected, !reportingMapper.selectEffectiveManagers(
                        SUBORDINATE_EMPLOYEE_ID, effectiveAt).isEmpty(),
                "下属侧的有效主管事实必须与统一人员资格一致");
        assertEquals(expected, !reportingMapper.selectEffectiveSubordinates(
                        MANAGER_EMPLOYEE_ID, effectiveAt).isEmpty(),
                "主管侧的有效管理范围必须与统一人员资格一致");
    }

    private void lockGraph(String name) {
        assertEquals(name, jdbc.queryForObject(
                "SELECT lock_name FROM t_authorization_graph_lock WHERE lock_name=? FOR UPDATE",
                String.class, name));
    }

    private void await(CountDownLatch latch, String message) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) throw new IllegalStateException(message);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(message, exception);
        }
    }

    private void insertEmployee(int userId, int employeeId, String loginAct, String employeeNo, String name) {
        jdbc.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,manual_locked,version,authorization_version,auth_version,session_revision,create_time,create_by) VALUES(?,?, 'x', ?,1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0,CURRENT_TIMESTAMP,1)", userId, loginAct, name);
        jdbc.update("INSERT INTO t_employee(id,user_id,employee_no,name,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(?,?,?,?,'ACTIVE',1,0,0,0,0,CURRENT_TIMESTAMP,1)", employeeId, userId, employeeNo, name);
        jdbc.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,DATEADD('DAY',-1,CURRENT_TIMESTAMP),'主管并发初始化',0,CURRENT_TIMESTAMP,1)", employeeId, ORGANIZATION_ID, POSITION_ID);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM t_authorization_history WHERE subject_id=?", String.valueOf(SUBORDINATE_EMPLOYEE_ID));
        jdbc.update("DELETE FROM t_employee_reporting WHERE subordinate_employee_id=? OR manager_employee_id=?",
                SUBORDINATE_EMPLOYEE_ID, MANAGER_EMPLOYEE_ID);
        jdbc.update("DELETE FROM t_employee_assignment WHERE employee_id IN (?,?)",
                MANAGER_EMPLOYEE_ID, SUBORDINATE_EMPLOYEE_ID);
        jdbc.update("DELETE FROM t_employee WHERE id IN (?,?)", MANAGER_EMPLOYEE_ID, SUBORDINATE_EMPLOYEE_ID);
        jdbc.update("DELETE FROM t_user WHERE id IN (?,?)", MANAGER_USER_ID, SUBORDINATE_USER_ID);
        jdbc.update("DELETE FROM t_position WHERE id=?", POSITION_ID);
        jdbc.update("DELETE FROM t_organization_unit WHERE id=?", ORGANIZATION_ID);
    }
}
