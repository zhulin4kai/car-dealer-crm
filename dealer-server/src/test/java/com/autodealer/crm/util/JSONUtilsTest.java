package com.autodealer.crm.util;

import com.autodealer.crm.model.TUser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JSONUtilsTest {

    @Test
    void testToJSONWithObject() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setName("Admin");

        String json = JSONUtils.toJSON(user);

        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"loginAct\":\"admin\""));
        assertTrue(json.contains("\"name\":\"Admin\""));
    }

    @Test
    void testToJSONWithMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);

        String json = JSONUtils.toJSON(map);

        assertNotNull(json);
        assertTrue(json.contains("\"key1\":\"value1\""));
        assertTrue(json.contains("\"key2\":123"));
    }

    @Test
    void testToBeanWithValidJson() {
        String json = "{\"id\":1,\"loginAct\":\"admin\",\"name\":\"Admin\"}";

        TUser user = JSONUtils.toBean(json, TUser.class);

        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("admin", user.getLoginAct());
        assertEquals("Admin", user.getName());
    }

    @Test
    void testToJSONAndToBeanRoundTrip() {
        TUser original = new TUser();
        original.setId(42);
        original.setLoginAct("testuser");
        original.setName("Test User");
        original.setPhone("13800138000");

        String json = JSONUtils.toJSON(original);
        TUser restored = JSONUtils.toBean(json, TUser.class);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getLoginAct(), restored.getLoginAct());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getPhone(), restored.getPhone());
    }

    @Test
    void testToJSONWithNull() {
        String json = JSONUtils.toJSON(null);

        assertEquals("null", json);
    }

    @Test
    void testToBeanWithInvalidJson() {
        assertThrows(RuntimeException.class, () -> {
            JSONUtils.toBean("invalid json", TUser.class);
        });
    }
}
