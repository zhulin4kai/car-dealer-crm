package com.autodealer.crm.shared.infrastructure.json;

import com.autodealer.crm.shared.web.ApiDateTimeParser;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Jackson 3 自定义日期时间反序列化模块。
 *
 * <p>序列化由 Spring Boot 的 {@code spring.jackson.date-format} 和
 * {@code spring.jackson.json.write.write-dates-as-timestamps=false} 自动处理。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule flexibleDateTimeModule() {
        SimpleModule module = new SimpleModule("FlexibleDateTimeModule");
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(ApiDateTimeParser.PROJECT_DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new ValueDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) {
                String value = parser.getValueAsString();
                if (value == null || value.isBlank()) {
                    return null;
                }
                try {
                    return ApiDateTimeParser.parseLocalDateTime(value.trim());
                } catch (DateTimeParseException ex) {
                    return (LocalDateTime) context.handleWeirdStringValue(
                            LocalDateTime.class, value, ex.getMessage());
                }
            }
        });
        return module;
    }
}
