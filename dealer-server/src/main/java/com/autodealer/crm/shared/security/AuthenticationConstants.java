package com.autodealer.crm.shared.security;

import com.autodealer.crm.shared.security.config.AuthenticationProperties;
/**
 * 认证相关不可变常量。
 *
 * <p>JWT 会话 TTL 等可调参数已移入 {@code AuthenticationProperties}。</p>
 */
public final class AuthenticationConstants {

    public static final long REMEMBER_ME_EXPIRE_SECONDS = 604800L;
    public static final long DEFAULT_EXPIRE_SECONDS = 7200L;

    private AuthenticationConstants() {
    }
}
