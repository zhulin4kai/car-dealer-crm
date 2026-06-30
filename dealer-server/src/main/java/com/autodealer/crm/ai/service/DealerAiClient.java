package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.DealerAiEventResponse;
import com.autodealer.crm.ai.dto.DealerAiRunRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class DealerAiClient {
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int STREAM_READ_TIMEOUT_MS = 1_000;

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;

    public DealerAiClient(ObjectMapper objectMapper,
                          @Value("${ai.dealer-ai.base-url:http://localhost:8091}") String baseUrl,
                          @Value("${ai.dealer-ai.internal-token:dev-internal-token}") String internalToken) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.internalToken = internalToken;
    }

    public void streamRunEvents(DealerAiRunRequest request,
                                DealerAiEventConsumer consumer,
                                AiRunCancellationToken token) {
        HttpURLConnection connection = null;
        try {
            connection = openStreamConnection(request);
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new BusinessException(CodeEnum.AI_DEALER_AI_UNAVAILABLE, "AI 编排服务不可用");
            }
            readSse(connection, consumer, token);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            if (token.isCancelled()) {
                return;
            }
            throw new BusinessException(CodeEnum.AI_DEALER_AI_UNAVAILABLE, "AI 编排服务不可用", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openStreamConnection(DealerAiRunRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl + "/internal/runs/stream")
                .toURL()
                .openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(STREAM_READ_TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("X-Dealer-AI-Token", internalToken);
        byte[] body = objectMapper.writeValueAsBytes(request);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }
        return connection;
    }

    private void readSse(HttpURLConnection connection,
                         DealerAiEventConsumer consumer,
                         AiRunCancellationToken token) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder data = new StringBuilder();
            while (!token.isCancelled()) {
                String line;
                try {
                    line = reader.readLine();
                } catch (SocketTimeoutException ex) {
                    continue;
                }
                if (line == null) {
                    break;
                }
                if (line.isBlank()) {
                    emitData(data, consumer);
                    data.setLength(0);
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring(5).trim());
                }
            }
        }
    }

    private void emitData(StringBuilder data, DealerAiEventConsumer consumer) {
        if (data.isEmpty()) {
            return;
        }
        try {
            consumer.accept(objectMapper.readValue(data.toString(), DealerAiEventResponse.class));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(CodeEnum.AI_SSE_FAILED, "AI 事件解析失败", ex);
        }
    }

    private String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
