package com.autodealer.crm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class RolePermissionIntegrationTest extends BackendIntegrationTestBase {
  private String token;
  @BeforeEach void login() throws Exception {token=loginAsQualifiedAdmin();}

  @Test void customRoleCrudPreviewAndAtomicMatrixReplacement() throws Exception {
    String code="custom_"+Math.abs(System.nanoTime()%1000000);
    String create="{\"code\":\"%s\",\"name\":\"自定义角色\",\"description\":\"测试\",\"authorizationLevel\":20,\"defaultDataScope\":\"SELF\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[]}".formatted(code);
    JsonNode created=body(mockMvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON).content(create))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value(code)).andReturn().getResponse().getContentAsString()).path("data");
    int id=created.path("id").asInt(); assertEquals(0,created.path("version").asInt());
    int permissionId=jdbcTemplate.queryForObject("select id from t_permission where code='organization:list'",Integer.class);
    String matrix="{\"expectedVersion\":0,\"permissionIds\":["+permissionId+"],\"permissionScopes\":[{\"permissionId\":"+permissionId+",\"dataScopeCode\":\"SELF\",\"organizationUnitIds\":[]}]}";
    mockMvc.perform(post("/api/roles/"+id+"/permissions/preview").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON).content(matrix))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.addedPermissions[0].permissionId").value(permissionId));
    mockMvc.perform(put("/api/roles/"+id+"/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"permissionIds\":["+permissionId+"],\"permissionScopes\":[{\"permissionId\":"+permissionId+",\"dataScopeCode\":\"SELF\",\"organizationUnitIds\":[]}],\"reason\":\"配置角色权限\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
    assertEquals(1,jdbcTemplate.queryForObject("select count(*) from t_role_permission where role_id=?",Integer.class,id));
    assertTrue(jdbcTemplate.queryForObject("select count(*) from t_authorization_history where role_id=?",Integer.class,id)>0);
    mockMvc.perform(put("/api/roles/"+id+"/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":1,\"permissionIds\":["+permissionId+"],\"permissionScopes\":[{\"permissionId\":"+permissionId+",\"dataScopeCode\":\"PRIMARY_ORG\",\"organizationUnitIds\":[]}],\"reason\":\"只调整范围\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2));
    assertEquals("PRIMARY_ORG",jdbcTemplate.queryForObject("select data_scope_code from t_role_permission where role_id=? and permission_id=?",String.class,id,permissionId));
    assertEquals(1,jdbcTemplate.queryForObject("select count(*) from t_authorization_history where role_id=? and permission_id=? and change_type='UPDATE'",Integer.class,id,permissionId));
    mockMvc.perform(put("/api/roles/"+id+"/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"permissionIds\":[],\"permissionScopes\":[],\"reason\":\"过期写入\"}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(600));
  }

  @Test void protectedRoleAndUnknownOrDisabledPermissionAreRejected() throws Exception {
    mockMvc.perform(put("/api/roles/1/disable").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"reason\":\"禁止\"}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(601));
    String code="invalid_"+Math.abs(System.nanoTime()%1000000);
    JsonNode created=body(mockMvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\"%s\",\"name\":\"无效权限测试\",\"authorizationLevel\":10,\"defaultDataScope\":\"SELF\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[]}".formatted(code)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    mockMvc.perform(post("/api/roles/"+created.path("id").asInt()+"/permissions/preview").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"permissionIds\":[999999],\"permissionScopes\":[{\"permissionId\":999999,\"dataScopeCode\":\"SELF\",\"organizationUnitIds\":[]}]}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(602));
  }

  @Test void permissionCatalogIsReadOnlyAndContainsMetadata() throws Exception {
    mockMvc.perform(get("/api/permissions/tree").header(HttpHeaders.AUTHORIZATION,token))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.data[0].children").isArray());
    int before=jdbcTemplate.queryForObject("select count(*) from t_permission",Integer.class);
    mockMvc.perform(post("/api/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().is5xxServerError());
    assertEquals(before,jdbcTemplate.queryForObject("select count(*) from t_permission",Integer.class));
  }

  @Test void protectedRoleCannotBeCopiedAndOrdinaryManagerCannotCopyGlobalOrRangeOutsideRole() throws Exception {
    mockMvc.perform(post("/api/roles/1/copy").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\"copy_admin\",\"name\":\"复制管理员\",\"authorizationLevel\":10,\"defaultDataScope\":\"SELF\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[],\"reason\":\"禁止复制\"}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(601));

    int managerOrg=insertOrganization("ROLE_MANAGER_ORG",1,"STORE");int foreignOrg=insertOrganization("ROLE_FOREIGN_ORG",1,"STORE");int position=insertPosition("ROLE_MANAGER_POS");moveEmployee(2,managerOrg,position);
    grantRoleAccess("sales_manager","role:list","role:view","role:add","role:copy");
    String managerToken=loginAs("lisi","123456",3);
    String sourceCode="outside_"+System.nanoTime();
    jdbcTemplate.update("INSERT INTO t_role(role,role_name,description,protected_role,authorization_level,default_data_scope,scope_type,enabled,version) VALUES(?,?,?,0,10,'SELF','ORGANIZATION',1,0)",sourceCode,sourceCode,sourceCode);
    int sourceId=jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role=?",Integer.class,sourceCode);
    jdbcTemplate.update("INSERT INTO t_role_organization(role_id,organization_unit_id) VALUES(?,?)",sourceId,foreignOrg);
    mockMvc.perform(post("/api/roles/"+sourceId+"/copy").header(HttpHeaders.AUTHORIZATION,managerToken).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\"copy_outside\",\"name\":\"越界复制\",\"authorizationLevel\":5,\"defaultDataScope\":\"SELF\",\"scopeType\":\"ORGANIZATION\",\"organizationUnitIds\":["+managerOrg+"],\"reason\":\"越界\"}"))
      .andExpect(status().isForbidden());
    jdbcTemplate.update("DELETE FROM t_role_organization WHERE role_id=?",sourceId);jdbcTemplate.update("INSERT INTO t_role_organization(role_id,organization_unit_id) VALUES(?,?)",sourceId,managerOrg);
    mockMvc.perform(post("/api/roles/"+sourceId+"/copy").header(HttpHeaders.AUTHORIZATION,managerToken).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\"copy_global\",\"name\":\"扩大范围\",\"authorizationLevel\":5,\"defaultDataScope\":\"GLOBAL\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[],\"reason\":\"扩大\"}"))
      .andExpect(status().isForbidden());
  }

  @Test void roleDefaultScopeChangeDoesNotRewriteExplicitPermissionScopeAndFutureMemberBlocksDisable() throws Exception {
    String code="scope_change_"+System.nanoTime();
    JsonNode created=body(mockMvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\""+code+"\",\"name\":\"范围变更\",\"authorizationLevel\":10,\"defaultDataScope\":\"SELF\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[]}"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    int roleId=created.path("id").asInt();int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code='organization:list'",Integer.class);
    mockMvc.perform(put("/api/roles/"+roleId+"/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"permissionIds\":["+permission+"],\"permissionScopes\":[{\"permissionId\":"+permission+",\"dataScopeCode\":\"SELF\",\"organizationUnitIds\":[]}],\"reason\":\"初始矩阵\"}")).andExpect(status().isOk());
    mockMvc.perform(put("/api/roles/"+roleId).header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":1,\"name\":\"范围变更\",\"authorizationLevel\":10,\"defaultDataScope\":\"PRIMARY_ORG\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[]}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2));
    assertEquals("SELF",jdbcTemplate.queryForObject("SELECT data_scope_code FROM t_role_permission WHERE role_id=? AND permission_id=?",String.class,roleId,permission));
    long authBefore=jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=2",Long.class);
    jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(2,?,1,'未来成员',CURRENT_TIMESTAMP+INTERVAL '1' DAY,1,0)",roleId);
    mockMvc.perform(put("/api/roles/"+roleId+"/disable").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":2,\"reason\":\"存在未来成员时拒绝停用\"}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(604));
    assertEquals(authBefore,jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=2",Long.class));
    assertEquals(1,jdbcTemplate.queryForObject("SELECT enabled FROM t_role WHERE id=?",Integer.class,roleId));
  }

  @Test void customOrganizationScopeIsPersistedPerRolePermissionSource() throws Exception {
    int org=insertOrganization("CUSTOM_SCOPE_ORG",1,"STORE");int sourceOrg=insertOrganization("CUSTOM_SOURCE_ORG",org,"TEAM");String code="custom_scope_"+System.nanoTime();
    JsonNode created=body(mockMvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\""+code+"\",\"name\":\"指定组织\",\"authorizationLevel\":10,\"defaultDataScope\":\"CUSTOM_ORGS\",\"scopeType\":\"ORGANIZATION\",\"organizationUnitIds\":["+org+"]}"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    int roleId=created.path("id").asInt();int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code='organization:list'",Integer.class);
    mockMvc.perform(put("/api/roles/"+roleId+"/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"permissionIds\":["+permission+"],\"permissionScopes\":[{\"permissionId\":"+permission+",\"dataScopeCode\":\"CUSTOM_ORGS\",\"organizationUnitIds\":["+sourceOrg+"]}],\"reason\":\"指定组织矩阵\"}")).andExpect(status().isOk());
    assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_role_permission_organization WHERE role_id=? AND permission_id=? AND organization_unit_id=?",Integer.class,roleId,permission,sourceOrg));
    assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_role_permission_organization WHERE role_id=? AND permission_id=? AND organization_unit_id=?",Integer.class,roleId,permission,org));
    mockMvc.perform(post("/api/roles/"+roleId+"/permissions/preview").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":1,\"permissionIds\":["+permission+"],\"permissionScopes\":[{\"permissionId\":"+permission+",\"dataScopeCode\":\"SELF\",\"organizationUnitIds\":[]}]}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.scopeDifferences[0].beforeDataScopeCode").value("CUSTOM_ORGS"))
      .andExpect(jsonPath("$.data.scopeDifferences[0].afterDataScopeCode").value("SELF"));
    String copyCode="custom_scope_copy_"+System.nanoTime();
    JsonNode copied=body(mockMvc.perform(post("/api/roles/"+roleId+"/copy").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\""+copyCode+"\",\"name\":\"复制指定组织\",\"authorizationLevel\":10,\"defaultDataScope\":\"SELF\",\"scopeType\":\"ORGANIZATION\",\"organizationUnitIds\":["+org+"],\"reason\":\"保留显式来源\"}"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    int copiedRoleId=copied.path("id").asInt();
    assertEquals("CUSTOM_ORGS",jdbcTemplate.queryForObject("SELECT data_scope_code FROM t_role_permission WHERE role_id=? AND permission_id=?",String.class,copiedRoleId,permission));
    assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_role_permission_organization WHERE role_id=? AND permission_id=? AND organization_unit_id=?",Integer.class,copiedRoleId,permission,sourceOrg));
  }

  @Test void matrixHistoryKeepsEventTimeMembersAfterLaterRoleRemovalAndAddition() throws Exception {
    String code="event_members_"+System.nanoTime();
    JsonNode created=body(mockMvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"code\":\""+code+"\",\"name\":\"事件成员角色\",\"authorizationLevel\":10,\"defaultDataScope\":\"SELF\",\"scopeType\":\"GLOBAL\",\"organizationUnitIds\":[]}"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    int roleId=created.path("id").asInt();
    int baseRole=jdbcTemplate.queryForObject("SELECT role_id FROM t_user_role WHERE user_id=2 AND active_marker=1 ORDER BY role_id LIMIT 1",Integer.class);
    int version2=jdbcTemplate.queryForObject("SELECT authorization_version FROM t_user WHERE id=2",Integer.class);
    mockMvc.perform(put("/api/users/2/authorization/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"authorizationVersion\":"+version2+",\"roleIds\":["+baseRole+","+roleId+"],\"reason\":\"加入事件成员\"}"))
      .andExpect(status().isOk());
    int permission=jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code='organization:list'",Integer.class);
    mockMvc.perform(put("/api/roles/"+roleId+"/permissions").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"expectedVersion\":0,\"permissionIds\":["+permission+"],\"permissionScopes\":[{\"permissionId\":"+permission+",\"dataScopeCode\":\"SELF\",\"organizationUnitIds\":[]}],\"reason\":\"记录事件成员\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.affectedUserCount").value(1));
    String snapshot=jdbcTemplate.queryForObject("SELECT affected_users_snapshot FROM t_authorization_history WHERE role_id=? AND permission_id=? ORDER BY id DESC LIMIT 1",String.class,roleId,permission);
    assertTrue(snapshot.contains("\"id\":2"));assertFalse(snapshot.contains("\"id\":3"));

    int currentVersion2=jdbcTemplate.queryForObject("SELECT authorization_version FROM t_user WHERE id=2",Integer.class);
    mockMvc.perform(put("/api/users/2/authorization/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"authorizationVersion\":"+currentVersion2+",\"roleIds\":["+baseRole+"],\"reason\":\"离开事件成员\"}"))
      .andExpect(status().isOk());
    long auth3=jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=3",Long.class);
    int version3=jdbcTemplate.queryForObject("SELECT authorization_version FROM t_user WHERE id=3",Integer.class);
    List<Integer> role3=jdbcTemplate.queryForList("SELECT role_id FROM t_user_role WHERE user_id=3 AND active_marker=1",Integer.class);
    String roleIds=role3.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    mockMvc.perform(put("/api/users/3/authorization/roles").header(HttpHeaders.AUTHORIZATION,token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"authorizationVersion\":"+version3+",\"roleIds\":["+roleIds+","+roleId+"],\"reason\":\"矩阵后加入\"}"))
      .andExpect(status().isOk());
    assertEquals(auth3+1,jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=3",Long.class));
    String stable=jdbcTemplate.queryForObject("SELECT affected_users_snapshot FROM t_authorization_history WHERE role_id=? AND permission_id=? ORDER BY id DESC LIMIT 1",String.class,roleId,permission);
    assertEquals(snapshot,stable);assertTrue(stable.contains("\"id\":2"));assertFalse(stable.contains("\"id\":3"));
    mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION,token).param("actionCode","ROLE_PERMISSION_GRANTED"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
      .andExpect(jsonPath("$.data.list[0].target.name").value("组织架构-列表"));
    mockMvc.perform(get("/api/users/3/history").header(HttpHeaders.AUTHORIZATION,token).param("actionCode","ROLE_PERMISSION_GRANTED"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
  }

  private int insertOrganization(String prefix,int parent,String type){String code=prefix+System.nanoTime();jdbcTemplate.update("INSERT INTO t_organization_unit(code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?,?,?, ?,1,0,1,0,CURRENT_TIMESTAMP,1)",code,code,type,parent);return jdbcTemplate.queryForObject("SELECT id FROM t_organization_unit WHERE code=?",Integer.class,code);}
  private int insertPosition(String prefix){String code=prefix+System.nanoTime();jdbcTemplate.update("INSERT INTO t_position(code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?,?,10,0,1,0,CURRENT_TIMESTAMP,1)",code,code);return jdbcTemplate.queryForObject("SELECT id FROM t_position WHERE code=?",Integer.class,code);}
  private void moveEmployee(int employeeId,int org,int position){jdbcTemplate.update("UPDATE t_employee_assignment SET status='ENDED',active_primary_marker=NULL,effective_to=CURRENT_TIMESTAMP WHERE employee_id=? AND active_primary_marker=1",employeeId);jdbcTemplate.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP-INTERVAL '1' SECOND,'角色范围测试',0,CURRENT_TIMESTAMP,1)",employeeId,org,position);}
  private void grantRoleAccess(String role,String...permissions){for(String permission:permissions)jdbcTemplate.update("INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code) SELECT r.id,p.id,p.delegable,'REPORTING_TREE' FROM t_role r CROSS JOIN t_permission p WHERE r.role=? AND p.code=? AND NOT EXISTS(SELECT 1 FROM t_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id)",role,permission);}
  private JsonNode body(String value)throws Exception{return objectMapper.readTree(value);}
}
