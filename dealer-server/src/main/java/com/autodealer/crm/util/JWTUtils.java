package com.autodealer.crm.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        //组装头数据
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        return JWT.create()
                .withHeader(header)
                .withClaim("userId", userId)
                .withClaim("loginAct", loginAct)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                .sign(Algorithm.HMAC256(SECRET));
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
