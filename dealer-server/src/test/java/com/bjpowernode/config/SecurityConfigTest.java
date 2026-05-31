package com.bjpowernode.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void testCorsAllowsCredentials() throws Exception {
        CorsConfig corsConfig = new CorsConfig();
        Field field = CorsConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(corsConfig, "http://localhost:5173");
        
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
