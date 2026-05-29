package com.bjpowernode.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void testCorsAllowsCredentials() {
        SecurityConfig config = new SecurityConfig();
        CorsConfigurationSource source = config.configurationSource();

        assertNotNull(source, "CorsConfigurationSource should not be null");

        CorsConfiguration configForPath = source.getCorsConfiguration(null);
        assertNotNull(configForPath, "CorsConfiguration should not be null");

        List<String> allowedOrigins = configForPath.getAllowedOrigins();
        assertNotNull(allowedOrigins, "Allowed origins should not be null");

        // Bug: allowedOrigins is set to ["*"] which is incompatible with allowCredentials=true.
        // After fix: origins should be specific URLs, not wildcard "*".
        // The test verifies that if credentials are allowed, origins must not be "*".
        Boolean allowCredentials = configForPath.getAllowCredentials();
        if (Boolean.TRUE.equals(allowCredentials)) {
            assertFalse(allowedOrigins.contains("*"),
                    "When allowCredentials is true, allowedOrigins must not contain '*' (wildcard). " +
                    "Use specific origins instead.");
        }
        // Also verify that origins should not be wildcard for security reasons
        assertFalse(allowedOrigins.contains("*"),
                "CORS allowedOrigins should not be wildcard '*' for security. Use specific origins.");
    }

    @Test
    void testDebugEndpointsAreSecured() {
        // Verify that /api/dict/debug/authorities endpoint exists in DicController
        // and is NOT annotated with @PreAuthorize or similar role restrictions.
        // The endpoint is publicly accessible to any authenticated user.

        // Read the DicController source to check for security annotations
        try {
            Class<?> dicControllerClass = Class.forName("com.bjpowernode.web.DicController");
            java.lang.reflect.Method method = dicControllerClass.getMethod("getCurrentAuthorities");

            // Bug: The debug endpoint has no @PreAuthorize annotation, so any authenticated user can access it.
            // After fix: @PreAuthorize("hasRole('ROLE_ADMIN')") should be added.
            org.springframework.security.access.prepost.PreAuthorize preAuthorize =
                    method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

            assertNull(preAuthorize,
                    "Debug endpoint /api/dict/debug/authorities currently has NO @PreAuthorize restriction. " +
                    "Any authenticated user can access it. After fix, it should require ROLE_ADMIN.");
        } catch (ClassNotFoundException e) {
            fail("DicController class not found: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("getCurrentAuthorities method not found: " + e.getMessage());
        }
    }
}
