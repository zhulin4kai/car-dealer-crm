package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.shared.security.SecurityPaths;
import com.autodealer.crm.modules.ai.application.api.dto.ExecuteAiToolRequest;
import com.autodealer.crm.modules.ai.application.api.AiInternalToolService;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.ai.web.AiInternalToolController;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class DealerAiTokenPolicyTest {

    @Test
    void localEnvironment_shouldAllowDefaultTokens() {
        DealerAiTokenPolicy policy = new DealerAiTokenPolicy(
                "local",
                "dev-internal-token",
                "dev-internal-token");

        assertDoesNotThrow(policy::afterPropertiesSet);
    }

    @Test
    void nonLocalEnvironment_shouldRejectDefaultTokens() {
        DealerAiTokenPolicy policy = new DealerAiTokenPolicy(
                "prod",
                "dev-internal-token",
                "dev-internal-token");

        assertThrows(IllegalStateException.class, policy::afterPropertiesSet);
    }

    @Test
    void internalToolTokenMismatch_shouldReturnStableAccessDeniedCode() {
        AiInternalToolController controller = new AiInternalToolController(
                mock(AiInternalToolService.class),
                "expected-token");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.execute("search_customers", "wrong-token", new ExecuteAiToolRequest()));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
    }

    @Test
    void securityPaths_shouldTreatErrorDispatchAndInternalToolsAsPublic() {
        MockHttpServletRequest errorRequest = new MockHttpServletRequest("GET", "/error");
        MockHttpServletRequest asyncDispatch = new MockHttpServletRequest("GET", "/api/ai/runs/run/events");
        asyncDispatch.setDispatcherType(DispatcherType.ASYNC);
        MockHttpServletRequest errorDispatch = new MockHttpServletRequest("GET", "/api/ai/runs/run/events");
        errorDispatch.setDispatcherType(DispatcherType.ERROR);
        MockHttpServletRequest toolRequest = new MockHttpServletRequest(
                "POST", "/internal/ai/tools/get_inventory_alerts/execute");

        assertEquals(true, SecurityPaths.isPublicPath(errorRequest));
        assertTrue(SecurityPaths.isPublicPath(asyncDispatch));
        assertTrue(SecurityPaths.isPublicPath(errorDispatch));
        assertTrue(SecurityPaths.isPublicPath(toolRequest));
    }
}
