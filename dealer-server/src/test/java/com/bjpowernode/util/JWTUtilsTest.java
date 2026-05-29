package com.bjpowernode.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bjpowernode.model.TUser;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilsTest {

    @Test
    void testSecretIsNotHardcoded() {
        String oldHardcodedSecret = "dY8300olWQ3345;1d<3w48";
        assertNotEquals(oldHardcodedSecret, JWTUtils.SECRET,
                "SECRET should not be the old hardcoded value 'dY8300olWQ3345;1d<3w48'");
    }

    @Test
    void testTokenHasExpiration() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("testuser");
        user.setName("Test User");
        user.setPhone("13800138000");
        user.setEmail("test@example.com");

        String userJSON = JSONUtils.toJSON(user);
        String token = JWTUtils.createJWT(userJSON);

        DecodedJWT decodedJWT = JWT.decode(token);
        Date expiresAt = decodedJWT.getExpiresAt();

        assertNotNull(expiresAt, "Token should have an expiration claim (expiresAt)");
        assertTrue(expiresAt.after(new Date()), "Token expiration should be in the future");
    }

    @Test
    void testGenerateAndParseToken() {
        TUser user = new TUser();
        user.setId(42);
        user.setLoginAct("alice");
        user.setName("Alice Wang");
        user.setPhone("13900139000");
        user.setEmail("alice@example.com");

        String userJSON = JSONUtils.toJSON(user);
        String token = JWTUtils.createJWT(userJSON);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        TUser parsed = JWTUtils.parseUserFromJWT(token);
        assertNotNull(parsed);
        assertEquals(42, parsed.getId());
        assertEquals("alice", parsed.getLoginAct());
        assertEquals("Alice Wang", parsed.getName());
        assertEquals("13900139000", parsed.getPhone());
        assertEquals("alice@example.com", parsed.getEmail());
    }

    @Test
    void testParseInvalidToken() {
        Boolean result = JWTUtils.verifyJWT("this.is.not.a.valid.token");
        assertFalse(result, "Verifying an invalid token should return false");
    }
}
