package com.autodealer.crm.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilsTest {

    @Test
    void testSecretReadsFromEnvironment() throws Exception {
        // JWTUtils reads SECRET from JWT_SECRET env variable.
        java.lang.reflect.Field secretField = JWTUtils.class.getDeclaredField("SECRET");
        secretField.setAccessible(true);
        String currentSecret = (String) secretField.get(null);
        
        // Verify SECRET is set (either from env or fallback)
        assertNotNull(currentSecret, "SECRET should not be null");
        assertFalse(currentSecret.isEmpty(), "SECRET should not be empty");
        
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null) {
            assertEquals(envSecret, currentSecret, "SECRET should match JWT_SECRET environment variable");
        }
    }

    @Test
    void testTokenHasExpiration() {
        String token = JWTUtils.createJWT(1, "testuser", 60);

        DecodedJWT decodedJWT = JWT.decode(token);
        Date expiresAt = decodedJWT.getExpiresAt();

        assertNotNull(expiresAt, "Token should have an expiration claim (expiresAt)");
        assertTrue(expiresAt.after(new Date()), "Token expiration should be in the future");
    }

    @Test
    void testGenerateAndParseToken() {
        String token = JWTUtils.createJWT(42, "alice", 60);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals(42, JWTUtils.parseUserIdFromJWT(token));
        assertEquals("alice", JWTUtils.parseLoginActFromJWT(token));
    }

    @Test
    void testParseInvalidToken() {
        Boolean result = JWTUtils.verifyJWT("this.is.not.a.valid.token");
        assertFalse(result, "Verifying an invalid token should return false");
    }
}
