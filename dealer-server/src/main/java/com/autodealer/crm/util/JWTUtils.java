package com.autodealer.crm.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.autodealer.crm.model.TUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * jwt工具类
 *
 */
public class JWTUtils {

    private static final String SECRET = resolveSecret();

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

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
    public static String createJWT(String userJSON) {
        //组装头数据
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        return JWT.create()
                //头部
                .withHeader(header)

                //负载
                .withClaim("user", userJSON)

                //过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_MS))

                //签名
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 验证JWT
     *
     * @param jwt 要验证的jwt的字符串
     */
    public static Boolean verifyJWT(String jwt) {
        try {
            // 使用秘钥创建一个JWT验证器对象
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

            //验证JWT，如果没有抛出异常，说明验证通过，否则验证不通过
            jwtVerifier.verify(jwt);

            return true;
        } catch (Exception e) {
            // Invalid or expired tokens are expected auth failures; callers handle the false result.
        }
        return false;
    }

    public static TUser parseUserFromJWT(String jwt) {
        try {
            // 使用秘钥创建一个验证器对象
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

            //验证JWT，得到一个解码后的jwt对象
            DecodedJWT decodedJWT = jwtVerifier.verify(jwt);

            //通过解码后的jwt对象，就可以获取里面的负载数据
            Claim userClaim = decodedJWT.getClaim("user");

            String userJSON = userClaim.asString();

            return JSONUtils.toBean(userJSON, TUser.class);
        } catch (TokenExpiredException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
