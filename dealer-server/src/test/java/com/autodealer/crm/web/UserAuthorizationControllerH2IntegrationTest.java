package com.autodealer.crm.web;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.integration.BackendIntegrationTestBase;
import com.autodealer.crm.mapper.TUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class UserAuthorizationControllerH2IntegrationTest extends BackendIntegrationTestBase {
    @MockBean OperationAuditRecorder operationAuditRecorder;
    @Autowired TUserMapper userMapper;
    private String adminToken;

    @BeforeEach void login() throws Exception { adminToken=loginAsQualifiedAdmin(); }

    @Test void anyUserIncludingAdministratorCannotChangeOwnAuthorization() throws Exception {
        mockMvc.perform(put("/api/users/"+QUALIFIED_ADMIN_USER_ID+"/authorization/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"authorizationVersion\":0,\"roleIds\":[],\"reason\":\"禁止自改\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(593));
    }

    @Test void databaseCannotCreateASecondProtectedAuthorizationTarget() {
        assertThrows(DataIntegrityViolationException.class,()->jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_type,protected_account,version,auth_version) VALUES(90,'protected_target','x','保护账号',1,1,1,1,'SYSTEM',1,0,0)"));
    }

    @Test void roleDiffClosesRemovedFactInsertsAddedFactAndRaisesBothVersions() throws Exception {
        int newRole=insertRole("controller_role");
        long oldAuth=jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=2",Long.class);
        int oldProfileVersion=jdbcTemplate.queryForObject("SELECT version FROM t_user WHERE id=2",Integer.class);
        mockMvc.perform(put("/api/users/2/authorization/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":0,\"roleIds\":["+newRole+"],\"reason\":\"岗位变化\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.authorizationVersion").value(1));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=2 AND active_marker=1 AND role_id<>(?)",Integer.class,newRole));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=2 AND active_marker=1 AND role_id=?",Integer.class,newRole));
        assertEquals(oldProfileVersion,jdbcTemplate.queryForObject("SELECT version FROM t_user WHERE id=2",Integer.class));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT authorization_version FROM t_user WHERE id=2",Integer.class));
        assertEquals(oldAuth+1,jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=2",Long.class));
        assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE subject_type='USER_ROLE' AND target_user_id=2",Integer.class));
    }

    @Test void staleVersionIsRejectedWithoutChangingFacts() throws Exception {
        int newRole=insertRole("stale_role");
        int before=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=2 AND active_marker=1",Integer.class);
        mockMvc.perform(put("/api/users/2/authorization/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":99,\"roleIds\":["+newRole+"],\"reason\":\"旧页面\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(600));
        assertEquals(before,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=2 AND active_marker=1",Integer.class));
    }

    @Test void personalGrantThenDenyChangesEffectiveStateAndKeepsSource() throws Exception {
        int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code='user:view'",Integer.class);
        mockMvc.perform(put("/api/users/2/authorization/permissions").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":0,\"changes\":[{\"permissionId\":"+permission+",\"state\":\"GRANT\",\"dataScopeCandidateKey\":\"SELF\"}],\"reason\":\"临时查看\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.authorizationVersion").value(1));
        assertEquals("GRANT",jdbcTemplate.queryForObject("SELECT effect FROM t_user_permission WHERE user_id=2 AND permission_id=?",String.class,permission));
        mockMvc.perform(put("/api/users/2/authorization/permissions").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":1,\"changes\":[{\"permissionId\":"+permission+",\"state\":\"DENY\"}],\"reason\":\"立即拒绝\"}"))
                .andExpect(status().isOk());
        assertEquals("DENY",jdbcTemplate.queryForObject("SELECT effect FROM t_user_permission WHERE user_id=2 AND permission_id=?",String.class,permission));
    }

    @Test void personalCustomOrganizationScopeIsStoredAsIndependentPermissionSource() throws Exception {
        int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code='user:view'",Integer.class);
        int targetOrganization=jdbcTemplate.queryForObject("""
                SELECT assignment.organization_unit_id FROM t_employee employee
                JOIN t_employee_assignment assignment ON assignment.employee_id=employee.id
                WHERE employee.user_id=2 AND assignment.active_primary_marker=1
                """,Integer.class);
        String code="USER_CUSTOM_ORG_"+System.nanoTime();
        jdbcTemplate.update("INSERT INTO t_organization_unit(code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?,?,'STORE',?,1,0,1,0,CURRENT_TIMESTAMP,1)",code,code,targetOrganization);
        int organization=jdbcTemplate.queryForObject("SELECT id FROM t_organization_unit WHERE code=?",Integer.class,code);
        mockMvc.perform(put("/api/users/2/authorization/permissions").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":0,\"changes\":[{\"permissionId\":"+permission+",\"state\":\"GRANT\",\"dataScopeCandidateKey\":\"CUSTOM_ORGS\",\"customOrganizationUnitIds\":["+organization+"]}],\"reason\":\"指定门店临时权限\"}"))
                .andExpect(status().isOk());
        long personalPermissionId=jdbcTemplate.queryForObject("SELECT id FROM t_user_permission WHERE user_id=2 AND permission_id=?",Long.class,permission);
        assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_permission_organization WHERE user_permission_id=? AND organization_unit_id=?",Integer.class,personalPermissionId,organization));
    }

    @Test void futurePersonalPermissionIsVisibleAsPendingButNotEffectiveAndCancellationKeepsPlan() throws Exception {
        String code="runtime:scheduled-api:"+System.nanoTime();
        jdbcTemplate.update("INSERT INTO t_permission(name,code,type,module,sensitivity_level,delegable,enabled,version) VALUES(?,?,'button','access','NORMAL',1,1,0)",code,code);
        int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code=?",Integer.class,code);
        grantAdminRolePermission(permission);
        OffsetDateTime planned=OffsetDateTime.now().plusDays(7).withNano(0);
        OffsetDateTime expires=planned.plusDays(3);

        MvcResult scheduled=mockMvc.perform(put("/api/users/2/authorization/permissions").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":0,\"changes\":[{\"permissionId\":"+permission+",\"state\":\"GRANT\",\"dataScopeCandidateKey\":\"SELF\",\"effectiveFrom\":\""+planned+"\",\"effectiveTo\":\""+expires+"\"}],\"reason\":\"预约临时权限\"}"))
                .andExpect(status().isOk()).andReturn();

        var item=StreamSupport.stream(objectMapper.readTree(scheduled.getResponse().getContentAsString())
                        .path("data").path("permissions").spliterator(),false)
                .filter(node->node.path("permissionId").asInt()==permission).findFirst().orElseThrow();
        assertEquals("GRANT",item.path("personalState").asText());
        assertEquals(false,item.path("effective").asBoolean());
        assertEquals(false,item.path("sources").get(0).path("active").asBoolean());
        long factId=jdbcTemplate.queryForObject("SELECT id FROM t_user_permission WHERE user_id=2 AND permission_id=?",Long.class,permission);
        LocalDateTime persistedPlan=jdbcTemplate.queryForObject("SELECT effective_from FROM t_user_permission WHERE id=?",LocalDateTime.class,factId);
        assertEquals(LocalDateTime.ofInstant(planned.toInstant(),ZoneId.systemDefault()),persistedPlan);

        MvcResult canceled=mockMvc.perform(put("/api/users/2/authorization/permissions").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":1,\"changes\":[{\"permissionId\":"+permission+",\"state\":\"INHERIT\"}],\"reason\":\"取消预约权限\"}"))
                .andExpect(status().isOk()).andReturn();
        var canceledItem=StreamSupport.stream(objectMapper.readTree(canceled.getResponse().getContentAsString())
                        .path("data").path("permissions").spliterator(),false)
                .filter(node->node.path("permissionId").asInt()==permission).findFirst().orElseThrow();
        assertEquals("INHERIT",canceledItem.path("personalState").asText());
        assertEquals(persistedPlan,jdbcTemplate.queryForObject("SELECT effective_from FROM t_user_permission WHERE id=?",LocalDateTime.class,factId));
        assertEquals(null,jdbcTemplate.queryForObject("SELECT active_marker FROM t_user_permission WHERE id=?",Boolean.class,factId));
    }

    @Test void batchRoleAssignmentUpdatesEveryTargetAndWritesOneBatchAudit() throws Exception {
        int role=insertRole("batch_role");int firstVersion=authorizationVersion(2);int secondVersion=authorizationVersion(3);
        mockMvc.perform(put("/api/users/authorization/batch/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targets\":[{\"userId\":2,\"authorizationVersion\":"+firstVersion+"},{\"userId\":3,\"authorizationVersion\":"+secondVersion+"}],\"operation\":\"ASSIGN\",\"roleIds\":["+role+"],\"reason\":\"批量补充角色\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.targetCount").value(2))
                .andExpect(jsonPath("$.data.changedTargetCount").value(2));
        assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id IN (2,3) AND role_id=? AND active_marker=1",Integer.class,role));
        assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE target_user_id IN (2,3) AND role_id=? AND change_type='ASSIGN'",Integer.class,role));
        verify(operationAuditRecorder).record(eq(AuditActionEnum.USER_ROLE_CHANGE),eq("batch:ROLE_ASSIGN"),eq("SUCCESS"),contains("\"totalCount\":2"));
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void staleTargetRollsBackEntireBatchRoleAssignment() throws Exception {
        int role=insertRole("batch_rollback_role");int firstVersion=authorizationVersion(2);int secondVersion=authorizationVersion(3);
        mockMvc.perform(put("/api/users/authorization/batch/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targets\":[{\"userId\":2,\"authorizationVersion\":"+firstVersion+"},{\"userId\":3,\"authorizationVersion\":"+(secondVersion+99)+"}],\"operation\":\"ASSIGN\",\"roleIds\":["+role+"],\"reason\":\"验证整批回滚\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(600));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id IN (2,3) AND role_id=?",Integer.class,role));
        assertEquals(firstVersion,authorizationVersion(2));assertEquals(secondVersion,authorizationVersion(3));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE target_user_id IN (2,3) AND role_id=?",Integer.class,role));
    }

    @Test void batchPermissionGrantUpdatesEveryTargetAtomically() throws Exception {
        String code="runtime:batch-permission:"+System.nanoTime();
        jdbcTemplate.update("INSERT INTO t_permission(name,code,type,module,sensitivity_level,delegable,enabled,version) VALUES(?,?,'button','access','NORMAL',1,1,0)",code,code);
        int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code=?",Integer.class,code);
        grantAdminRolePermission(permission);
        int firstVersion=authorizationVersion(2);int secondVersion=authorizationVersion(3);
        mockMvc.perform(put("/api/users/authorization/batch/permissions").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targets\":[{\"userId\":2,\"authorizationVersion\":"+firstVersion+"},{\"userId\":3,\"authorizationVersion\":"+secondVersion+"}],\"changes\":[{\"permissionId\":"+permission+",\"state\":\"GRANT\",\"dataScopeCandidateKey\":\"SELF\"}],\"reason\":\"批量临时授权\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.changedTargetCount").value(2));
        assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_permission WHERE user_id IN (2,3) AND permission_id=? AND active_marker=1",Integer.class,permission));
        assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_authorization_history WHERE target_user_id IN (2,3) AND permission_id=? AND change_type='GRANT'",Integer.class,permission));
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void batchCannotRemoveAllAvailableOrdinaryAdministrators() throws Exception {
        int adminRole=jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role='admin'",Integer.class);
        int operatorId=93;int operatorRoleId=insertQualifiedSecurityOperator(operatorId,"batch_security_operator","user:permission","user:role");
        insertAvailableAdmin(91,991);insertAvailableAdmin(92,992);
        String operatorToken=loginAs("batch_security_operator","123456",operatorId);
        jdbcTemplate.update("UPDATE t_user SET account_enabled=0,account_status='DISABLED' WHERE id=?",QUALIFIED_ADMIN_USER_ID);
        try {
            mockMvc.perform(put("/api/users/authorization/batch/roles").header(HttpHeaders.AUTHORIZATION,operatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targets\":[{\"userId\":91,\"authorizationVersion\":0},{\"userId\":92,\"authorizationVersion\":0}],\"operation\":\"UNASSIGN\",\"roleIds\":["+adminRole+"],\"reason\":\"禁止清空普通管理入口\"}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(605));
            assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id IN (91,92) AND role_id=? AND active_marker=1",Integer.class,adminRole));
            assertEquals(2,userMapper.countAdminUsers());
        } finally {
            jdbcTemplate.update("UPDATE t_user SET account_enabled=1,account_status='ACTIVE' WHERE id=?",QUALIFIED_ADMIN_USER_ID);
            jdbcTemplate.update("DELETE FROM t_user_session WHERE user_id=?",operatorId);
            jdbcTemplate.update("DELETE FROM t_user_permission WHERE user_id=?",operatorId);
            jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id IN (91,92,?)",operatorId);
            jdbcTemplate.update("DELETE FROM t_role_permission WHERE role_id=?",operatorRoleId);
            jdbcTemplate.update("DELETE FROM t_employee_assignment WHERE employee_id IN (91,92,?)",operatorId);
            jdbcTemplate.update("DELETE FROM t_employee WHERE id IN (91,92,?)",operatorId);
            jdbcTemplate.update("DELETE FROM t_login_identifier WHERE user_id IN (91,92,?)",operatorId);
            jdbcTemplate.update("DELETE FROM t_user WHERE id IN (91,92,?)",operatorId);
            jdbcTemplate.update("DELETE FROM t_position WHERE id IN (991,992)");
            jdbcTemplate.update("DELETE FROM t_organization_unit WHERE id IN (991,992)");
            jdbcTemplate.update("DELETE FROM t_role WHERE id=?",operatorRoleId);
        }
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void auditFailureRollsBackRoleFactsAndSecurityVersion() throws Exception {
        int newRole=insertRole("audit_rollback_role");
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_type,protected_account,version,auth_version) VALUES(96,'audit_target','x','审计目标',1,1,1,1,'HUMAN',0,0,0)");
        doThrow(new IllegalStateException("审计不可用")).when(operationAuditRecorder).record(any(),anyString(),anyString(),anyString());
        mockMvc.perform(put("/api/users/96/authorization/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":0,\"roleIds\":["+newRole+"],\"reason\":\"审计回滚\"}"))
                .andExpect(status().isInternalServerError());
        assertEquals(0,jdbcTemplate.queryForObject("SELECT authorization_version FROM t_user WHERE id=96",Integer.class));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=96 AND role_id=?",Integer.class,newRole));
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void redisCleanupFailureDoesNotRestoreOldTokenAuthorization() throws Exception {
        int newRole=insertRole("redis_failure_role");
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_type,protected_account,version,auth_version) VALUES(95,'redis_target','x','缓存目标',1,1,1,1,'HUMAN',0,0,0)");
        when(redisManager.delete(anyString())).thenReturn(false);
        mockMvc.perform(put("/api/users/95/authorization/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationVersion\":0,\"roleIds\":["+newRole+"],\"reason\":\"缓存失败\"}"))
                .andExpect(status().isOk());
        assertEquals(1,jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=95",Integer.class));
        verify(redisManager,times(2)).delete("cdrm:user:login:95");
    }

    @Test void authorizationDetailCanBeReadButLegacyRoleEndpointStaysClosed() throws Exception {
        mockMvc.perform(get("/api/users/2/authorization").header(HttpHeaders.AUTHORIZATION,adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.id").value(2));
        mockMvc.perform(put("/api/user/2/roles").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test void ordinaryUserCanReadOwnEffectiveAuthorizationWithoutUserViewPermission() throws Exception {
        String ownToken=loginAs("zhangsan","123456",2);
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_permission permission INNER JOIN t_role_permission relation ON relation.permission_id=permission.id INNER JOIN t_user_role user_role ON user_role.role_id=relation.role_id WHERE user_role.user_id=2 AND permission.code='user:view'",Integer.class));

        mockMvc.perform(get("/api/users/2/authorization").header(HttpHeaders.AUTHORIZATION,ownToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(2))
                .andExpect(jsonPath("$.data.permissions").isArray());
        mockMvc.perform(get("/api/users/3/authorization").header(HttpHeaders.AUTHORIZATION,ownToken))
                .andExpect(status().isForbidden());
    }

    @Test void allLegacyUserWriteEntrypointsFailClosedUntilDedicatedCommandsExist() throws Exception {
        mockMvc.perform(post("/api/user").header(HttpHeaders.AUTHORIZATION,adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginAct\":\"legacy\",\"loginPwd\":\"123456\",\"name\":\"旧入口\",\"phone\":\"13900000000\",\"email\":\"legacy@test.com\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION,adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2,\"loginAct\":\"zhangsan\",\"name\":\"张三\",\"phone\":\"13800000001\",\"email\":\"zhangsan@test.com\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/user/2/password").header(HttpHeaders.AUTHORIZATION,adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"newPassword\":\"123456\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/user/2/handover").header(HttpHeaders.AUTHORIZATION,adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":3,\"reason\":\"旧交接入口\"}"))
                .andExpect(status().isForbidden());
        for(String command:List.of("disable","enable","lock","unlock"))
            mockMvc.perform(put("/api/user/2/"+command).header(HttpHeaders.AUTHORIZATION,adminToken))
                    .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/batch-disable").header(HttpHeaders.AUTHORIZATION,adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[2]}"))
                .andExpect(status().isForbidden());
    }

    private int authorizationVersion(int userId){return jdbcTemplate.queryForObject("SELECT authorization_version FROM t_user WHERE id=?",Integer.class,userId);}
    private void grantAdminRolePermission(int permissionId){jdbcTemplate.update("INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code) SELECT id,?,1,'GLOBAL' FROM t_role WHERE role='admin'",permissionId);}
    private void insertAvailableAdmin(int userId,int baseId){
        jdbcTemplate.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?,?,?,'DEPARTMENT',1,1,0,1,0,CURRENT_TIMESTAMP,1)",baseId,"BATCH_ADMIN_ORG_"+baseId,"批量管理员组织"+baseId);
        jdbcTemplate.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?,?,?,100,0,1,0,CURRENT_TIMESTAMP,1)",baseId,"BATCH_ADMIN_POSITION_"+baseId,"批量管理员岗位"+baseId);
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,manual_locked,version,authorization_version,auth_version,session_revision,create_time,create_by) VALUES(?,?, 'x',?,1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0,CURRENT_TIMESTAMP,1)",userId,"batch_admin_"+userId,"普通管理员"+userId);
        jdbcTemplate.update("INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(?,?,?,?,?,'ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)",userId,userId,"EMP-BATCH-"+userId,"普通管理员"+userId,"139"+String.format("%08d",userId));
        jdbcTemplate.update("INSERT INTO t_employee_assignment(id,employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,?, 'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'批量管理员保护测试',0,CURRENT_TIMESTAMP,1)",baseId,userId,baseId,baseId);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) SELECT ?,id,1,'批量管理员保护测试',CURRENT_TIMESTAMP,1,0 FROM t_role WHERE role='admin'",userId);
    }
    private int insertRole(String prefix){String code=prefix+System.nanoTime();jdbcTemplate.update("INSERT INTO t_role(role,role_name,description,protected_role,authorization_level,default_data_scope,scope_type,enabled,version) VALUES(?,?,?,0,10,'SELF','GLOBAL',1,0)",code,code,code);return jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role=?",Integer.class,code);}
}
