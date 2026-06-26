package com.autodealer.crm.integration;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.util.JWTUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for backend integration tests using real H2 database.
 *
 * Provides:
 * - Real Spring Boot context with H2 datasource (test profile)
 * - Real Mapper, Service, Controller, Security, JWT layer
 * - Faked RedisManager so token can be persisted in-memory during the test
 *
 * Login response contract (per MyAuthenticationSuccessHandler):
 *   { "code": 200, "msg": "操作成功", "data": "<jwt-token-string>" }
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BackendIntegrationTestBase {

    private static final String ADMIN_LOGIN_ACT = "admin";
    private static final String ADMIN_LOGIN_PWD = "123456";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    protected RedisManager redisManager;

    private final Map<String, String> tokenStore = new HashMap<>();

    @BeforeEach
    void resetTokenStore() {
        tokenStore.clear();
        // When MyAuthenticationSuccessHandler writes the JWT to Redis,
        // the TokenVerifyFilter later reads it back. Mirror that flow in-memory.
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            tokenStore.put(key, String.valueOf(value));
            return Boolean.TRUE;
        }).when(redisManager).set(anyString(), anyString(), anyLong());

        lenient().when(redisManager.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return tokenStore.get(key);
        });

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            tokenStore.remove(key);
            return Boolean.TRUE;
        }).when(redisManager).delete(anyString());
    }

    /**
     * Performs a real login against the SecurityConfig + MyAuthenticationSuccessHandler
     * + H2 user table, and returns the JWT string persisted in the fake Redis store.
     */
    protected String loginAsAdmin() throws Exception {
        return loginAs(ADMIN_LOGIN_ACT, ADMIN_LOGIN_PWD, 1);
    }

    protected String loginAs(String loginAct, String loginPwd, int expectedUserId) throws Exception {
        MvcResult result = mockMvc.perform(post(Constants.LOGIN_URI)
                        .param("loginAct", loginAct)
                        .param("loginPwd", loginPwd)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").asText();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Login response did not include a JWT in $.data: "
                    + result.getResponse().getContentAsString());
        }
        // Make sure the token is in our fake Redis so TokenVerifyFilter can find it.
        tokenStore.put(Constants.REDIS_JWT_KEY + expectedUserId, token);
        return "Bearer " + token;
    }

    /**
     * Builds a valid bearer token for an arbitrary user without going through /api/login.
     * Useful for permission tests where we need a logged-in user that does NOT have a
     * certain authority.
     */
    protected String buildDirectToken(TUser user) {
        String token = JWTUtils.createJWT(user.getId(), user.getLoginAct(), Constants.DEFAULT_EXPIRE_TIME);
        tokenStore.put(Constants.REDIS_JWT_KEY + user.getId(), token);
        return "Bearer " + token;
    }
}
