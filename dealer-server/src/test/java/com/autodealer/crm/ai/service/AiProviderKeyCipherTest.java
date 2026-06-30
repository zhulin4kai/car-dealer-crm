package com.autodealer.crm.ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderKeyCipherTest {
    @TempDir
    Path tempDir;

    @Test
    void localEnvironmentWithoutSecret_shouldGenerateReusableLocalKeyFile() throws Exception {
        Path keyFile = tempDir.resolve("ai-provider-key.secret");
        AiProviderKeyCipher firstCipher = new AiProviderKeyCipher("local", "", keyFile.toString());
        firstCipher.afterPropertiesSet();

        AiProviderKeyCipher.EncryptedApiKey encrypted = firstCipher.encrypt("sk-local-test-key");

        assertTrue(Files.exists(keyFile));
        assertEquals("sk-local-test-key", firstCipher.decrypt(encrypted.encryptedApiKey(), encrypted.apiKeyNonce()));

        AiProviderKeyCipher secondCipher = new AiProviderKeyCipher("local", "", keyFile.toString());
        secondCipher.afterPropertiesSet();

        assertEquals("sk-local-test-key", secondCipher.decrypt(encrypted.encryptedApiKey(), encrypted.apiKeyNonce()));
    }

    @Test
    void nonLocalEnvironmentWithoutSecret_shouldFailFast() {
        AiProviderKeyCipher cipher = new AiProviderKeyCipher("prod", "", tempDir.resolve("unused.secret").toString());

        IllegalStateException ex = assertThrows(IllegalStateException.class, cipher::afterPropertiesSet);

        assertTrue(ex.getMessage().contains("AI_PROVIDER_KEY_ENCRYPTION_SECRET"));
    }
}
