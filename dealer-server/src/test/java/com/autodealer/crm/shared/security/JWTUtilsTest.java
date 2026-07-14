package com.autodealer.crm.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
        String token = JWTUtils.createJWT(42, "alice", 7L, 60);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals(42, JWTUtils.parseUserIdFromJWT(token));
        assertEquals("alice", JWTUtils.parseLoginActFromJWT(token));
        assertEquals(7L, JWTUtils.parseAuthVersionFromJWT(token));
    }

    @Test
    void legacyTokenWithoutAuthVersionShouldParseNullClaim() {
        String token = JWTUtils.createJWT(42, "alice", 60);

        assertNull(JWTUtils.parseAuthVersionFromJWT(token));
    }

    @Test
    void sessionTokenContainsOnlyStableSessionClaims() {
        Instant issuedAt = Instant.now().minusSeconds(1);
        Instant expiresAt = issuedAt.plusSeconds(900);

        String token = JWTUtils.createSessionJWT(42, "session-opaque-id", 7L, issuedAt, expiresAt);
        DecodedJWT decoded = JWT.decode(token);

        assertEquals(42, JWTUtils.parseUserIdFromJWT(token));
        assertEquals("session-opaque-id", JWTUtils.parseSessionIdFromJWT(token));
        assertEquals(7L, JWTUtils.parseAuthVersionFromJWT(token));
        assertEquals(issuedAt.getEpochSecond(), JWTUtils.parseIssuedAtFromJWT(token).getEpochSecond());
        assertEquals(expiresAt.getEpochSecond(), decoded.getExpiresAt().toInstant().getEpochSecond());
        assertTrue(decoded.getClaim("loginAct").isMissing(), "会话 JWT 不应携带可变登录账号");
        assertEquals(5, decoded.getClaims().size(), "会话 JWT 只能包含 userId/sessionId/authVersion/iat/exp");
    }

    @Test
    void testParseInvalidToken() {
        Boolean result = JWTUtils.verifyJWT("this.is.not.a.valid.token");
        assertFalse(result, "Verifying an invalid token should return false");
    }
}
