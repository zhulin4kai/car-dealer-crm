package com.autodealer.crm.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    void corsConfigurationAllowsViteDevServerOn8081() throws Exception {
        CorsConfig corsConfig = new CorsConfig();
        Field allowedOrigins = CorsConfig.class.getDeclaredField("allowedOrigins");
        allowedOrigins.setAccessible(true);
        allowedOrigins.set(corsConfig, "http://localhost:8081, http://127.0.0.1:8081");

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/login");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:8081");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals("http://localhost:8081", configuration.checkOrigin("http://localhost:8081"));
    }
}
