package com.autodealer.crm.integration;

import com.autodealer.crm.model.TUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PermissionSecurityIntegrationTest extends BackendIntegrationTestBase {

    @Test
    @DisplayName("财务可收款退款开票但不能普通编辑交易")
    void financePermissionBoundary() throws Exception {
        String token = createTokenForRole(801, "finance_test", "finance_specialist");

        assertAuthorized(post("/api/tran/payment").contentType(MediaType.APPLICATION_JSON).content("{}"), token);
        assertAuthorized(post("/api/tran/payment/99999/refund-requests")
                .contentType(MediaType.APPLICATION_JSON).content("{}"), token);
        assertAuthorized(post("/api/tran/invoice").contentType(MediaType.APPLICATION_JSON).content("{}"), token);
        assertForbidden(put("/api/tran/update").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}"), token);
    }

    @Test
    @DisplayName("库存专员可调整库存但不能编辑交易")
    void inventoryPermissionBoundary() throws Exception {
        String token = createTokenForRole(802, "inventory_test", "inventory_specialist");

        assertAuthorized(post("/api/productstock/restock")
                .contentType(MediaType.APPLICATION_JSON).content("{}"), token);
        assertForbidden(put("/api/tran/update").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}"), token);
    }

    @Test
    @DisplayName("销售顾问不能审批退款或分配用户角色")
    void salesConsultantSensitiveOperationsAreForbidden() throws Exception {
        String token = createTokenForRole(803, "sales_test", "sales_consultant");

        assertForbidden(put("/api/tran/approve/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":true,\"comment\":\"测试\"}"), token);
        assertForbidden(post("/api/tran/payment/1/refund-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"100.00\",\"refundType\":\"CUSTOMER_REFUND\",\"reason\":\"测试\"}"), token);
        assertForbidden(put("/api/user/2/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":2,\"roleIds\":[1]}"), token);
    }

    @Test
    @DisplayName("非管理员获得字典权限后可以访问字典接口")
    void nonAdminWithDictionaryPermissionCanAccessDictionary() throws Exception {
        String token = createTokenForRole(804, "dict_test", "finance_specialist");
        jdbcTemplate.update(
                "INSERT INTO t_role_permission (role_id, permission_id) " +
                "SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p " +
                "WHERE r.role = 'finance_specialist' AND p.code = 'dict:type:list'");

        mockMvc.perform(get("/api/dict/types")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private String createTokenForRole(int id, String loginAct, String role) {
        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, " +
                "credentials_no_expired, account_no_locked, account_enabled) " +
                "VALUES (?, ?, 'unused', ?, 1, 1, 1, 1)", id, loginAct, loginAct);
        jdbcTemplate.update(
                "INSERT INTO t_user_role (user_id, role_id) " +
                "SELECT ?, id FROM t_role WHERE role = ?", id, role);
        TUser user = new TUser();
        user.setId(id);
        user.setLoginAct(loginAct);
        return buildDirectToken(user);
    }

    private void assertForbidden(RequestBuilder request, String token) throws Exception {
        mockMvc.perform(withToken(request, token)).andExpect(status().isForbidden());
    }

    private void assertAuthorized(RequestBuilder request, String token) throws Exception {
        mockMvc.perform(withToken(request, token))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    private RequestBuilder withToken(RequestBuilder request, String token) {
        return servletContext -> {
            var builtRequest = request.buildRequest(servletContext);
            builtRequest.addHeader(HttpHeaders.AUTHORIZATION, token);
            return builtRequest;
        };
    }
}
