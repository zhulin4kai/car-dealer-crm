package com.autodealer.crm.bootstrap.security;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.SecurityFailureAuditService;
import com.autodealer.crm.shared.security.SecurityPaths;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.UserService;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import com.autodealer.crm.modules.identity.application.api.security.SessionAuthenticationDetails;
import com.autodealer.crm.modules.identity.application.api.security.UserManagementAccessGate;
import com.autodealer.crm.shared.infrastructure.json.JSONUtils;
import com.autodealer.crm.shared.security.JWTUtils;
import com.autodealer.crm.shared.web.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;

@Component
public class TokenVerifyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenVerifyFilter.class);
    private static final int SESSION_CLEANUP_RETRY_TIMES = 2;

    @Resource
    private RedisManager redisManager;

    @Resource
    private UserService userService;
    @Resource private UserSessionService userSessionService;
    @Resource private UserManagementAccessGate userManagementAccessGate;
    @Resource private SecurityFailureAuditService securityFailureAuditService;
    @Value("${security.session.legacy-accept-until:${SESSION_LEGACY_ACCEPT_UNTIL:}}")
    private String legacyAcceptUntil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityPaths.isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_EMPTY);
            return;
        }
        if (!authorization.startsWith("Bearer ")) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }

        String token = authorization.substring(7);
        if (!StringUtils.hasText(token)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_EMPTY);
            return;
        }
        if (!JWTUtils.verifyJWT(token)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }

        Integer userId;
        Long tokenAuthVersion;
        String sessionId;
        try {
            userId = JWTUtils.parseUserIdFromJWT(token);
            tokenAuthVersion = JWTUtils.parseAuthVersionFromJWT(token);
            sessionId = JWTUtils.parseSessionIdFromJWT(token);
        } catch (RuntimeException exception) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }
        if (userId == null) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }
        boolean legacy=!StringUtils.hasText(sessionId);
        if (legacy) {
            if (!legacyAllowed()) { writeAuthFailure(response,CodeEnum.TOKEN_IS_EXPIRED); return; }
            String redisToken=redisManager.get(RedisKeys.userLogin(userId));
            if (!StringUtils.hasText(redisToken)) { writeAuthFailure(response,CodeEnum.TOKEN_IS_EXPIRED); return; }
            if (!token.equals(redisToken)) { writeAuthFailure(response,CodeEnum.TOKEN_IS_NONE_MATCH); return; }
        } else {
            try {
                if (!userSessionService.validateAndTouch(token,userId,sessionId,tokenAuthVersion)) {
                    writeAuthFailure(response,CodeEnum.TOKEN_IS_EXPIRED); return;
                }
            } catch (RuntimeException exception) {
                log.error("会话基础设施校验失败 userId={}",userId,exception);
                writeAuthFailure(response,CodeEnum.TOKEN_IS_EXPIRED); return;
            }
        }

        TUser currentUser = userService.getLoginUserById(userId);
        if (!isUsable(currentUser)) {
            clearSessionWithRetry(userId,sessionId,legacy);
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }
        if (!matchesAuthVersion(tokenAuthVersion, currentUser.getAuthVersion())) {
            clearSessionWithRetry(userId,sessionId,legacy);
            writeAuthFailure(response, CodeEnum.TOKEN_IS_EXPIRED);
            return;
        }
        UserManagementAccessGate.Decision gate = userManagementAccessGate.evaluate(currentUser, request);
        if (!gate.allowed()) {
            securityFailureAuditService.recordAuthenticated(AuditActionEnum.USER_MANAGEMENT_GATE_REJECTED,
                    String.valueOf(currentUser.getId()),"{\"code\":"+gate.code().getCode()+"}",currentUser);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            ResponseUtils.write(response, JSONUtils.toJSON(Result.FAIL(gate.code().getCode(), gate.message())));
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        authentication.setDetails(new SessionAuthenticationDetails(sessionId,legacy));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        if (Boolean.TRUE.equals(currentUser.getMustChangePassword()) && !isFirstChangeAllowed(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            ResponseUtils.write(response, JSONUtils.toJSON(Result.FAIL(CodeEnum.ACCESS_DENIED.getCode(), "首次改密完成前不能访问业务接口")));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isFirstChangeAllowed(HttpServletRequest request) {
        String path=request.getRequestURI();
        return path.equals("/api/credentials/first-password-change")
                || path.equals("/api/login/info") || path.startsWith("/api/me/sessions")
                || SecurityPaths.isLogoutPath(request);
    }

    private boolean legacyAllowed() {
        if (!StringUtils.hasText(legacyAcceptUntil)) return false;
        try { return Instant.now().isBefore(Instant.parse(legacyAcceptUntil.trim())); }
        catch (RuntimeException exception) { log.error("旧会话兼容截止时间配置无效"); return false; }
    }

    private boolean isUsable(TUser user) {
        return user != null
                && user.isEnabled()
                && user.isAccountNonExpired()
                && user.isAccountNonLocked()
                && user.isCredentialsNonExpired();
    }

    private boolean matchesAuthVersion(Long tokenAuthVersion, Long databaseAuthVersion) {
        long currentVersion = databaseAuthVersion == null ? 0L : databaseAuthVersion;
        if (tokenAuthVersion == null) {
            return currentVersion == 0L;
        }
        return tokenAuthVersion == currentVersion;
    }

    private void writeAuthFailure(HttpServletResponse response, CodeEnum codeEnum) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ResponseUtils.write(response, JSONUtils.toJSON(Result.FAIL(codeEnum)));
    }

    private void clearSessionWithRetry(Integer userId,String sessionId,boolean legacy) {
        String key=legacy?RedisKeys.userLogin(userId):RedisKeys.userSession(sessionId);
        for (int attempt = 1; attempt <= SESSION_CLEANUP_RETRY_TIMES; attempt++) {
            try {
                if (redisManager.delete(key)) {
                    return;
                }
                log.warn("event=auth_session_cleanup result=not_deleted userId={} attempt={}", userId, attempt);
            } catch (RuntimeException exception) {
                log.warn("event=auth_session_cleanup result=failed userId={} attempt={}",
                        userId, attempt, exception);
            }
        }
        log.error("event=auth_session_cleanup result=retry_exhausted userId={} attempts={}",
                userId, SESSION_CLEANUP_RETRY_TIMES);
    }
}
