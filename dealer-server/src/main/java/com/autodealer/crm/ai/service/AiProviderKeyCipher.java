package com.autodealer.crm.ai.service;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Component
public class AiProviderKeyCipher implements InitializingBean {
    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final Set<String> LOCAL_ENVIRONMENTS = Set.of("local", "dev", "test", "smoke");

    private final String environment;
    private final String encodedSecret;
    private final String localSecretFile;
    private final SecureRandom secureRandom = new SecureRandom();
    private byte[] secretKey;

    public AiProviderKeyCipher(
            @Value("${ai.dealer-ai.environment:${DEALER_AI_ENV:${APP_ENV:local}}}") String environment,
            @Value("${ai.provider.key-encryption-secret:${AI_PROVIDER_KEY_ENCRYPTION_SECRET:}}")
            String encodedSecret,
            @Value("${ai.provider.local-key-file:"
                    + "${AI_PROVIDER_KEY_ENCRYPTION_SECRET_FILE:${user.home}/.car-dealer-crm/ai-provider-key.secret}}")
            String localSecretFile) {
        this.environment = environment;
        this.encodedSecret = encodedSecret;
        this.localSecretFile = localSecretFile;
    }

    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(encodedSecret)) {
            if (isLocalEnvironment(environment)) {
                secretKey = loadOrCreateLocalSecret();
                return;
            }
            throw new IllegalStateException("AI_PROVIDER_KEY_ENCRYPTION_SECRET must be configured outside local environment");
        }
        secretKey = decodeSecret(encodedSecret);
    }

    public EncryptedApiKey encrypt(String apiKey) {
        requireSecret();
        byte[] nonce = new byte[GCM_NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(secretKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            return new EncryptedApiKey(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(nonce));
        } catch (GeneralSecurityException ex) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_KEY_ENCRYPTION_FAILED, "AI 模型密钥加密失败", ex);
        }
    }

    public String decrypt(String encryptedApiKey, String apiKeyNonce) {
        requireSecret();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(secretKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(apiKeyNonce)));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(encryptedApiKey));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_KEY_DECRYPTION_FAILED, "AI 模型密钥解密失败", ex);
        }
    }

    private void requireSecret() {
        if (secretKey == null) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_KEY_ENCRYPTION_FAILED,
                    "AI Provider API Key 加密主密钥未配置");
        }
    }

    private byte[] loadOrCreateLocalSecret() {
        Path path = resolveLocalSecretPath();
        try {
            if (Files.exists(path)) {
                return decodeSecret(Files.readString(path).trim());
            }
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            byte[] key = new byte[AES_KEY_BYTES];
            secureRandom.nextBytes(key);
            String encoded = Base64.getEncoder().encodeToString(key);
            try {
                Files.writeString(path, encoded + System.lineSeparator(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ex) {
                return decodeSecret(Files.readString(path).trim());
            }
            restrictLocalSecretPermissions(path);
            return key;
        } catch (IOException ex) {
            throw new IllegalStateException("本地 AI Provider API Key 加密主密钥文件不可用: " + path, ex);
        }
    }

    private Path resolveLocalSecretPath() {
        if (StringUtils.hasText(localSecretFile)) {
            return Path.of(localSecretFile.trim());
        }
        return Path.of(System.getProperty("user.home"), ".car-dealer-crm", "ai-provider-key.secret");
    }

    private void restrictLocalSecretPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // 非 POSIX 文件系统无法设置权限时，仍依赖用户主目录隔离。
        }
    }

    private byte[] decodeSecret(String value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            if (decoded.length != AES_KEY_BYTES) {
                throw new IllegalArgumentException("invalid length");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("AI_PROVIDER_KEY_ENCRYPTION_SECRET must be Base64 encoded 32 bytes", ex);
        }
    }

    private boolean isLocalEnvironment(String value) {
        return value != null && LOCAL_ENVIRONMENTS.contains(value.trim().toLowerCase());
    }

    public record EncryptedApiKey(String encryptedApiKey, String apiKeyNonce) {
    }
}
