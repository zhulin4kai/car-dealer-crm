package com.autodealer.crm.shared.security.config;

import com.autodealer.crm.shared.security.AuthenticationConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关可调参数，通过 application.yml 的 {@code auth} 前缀配置。
 *
 * <p>仅在 {@code JwtSecretValidator} 和 {@code AuthenticationConstants} 中保留真正不变的常量。</p>
 */
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthenticationProperties {

    /** 普通登录 JWT 有效时长（秒），默认 4 小时 */
    private long defaultExpireSeconds = 4 * 60 * 60L;

    /** 记住我 JWT 有效时长（秒），默认 7 天 */
    private long rememberMeExpireSeconds = 7 * 24 * 60 * 60L;

    public long getDefaultExpireSeconds() {
        return defaultExpireSeconds;
    }

    public void setDefaultExpireSeconds(long defaultExpireSeconds) {
        this.defaultExpireSeconds = defaultExpireSeconds;
    }

    public long getRememberMeExpireSeconds() {
        return rememberMeExpireSeconds;
    }

    public void setRememberMeExpireSeconds(long rememberMeExpireSeconds) {
        this.rememberMeExpireSeconds = rememberMeExpireSeconds;
    }
}
