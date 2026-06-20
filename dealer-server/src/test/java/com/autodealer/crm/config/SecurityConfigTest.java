package com.autodealer.crm.config;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.integration.BackendIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Real auth/authorization/logout tests driven by SecurityConfig + MyAuthenticationSuccessHandler
 * + TokenVerifyFilter + H2 seed data, NOT by @MockBean or @AutoConfigureMockMvc(addFilters = false).
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(510));
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
    @DisplayName("login with wrong password returns a non-200 code and no JWT-shaped token in data")
    void wrongPasswordReturnsLoginError() throws Exception {
        MvcResult result = mockMvc.perform(post(Constants.LOGIN_URI)
                        .param("loginAct", "admin")
                        .param("loginPwd", "wrong-password")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        int code = body.path("code").asInt();
        org.junit.jupiter.api.Assertions.assertNotEquals(200, code,
                "Wrong password must not return a successful login code");
        JsonNode dataNode = body.path("data");
        org.junit.jupiter.api.Assertions.assertTrue(dataNode.isNull() || dataNode.isMissingNode(),
                "Wrong password must not return a JWT in data, got: " + dataNode);
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
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
