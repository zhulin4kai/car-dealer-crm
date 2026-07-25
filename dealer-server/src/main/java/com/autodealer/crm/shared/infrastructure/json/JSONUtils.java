package com.autodealer.crm.shared.infrastructure.json;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JSONUtils {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    /**
     * 把 java 对象转成 json
     *
     * @param object
     */
    public static String toJSON(Object object) {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
     * 把 json 字符串转 java 对象
     *
     * @param json
     * @param clazz
     * @param <T>
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(json, clazz);
    }
}
