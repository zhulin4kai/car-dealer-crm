package com.autodealer.crm.config.filter;

import com.autodealer.crm.config.security.SecurityPaths;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.util.JSONUtils;
import com.autodealer.crm.util.JWTUtils;
import com.autodealer.crm.util.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenVerifyFilter extends OncePerRequestFilter {

    @Resource
    private RedisManager redisManager;

    @Resource
    private UserService userService;

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

        Integer userId = JWTUtils.parseUserIdFromJWT(token);
        String redisToken = redisManager.get(RedisKeys.userLogin(userId));
        if (!StringUtils.hasText(redisToken)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_EXPIRED);
            return;
        }
        if (!token.equals(redisToken)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_NONE_MATCH);
            return;
        }

        TUser currentUser = userService.getLoginUserById(userId);
        if (!isUsable(currentUser)) {
            if (!revokeSession(response, userId)) {
                return;
            }
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isUsable(TUser user) {
        return user != null
                && user.isEnabled()
                && user.isAccountNonExpired()
                && user.isAccountNonLocked()
                && user.isCredentialsNonExpired();
    }

    private void writeAuthFailure(HttpServletResponse response, CodeEnum codeEnum) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(codeEnum)));
    }

    private boolean revokeSession(HttpServletResponse response, Integer userId) {
        try {
            if (redisManager.delete(RedisKeys.userLogin(userId))) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // The response below is intentionally generic; Redis details must not leak to clients.
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(CodeEnum.SYSTEM_ERROR)));
        return false;
    }
}
