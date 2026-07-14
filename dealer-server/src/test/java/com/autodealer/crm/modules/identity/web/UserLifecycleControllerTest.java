package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.modules.identity.application.api.dto.user.UserLifecycleDtos.Context;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserLifecycleDtos.RehireRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserLifecycleDtos.RehireResult;
import com.autodealer.crm.modules.identity.application.api.UserLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserLifecycleControllerTest {

    private UserLifecycleService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserLifecycleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserLifecycleController(service)).build();
    }

    @Test
    void rehireReturnsOkWhenServiceResultNeedsNoCredentialDelivery() throws Exception {
        when(service.rehire(eq(21), any(RehireRequest.class)))
                .thenReturn(rehireResult("NOT_REQUIRED"));

        mockMvc.perform(post("/api/users/21/lifecycle/rehire")
                        .contentType(APPLICATION_JSON)
                        .content(rehireRequest("INVITE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.restoredLegacyAuthorizationCount").value(0))
                .andExpect(jsonPath("$.data.credentialDeliveryStatus").value("NOT_REQUIRED"));
    }

    @Test
    void rehireReturnsAcceptedWhenServiceResultHasQueuedCredentialDelivery() throws Exception {
        when(service.rehire(eq(21), any(RehireRequest.class)))
                .thenReturn(rehireResult("QUEUED"));

        mockMvc.perform(post("/api/users/21/lifecycle/rehire")
                        .contentType(APPLICATION_JSON)
                        .content(rehireRequest("RECOVER")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.restoredLegacyAuthorizationCount").value(0))
                .andExpect(jsonPath("$.data.credentialDeliveryStatus").value("QUEUED"));
    }

    private RehireResult rehireResult(String deliveryStatus) {
        Context context = new Context();
        context.setUserId(21);
        context.setEmployeeId(31);
        context.setEmploymentStatus("ACTIVE");
        context.setEmployeeVersion(6);
        RehireResult result = new RehireResult();
        result.setContext(context);
        result.setRestoredLegacyAuthorizationCount(0);
        result.setCredentialDeliveryStatus(deliveryStatus);
        return result;
    }

    private String rehireRequest(String activationMode) {
        return """
                {
                  "employeeVersion": 5,
                  "organizationUnitId": 2,
                  "positionId": 3,
                  "managerEmployeeId": 4,
                  "effectiveFrom": "2026-08-01T01:00:00+08:00",
                  "reason": "重新入职",
                  "accountActivationMode": "%s"
                }
                """.formatted(activationMode);
    }
}
