package com.autodealer.crm.integration;

import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserLifecycleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class EffectiveAuthorizationIntegrationTest extends BackendIntegrationTestBase {
    @Autowired TPermissionMapper permissionMapper;
    @Autowired SqlSession sqlSession;
    @Autowired TUserPermissionMapper userPermissions;
    @Autowired TUserRoleMapper userRoles;
    @Autowired TUserLifecycleMapper lifecycle;

    @Test
    void personalGrantIsEffectiveDenyWinsAndExpiredGrantDisappears() {
        String permissionCode="runtime:personal:"+System.nanoTime();
        int permissionId=insertPermission(permissionCode);
        jdbcTemplate.update("""
                INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,
                  effective_to,reason,granted_by,version,create_time)
                VALUES(2,?,'GRANT','SELF',CURRENT_TIMESTAMP-INTERVAL '1' SECOND,NULL,'集成授权',1,0,CURRENT_TIMESTAMP)
                """,permissionId);
        assertTrue(buttonCodes(2).contains(permissionCode));

        jdbcTemplate.update("UPDATE t_user_permission SET effect='DENY',data_scope_code=NULL,version=version+1 WHERE user_id=2 AND permission_id=?",permissionId);
        sqlSession.clearCache();
        assertFalse(buttonCodes(2).contains(permissionCode));

        jdbcTemplate.update("""
                UPDATE t_user_permission SET effect='GRANT',data_scope_code='SELF',
                  effective_from=CURRENT_TIMESTAMP-INTERVAL '2' DAY,effective_to=CURRENT_TIMESTAMP-INTERVAL '1' DAY
                WHERE user_id=2 AND permission_id=?
                """,permissionId);
        sqlSession.clearCache();
        assertFalse(buttonCodes(2).contains(permissionCode));
    }

    @Test
    void organizationRoleAppliesToChildAndStopsImmediatelyAfterCrossTreeTransfer() {
        int store=insertOrganization("AUTH_STORE",1,"STORE");
        int team=insertOrganization("AUTH_TEAM",store,"TEAM");
        int otherStore=insertOrganization("AUTH_OTHER",1,"STORE");
        int position=insertPosition("AUTH_POSITION");
        moveEmployee(1,team,position);
        int roleId=insertRole("org_runtime_role","ORGANIZATION");
        String permissionCode="runtime:organization:"+System.nanoTime();
        int permissionId=insertPermission(permissionCode);
        jdbcTemplate.update("INSERT INTO t_role_organization(role_id,organization_unit_id) VALUES(?,?)",roleId,store);
        jdbcTemplate.update("INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code) VALUES(?,?,1,'SELF')",roleId,permissionId);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(2,?,1,'组织角色',CURRENT_TIMESTAMP,1,0)",roleId);

        assertTrue(buttonCodes(2).contains(permissionCode),"父组织适用角色必须覆盖当前子组织");
        moveEmployee(1,otherStore,position);
        sqlSession.clearCache();
        assertFalse(buttonCodes(2).contains(permissionCode),"调岗到适用组织树外后角色必须立即失效");
    }

    @Test
    void futureRoleAssignmentDoesNotEnterRuntimeAuthorities() {
        int roleId=insertRole("future_runtime_role","GLOBAL");
        String permissionCode="runtime:future:"+System.nanoTime();
        int permissionId=insertPermission(permissionCode);
        jdbcTemplate.update("INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code) VALUES(?,?,1,'SELF')",roleId,permissionId);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(2,?,1,'未来角色',CURRENT_TIMESTAMP+INTERVAL '1' DAY,1,0)",roleId);
        assertFalse(buttonCodes(2).contains(permissionCode));
    }

    @Test
    void canceledFutureRoleAndPersonalPermissionNeverActivateAndKeepOriginalPlannedTime() {
        LocalDateTime now=LocalDateTime.now();LocalDateTime planned=now.plusDays(1);LocalDateTime afterPlan=planned.plusSeconds(1);
        String permissionCode="runtime:canceled-future:"+System.nanoTime();
        int permissionId=insertPermission(permissionCode);
        int roleId=insertRole("canceled_future_role","GLOBAL");
        int roleCountBefore=lifecycle.countCurrentAndFutureRoles(2,now);int permissionCountBefore=lifecycle.countCurrentAndFuturePermissions(2,now);
        jdbcTemplate.update("INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code) VALUES(?,?,1,'SELF')",roleId,permissionId);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(2,?,1,'取消未来角色',?,1,0)",roleId,planned);
        jdbcTemplate.update("INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,effective_to,active_marker,reason,granted_by,version,create_time) VALUES(2,?,'GRANT','SELF',?,NULL,1,'取消未来个人权限',1,0,CURRENT_TIMESTAMP)",permissionId,planned);
        TUserRole role=userRoles.selectCurrentAndFutureByUserId(2,now).stream().filter(value->value.getRoleId()==roleId).findFirst().orElseThrow();
        TUserPermission permission=userPermissions.selectCurrentAndFutureByUserId(2,now).stream().filter(value->value.getPermissionId()==permissionId).findFirst().orElseThrow();

        assertEquals(1,userRoles.closeByIdAndVersion(role.getId(),role.getVersion(),now));
        assertEquals(1,userPermissions.closeByIdAndVersion(permission.getId(),permission.getVersion(),now));
        sqlSession.clearCache();

        assertEquals(planned,jdbcTemplate.queryForObject("SELECT effective_from FROM t_user_role WHERE id=?",LocalDateTime.class,role.getId()));
        assertEquals(planned,jdbcTemplate.queryForObject("SELECT effective_from FROM t_user_permission WHERE id=?",LocalDateTime.class,permission.getId()));
        assertNull(jdbcTemplate.queryForObject("SELECT active_marker FROM t_user_role WHERE id=?",Boolean.class,role.getId()));
        assertNull(jdbcTemplate.queryForObject("SELECT active_marker FROM t_user_permission WHERE id=?",Boolean.class,permission.getId()));
        assertTrue(userRoles.selectEffectiveByUserId(2,afterPlan).stream().noneMatch(value->value.getRoleId()==roleId));
        assertTrue(userPermissions.selectEffectiveByUserId(2,afterPlan).stream().noneMatch(value->value.getPermissionId()==permissionId));
        assertEquals(roleCountBefore,lifecycle.countCurrentAndFutureRoles(2,now));
        assertEquals(permissionCountBefore,lifecycle.countCurrentAndFuturePermissions(2,now));
        assertFalse(buttonCodes(2).contains(permissionCode),"失活未来授权不得进入运行时权限");
    }

    @Test
    void canceledFutureGrantAndDenyDoNotChangeHandoverCandidateAtPlannedTime() {
        LocalDateTime now=LocalDateTime.now();LocalDateTime planned=now.plusDays(1);LocalDateTime afterPlan=planned.plusSeconds(1);
        jdbcTemplate.update("UPDATE t_user SET account_enabled=1,account_no_locked=1,account_status='ACTIVE',account_no_expired=1,credentials_no_expired=1,password_expires_at=NULL,account_type='HUMAN',protected_account=0,manual_locked=0,auto_locked_until=NULL WHERE id=3");
        jdbcTemplate.update("UPDATE t_employee SET employment_status='ACTIVE' WHERE id=2");
        int candidateOrganization=insertOrganization("CANDIDATE_ORG",1,"STORE");
        int candidatePosition=insertPosition("CANDIDATE_POSITION");
        moveEmployee(2,candidateOrganization,candidatePosition);
        String grantCode="runtime:candidate-future-grant:"+System.nanoTime();int grantId=insertPermission(grantCode);
        jdbcTemplate.update("INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,active_marker,reason,granted_by,version,create_time) VALUES(3,?,'GRANT','SELF',?,1,'候选未来允许',1,0,CURRENT_TIMESTAMP)",grantId,planned);
        TUserPermission grant=userPermissions.selectCurrentAndFutureByUserId(3,now).stream().filter(value->value.getPermissionId()==grantId).findFirst().orElseThrow();
        assertEquals(1,userPermissions.closeByIdAndVersion(grant.getId(),grant.getVersion(),now));
        assertTrue(lifecycle.selectQualifiedCandidates(2,List.of(grantCode),afterPlan).stream().noneMatch(value->value.getId()==2),
                "已取消的未来GRANT不得在计划时刻赋予接收资格");

        String denyCode="runtime:candidate-future-deny:"+System.nanoTime();int denyId=insertPermission(denyCode);
        int qualifyingRole=insertRole("candidate_deny_role","GLOBAL");
        jdbcTemplate.update("INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code) VALUES(?,?,1,'SELF')",qualifyingRole,denyId);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(3,?,1,'候选资格角色',CURRENT_TIMESTAMP-INTERVAL '1' SECOND,1,0)",qualifyingRole);
        assertTrue(lifecycle.selectQualifiedCandidates(2,List.of(denyCode),afterPlan).stream().anyMatch(value->value.getId()==2),
                "测试前置：没有个人DENY时目标应具备角色接收资格");
        jdbcTemplate.update("INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,active_marker,reason,granted_by,version,create_time) VALUES(3,?,'DENY',NULL,?,1,'候选未来拒绝',1,0,CURRENT_TIMESTAMP)",denyId,planned);
        TUserPermission deny=userPermissions.selectCurrentAndFutureByUserId(3,now).stream().filter(value->value.getPermissionId()==denyId).findFirst().orElseThrow();
        assertEquals(1,userPermissions.closeByIdAndVersion(deny.getId(),deny.getVersion(),now));
        assertTrue(lifecycle.selectQualifiedCandidates(2,List.of(denyCode),afterPlan).stream().anyMatch(value->value.getId()==2),
                "已取消的未来DENY不得在计划时刻排除原本合格的接收人");
    }

    private List<String> buttonCodes(int userId){return permissionMapper.selectButtonPermissionByUserId(userId).stream().map(TPermission::getCode).toList();}
    private int insertPermission(String code){jdbcTemplate.update("INSERT INTO t_permission(name,code,type,module,sensitivity_level,delegable,enabled,version) VALUES(?,?,'button','access','NORMAL',1,1,0)",code,code);return jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code=?",Integer.class,code);}
    private int insertOrganization(String prefix,int parent,String type){String code=prefix+System.nanoTime();jdbcTemplate.update("INSERT INTO t_organization_unit(code,name,type,parent_id,order_no,placeholder,enabled,version,create_time,create_by) VALUES(?,?,?, ?,1,0,1,0,CURRENT_TIMESTAMP,1)",code,code,type,parent);return jdbcTemplate.queryForObject("SELECT id FROM t_organization_unit WHERE code=?",Integer.class,code);}
    private int insertPosition(String prefix){String code=prefix+System.nanoTime();jdbcTemplate.update("INSERT INTO t_position(code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?,?,10,0,1,0,CURRENT_TIMESTAMP,1)",code,code);return jdbcTemplate.queryForObject("SELECT id FROM t_position WHERE code=?",Integer.class,code);}
    private int insertRole(String prefix,String scopeType){String code=prefix+System.nanoTime();jdbcTemplate.update("INSERT INTO t_role(role,role_name,description,protected_role,authorization_level,default_data_scope,scope_type,enabled,version) VALUES(?,?,?,0,10,'SELF',?,1,0)",code,code,code,scopeType);return jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role=?",Integer.class,code);}
    private void moveEmployee(int employeeId,int orgId,int positionId){jdbcTemplate.update("UPDATE t_employee_assignment SET status='ENDED',active_primary_marker=NULL,effective_to=CURRENT_TIMESTAMP WHERE employee_id=? AND active_primary_marker=1",employeeId);jdbcTemplate.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP-INTERVAL '1' SECOND,'运行时范围测试',0,CURRENT_TIMESTAMP,1)",employeeId,orgId,positionId);}
}
