package com.autodealer.crm.modules.identity.application.api.model;

import com.autodealer.crm.shared.infrastructure.json.JSONUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TUserTest {

    @Test
    void testPasswordNotSerializedToJson() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setLoginPwd("secretPassword123");
        user.setName("Admin User");
        user.setPhone("13800138000");
        user.setEmail("admin@example.com");
        user.setAccountNoExpired(1);
        user.setCredentialsNoExpired(1);
        user.setAccountNoLocked(1);
        user.setAccountEnabled(1);
        user.setCreateTime(new Date());

        String json = JSONUtils.toJSON(user);

        // Bug: loginPwd is NOT annotated with @JsonIgnore, so it WILL appear in JSON.
        // After fix: this assertion should PASS (loginPwd should NOT be in JSON).
        assertFalse(json.contains("secretPassword123"),
                "loginPwd field should NOT be serialized to JSON. Found password in: " + json);
        assertFalse(json.contains("\"loginPwd\""),
                "loginPwd field should NOT appear as a key in JSON. Found in: " + json);
    }
}
