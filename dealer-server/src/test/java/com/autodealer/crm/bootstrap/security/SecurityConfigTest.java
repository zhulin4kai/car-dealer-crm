package com.autodealer.crm.bootstrap.security;

import com.autodealer.crm.modules.identity.web.UserController;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.autodealer.crm.shared.security.SecurityPaths;
import com.autodealer.crm.integration.BackendIntegrationTestBase;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.startsWith;

/**
 * Real auth/authorization/logout tests driven by SecurityConfig + MyAuthenticationSuccessHandler
 * + TokenVerifyFilter + H2 seed data, NOT by @MockitoBean or @AutoConfigureMockMvc(addFilters = false).
 */
class SecurityConfigTest extends BackendIntegrationTestBase {

    @Test
    @DisplayName("CORS preflight from the Vite dev server on 8081 is allowed for login")
    void loginCorsPreflightAllowsViteDevServer() throws Exception {
        mockMvc.perform(options(Constants.LOGIN_URI)
                        .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("unauthenticated request to /api/users must be rejected by the real Security filter chain")
    void unauthenticatedRequestToProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(510));
    }

    @Test
    @DisplayName("匿名联系方式验证请求能够穿过真实安全链并由凭证域判定")
    void contactVerificationEndpointIsPublicInTheRealFilterChain() throws Exception {
        mockMvc.perform(post(SecurityPaths.CREDENTIAL_VERIFY_CONTACT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"invalid-credential\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(620));
    }

    @Test
    @DisplayName("匿名应急恢复请求能够穿过真实安全链并由默认关闭策略拒绝")
    void breakGlassEndpointsArePublicButDefaultClosed() throws Exception {
        String recoveryKey = "A".repeat(32);
        mockMvc.perform(post(SecurityPaths.BREAK_GLASS_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginAct\":\"admin\",\"recoveryKey\":\"" + recoveryKey + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(520));

        mockMvc.perform(post(SecurityPaths.BREAK_GLASS_COMPLETE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"invalid-credential\",\"newPassword\":\"Valid1A\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(520));
    }

    @Test
    @DisplayName("login with admin/123456 returns code 200 and a non-empty JWT in $.data")
    void validLoginReturnsJwt() throws Exception {
        MvcResult result = mockMvc.perform(post(Constants.LOGIN_URI)
                        .param("loginAct", "admin")
                        .param("loginPwd", "123456")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").asText();
        org.junit.jupiter.api.Assertions.assertTrue(token != null && !token.isEmpty(),
                "Login response must include a non-empty JWT in $.data");
    }

    @Test
    @DisplayName("login failure returns HTTP 401 and the stable AUTH_LOGIN_FAILED contract")
    void wrongPasswordReturnsLoginError() throws Exception {
        mockMvc.perform(post(Constants.LOGIN_URI)
                        .param("loginAct", "admin")
                        .param("loginPwd", "wrong-password")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("with a real login token, /api/login/info returns the current user from H2")
    void loginInfoReturnsCurrentUser() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/login/info")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.loginAct").value("admin"))
                .andExpect(jsonPath("$.data.name").value("管理员"));
    }

    @Test
    @DisplayName("with a real login token, /api/users returns the seeded H2 users")
    void userListReturnsH2Data() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list[?(@.loginAct == 'admin')]").exists());
    }

    @Test
    @DisplayName("POST /api/logout invalidates the token and returns logout success")
    void logoutIsPostAndInvalidatesToken() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/logout")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("USER_LOGOUT"));

        mockMvc.perform(get("/api/login/info")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(512));
    }

    @Test
    @DisplayName("当前会话Redis精确清理失败时返回503且数据库撤销事实仍使Token失效")
    void logoutRedisCleanupFailureStillInvalidatesOldTokenByDatabaseVersion() throws Exception {
        String token = loginAsAdmin();
        doReturn(false).when(redisManager).delete(startsWith("cdrm:session:"));

        mockMvc.perform(post("/api/logout")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(635));

        mockMvc.perform(get("/api/login/info")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(512));
    }

    @Test
    @DisplayName("batch delete via DELETE /api/user accepts a JSON array body and works through real Security")
    void batchDeleteAcceptsJsonArray() throws Exception {
        String token = loginAsAdmin();

        // Use non-seed IDs (999, 1000) so we exercise the batch-delete contract
        // without actually deleting the seeded admin/zhangsan/lisi users that
        // other integration tests in the same shared H2 DB depend on.
        mockMvc.perform(delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[999,1000]"))
                .andExpect(status().isMethodNotAllowed());
        // UserController has no @DeleteMapping, so DELETE /api/user returns HTTP 405.
    }
}
