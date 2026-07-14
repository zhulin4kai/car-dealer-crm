package com.autodealer.crm.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

/**
 * jwt 工具类
 *
 */
public class JWTUtils {

    private static final String SECRET = resolveSecret();

    private static String resolveSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET 环境变量未配置，应用无法签发或校验 JWT");
        }
        return secret.trim();
    }

    /**
     * 生成JWT （token）
     *
     */
    public static String createJWT(Integer userId, String loginAct, long expirationSeconds) {
        return createJWTBuilder(userId, loginAct, expirationSeconds)
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 生成携带认证安全版本的 JWT。新登录必须使用该入口。
     */
    public static String createJWT(Integer userId, String loginAct, Long authVersion, long expirationSeconds) {
        if (authVersion == null || authVersion < 0) {
            throw new IllegalArgumentException("认证安全版本不能为空或为负数");
        }
        return createJWTBuilder(userId, loginAct, expirationSeconds)
                .withClaim("authVersion", authVersion)
                .sign(Algorithm.HMAC256(SECRET));
    }

    /** Task17 新会话 JWT：只包含稳定用户ID、会话ID、安全版本、iat、exp。 */
    public static String createSessionJWT(Integer userId, String sessionId, Long authVersion,
                                          Instant issuedAt, Instant expiresAt) {
        if (userId == null || sessionId == null || sessionId.isBlank() || authVersion == null
                || authVersion < 0 || issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("会话 JWT 参数不合法");
        }
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256"); header.put("typ", "JWT");
        return JWT.create().withHeader(header).withClaim("userId", userId)
                .withClaim("sessionId", sessionId).withClaim("authVersion", authVersion)
                .withIssuedAt(Date.from(issuedAt)).withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.HMAC256(SECRET));
    }

    private static com.auth0.jwt.JWTCreator.Builder createJWTBuilder(
            Integer userId, String loginAct, long expirationSeconds) {
        //组装头数据
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        return JWT.create()
                .withHeader(header)
                .withClaim("userId", userId)
                .withClaim("loginAct", loginAct)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationSeconds * 1000));
    }

    /**
     * 验证 JWT
     *
     * @param jwt 要验证的 jwt 的字符串
     */
    public static Boolean verifyJWT(String jwt) {
        try {
            // 使用秘钥创建一个 JWT 验证器对象
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

            //验证JWT，如果没有抛出异常，说明验证通过，否则验证不通过
            jwtVerifier.verify(jwt);

            return true;
        } catch (Exception e) {
            // Invalid or expired tokens are expected auth failures; callers handle the false result.
        }
        return false;
    }

    /**
     * 从 JWT 字符串中解析用户 ID。
     *
     * @param jwt JWT 字符串
     * @return 用户 ID
     */
    public static Integer parseUserIdFromJWT(String jwt) {
        DecodedJWT decodedJWT = verifyAndDecode(jwt);
        return decodedJWT.getClaim("userId").asInt();
    }

    /**
     * 从 JWT 字符串中解析登录账号。
     *
     * @param jwt JWT 字符串
     * @return 登录账号
     */
    public static String parseLoginActFromJWT(String jwt) {
        DecodedJWT decodedJWT = verifyAndDecode(jwt);
        return decodedJWT.getClaim("loginAct").asString();
    }

    /**
     * 解析认证安全版本。兼容期内，旧 Token 没有该 claim 时返回 null。
     */
    public static Long parseAuthVersionFromJWT(String jwt) {
        DecodedJWT decodedJWT = verifyAndDecode(jwt);
        return decodedJWT.getClaim("authVersion").asLong();
    }

    public static String parseSessionIdFromJWT(String jwt) {
        return verifyAndDecode(jwt).getClaim("sessionId").asString();
    }

    public static Instant parseIssuedAtFromJWT(String jwt) {
        Date issuedAt = verifyAndDecode(jwt).getIssuedAt();
        return issuedAt == null ? null : issuedAt.toInstant();
    }

    /**
     * 验证 JWT 签名并将其解码为 DecodedJWT 对象。
     *
     * @param jwt JWT 字符串
     * @return 解码后的 DecodedJWT 对象
     */
    private static DecodedJWT verifyAndDecode(String jwt) {
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
        return jwtVerifier.verify(jwt);
    }
}
