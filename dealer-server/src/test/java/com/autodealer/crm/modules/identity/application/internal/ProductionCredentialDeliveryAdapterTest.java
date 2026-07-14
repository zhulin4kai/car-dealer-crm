package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.fulfillment.delivery.application.api.enums.DeliveryStatus;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.enums.CredentialPurpose;
import com.autodealer.crm.modules.identity.application.api.CredentialDeliveryPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionCredentialDeliveryAdapterTest {

    @Test
    void reportsChannelNotConfiguredWithoutTrustedEndpointAndStrongToken() {
        ProductionCredentialDeliveryAdapter adapter = adapter();

        CredentialDeliveryPort.DeliveryStatus status = adapter.deliver(message("u@example.com"));

        assertEquals("CHANNEL_NOT_CONFIGURED", status.code());
    }

    @Test
    void rejectsPlainHttpOutsideExplicitLoopbackMode() {
        ProductionCredentialDeliveryAdapter adapter = adapter();
        ReflectionTestUtils.setField(adapter, "webhookUrl", "http://example.com/deliver");
        ReflectionTestUtils.setField(adapter, "bearerToken", "x".repeat(32));
        ReflectionTestUtils.setField(adapter, "allowInsecureLoopback", true);

        assertEquals("CHANNEL_NOT_CONFIGURED", adapter.deliver(message("u@example.com")).code());
    }

    @Test
    void sendsCredentialOnlyToConfiguredTrustedLoopbackReceiverForManualValidation() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(204);
        doReturn(response).when(client).send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any());
        ProductionCredentialDeliveryAdapter adapter = new ProductionCredentialDeliveryAdapter(objectMapper, client);
        String secret = "manual-validation-delivery-secret-0001";
        ReflectionTestUtils.setField(adapter, "webhookUrl", "http://127.0.0.1:18080/credential");
        ReflectionTestUtils.setField(adapter, "bearerToken", secret);
        ReflectionTestUtils.setField(adapter, "allowInsecureLoopback", true);

        CredentialDeliveryPort.DeliveryStatus status = adapter.deliver(message("u@example.com"));

        assertEquals("WEBHOOK_DELIVERED", status.code());
        org.mockito.ArgumentCaptor<HttpRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(client).send(requestCaptor.capture(), any());
        HttpRequest request = requestCaptor.getValue();
        JsonNode payload = objectMapper.readTree(readBody(request));
        assertEquals("http://127.0.0.1:18080/credential", request.uri().toString());
        assertEquals("Bearer " + secret, request.headers().firstValue("Authorization").orElseThrow());
        assertEquals("message-8", request.headers().firstValue("Idempotency-Key").orElseThrow());
        assertEquals("message-8", payload.path("messageId").asText());
        assertEquals("raw-one-time-credential", payload.path("credential").asText());
        assertEquals("INVITATION", payload.path("purpose").asText());
        assertEquals("u@example.com", payload.path("email").asText());
    }

    @Test
    void refusesToClaimDeliveryWithoutAnyContact() {
        ProductionCredentialDeliveryAdapter adapter = adapter();
        ReflectionTestUtils.setField(adapter, "webhookUrl", "https://notify.example.com/credential");
        ReflectionTestUtils.setField(adapter, "bearerToken", "x".repeat(32));

        assertEquals("NO_DELIVERY_CONTACT", adapter.deliver(message(null)).code());
    }

    private ProductionCredentialDeliveryAdapter adapter() {
        return new ProductionCredentialDeliveryAdapter(new ObjectMapper(), HttpClient.newHttpClient());
    }

    private CredentialDeliveryPort.DeliveryMessage message(String email) {
        return new CredentialDeliveryPort.DeliveryMessage(
                "message-8",
                8,
                "user8",
                null,
                email,
                CredentialPurpose.INVITATION,
                "raw-one-time-credential",
                LocalDateTime.now().plusHours(1));
    }

    private byte[] readBody(HttpRequest request) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompletableFuture<Void> completed = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            @Override public void onSubscribe(Flow.Subscription value) { subscription = value; subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer item) { byte[] bytes = new byte[item.remaining()]; item.get(bytes); output.writeBytes(bytes); }
            @Override public void onError(Throwable throwable) { completed.completeExceptionally(throwable); }
            @Override public void onComplete() { completed.complete(null); }
        });
        completed.get();
        return output.toByteArray();
    }
}
