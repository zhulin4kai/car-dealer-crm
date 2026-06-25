package com.autodealer.crm.config.handler;

import com.autodealer.crm.result.CodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyAccessDeniedHandlerTest {

    private final MyAccessDeniedHandler handler = new MyAccessDeniedHandler();

    @Test
    void accessDeniedShouldReturnHttp403WithStableCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("forbidden"));

        String content = response.getContentAsString();
        assertEquals(403, response.getStatus());
        assertTrue(content.contains("\"code\":" + CodeEnum.ACCESS_DENIED.getCode()));
        assertTrue(response.getContentType().contains("application/json"));
    }
}
