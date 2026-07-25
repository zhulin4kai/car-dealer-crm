package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.dto.CreateAiProviderConfigRequest;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiProviderConfigMapper;
import com.autodealer.crm.modules.ai.application.internal.AiProviderConfigServiceImpl;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AiProviderConfigServiceImplTest {

    @Test
    void create_shouldRejectLoopbackProviderBeforeEncryptingKey() {
        TAiProviderConfigMapper mapper = mock(TAiProviderConfigMapper.class);
        AiProviderKeyCipher keyCipher = mock(AiProviderKeyCipher.class);
        AiProviderConfigServiceImpl service = new AiProviderConfigServiceImpl(
                mapper,
                keyCipher,
                mock(CurrentUserProvider.class),
                new AiSensitiveDataSanitizer(),
                new ObjectMapper());
        CreateAiProviderConfigRequest request = request("https://127.0.0.1:8091");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(keyCipher, never()).encrypt("not-a-real-key");
        verify(mapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_shouldRejectPlainHttpProvider() {
        AiProviderConfigServiceImpl service = new AiProviderConfigServiceImpl(
                mock(TAiProviderConfigMapper.class),
                mock(AiProviderKeyCipher.class),
                mock(CurrentUserProvider.class),
                new AiSensitiveDataSanitizer(),
                new ObjectMapper());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(request("http://provider.example")));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://localhost:8091",
            "https://[::1]:8091",
            "https://provider.internal/v1",
            "https://user@provider.example/v1",
            "https://provider.example/v1?target=internal",
            "https://provider.example/v1#fragment"
    })
    void create_shouldRejectUnsafeProviderUrlForms(String baseUrl) {
        AiProviderConfigServiceImpl service = new AiProviderConfigServiceImpl(
                mock(TAiProviderConfigMapper.class),
                mock(AiProviderKeyCipher.class),
                mock(CurrentUserProvider.class),
                new AiSensitiveDataSanitizer(),
                new ObjectMapper());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(request(baseUrl)));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
    }

    private CreateAiProviderConfigRequest request(String baseUrl) {
        CreateAiProviderConfigRequest request = new CreateAiProviderConfigRequest();
        request.setProviderName("边界测试");
        request.setProviderFormat("OPENAI_COMPATIBLE");
        request.setBaseUrl(baseUrl);
        request.setModelName("test-model");
        request.setModelDisplayName("Test Model");
        request.setApiKey("not-a-real-key");
        request.setTimeoutSeconds(5);
        request.setMaxOutputTokens(16);
        request.setTemperature(java.math.BigDecimal.ZERO);
        return request;
    }
}
