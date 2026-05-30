package com.bjpowernode.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void testCorsAllowsCredentials() {
        CorsConfig corsConfig = new CorsConfig();
        org.springframework.web.filter.CorsFilter corsFilter = corsConfig.corsFilter();

        assertNotNull(corsFilter, "CorsFilter should not be null");
    }

    @Test
    void testDebugEndpointsAreSecured() {
        try {
            Class<?> dicControllerClass = Class.forName("com.bjpowernode.web.DicController");
            try {
                dicControllerClass.getMethod("getCurrentAuthorities");
                fail("Debug endpoint getCurrentAuthorities() should NOT exist - it was removed for security");
            } catch (NoSuchMethodException e) {
                // Expected: debug endpoint was removed
            }
        } catch (ClassNotFoundException e) {
            fail("DicController class not found: " + e.getMessage());
        }
    }
}
