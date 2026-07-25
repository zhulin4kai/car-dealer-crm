package com.autodealer.crm.shared.infrastructure.json;

import com.autodealer.crm.modules.fulfillment.transaction.application.api.dto.UpdateTranRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(JacksonConfig.class);

    @Test
    void localDateTimeShouldAcceptProjectDateTimeFormat() throws Exception {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            UpdateTranRequest request = objectMapper.readValue(
                    "{\"id\":1,\"expectedDeliveryDate\":\"2026-07-12 00:00:00\"}",
                    UpdateTranRequest.class);

            assertEquals(LocalDateTime.of(2026, 7, 12, 0, 0), request.getExpectedDeliveryDate());
        });
    }

    @Test
    void localDateTimeShouldAcceptIsoDateTimeFormat() throws Exception {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            UpdateTranRequest request = objectMapper.readValue(
                    "{\"id\":1,\"expectedDeliveryDate\":\"2026-07-12T08:30:15\"}",
                    UpdateTranRequest.class);

            assertEquals(LocalDateTime.of(2026, 7, 12, 8, 30, 15), request.getExpectedDeliveryDate());
        });
    }

    @Test
    void localDateTimeShouldAcceptIsoOffsetDateTimeFormat() throws Exception {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            UpdateTranRequest request = objectMapper.readValue(
                    "{\"id\":1,\"expectedDeliveryDate\":\"2026-07-12T08:30:15+08:00\"}",
                    UpdateTranRequest.class);

            assertEquals(LocalDateTime.of(2026, 7, 12, 8, 30, 15), request.getExpectedDeliveryDate());
        });
    }

    @Test
    void localDateTimeShouldAcceptDateOnlyAsStartOfDay() throws Exception {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            UpdateTranRequest request = objectMapper.readValue(
                    "{\"id\":1,\"expectedDeliveryDate\":\"2026-07-12\"}",
                    UpdateTranRequest.class);

            assertEquals(LocalDateTime.of(2026, 7, 12, 0, 0), request.getExpectedDeliveryDate());
        });
    }

    @Test
    void localDateTimeShouldSerializeProjectDateTimeFormat() throws Exception {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            UpdateTranRequest request = new UpdateTranRequest();
            request.setId(1);
            request.setExpectedDeliveryDate(LocalDateTime.of(2026, 7, 12, 0, 0));

            String json = objectMapper.writeValueAsString(request);

            assertEquals("{\"customerId\":null,\"description\":null,"
                    + "\"expectedDeliveryDate\":\"2026-07-12 00:00:00\","
                    + "\"id\":1,\"products\":null}", json);
        });
    }
}
