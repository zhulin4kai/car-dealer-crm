package com.autodealer.crm.config.filter;

import com.autodealer.crm.config.security.SecurityPaths;
import com.autodealer.crm.constant.Constants;
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

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_EMPTY);
            return;
        }
        if (!JWTUtils.verifyJWT(token)) {
            writeAuthFailure(response, CodeEnum.TOKEN_IS_ERROR);
            return;
        }

        Integer userId = JWTUtils.parseUserIdFromJWT(token);
        String redisToken = redisManager.get(Constants.REDIS_JWT_KEY + userId);
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
            redisManager.delete(Constants.REDIS_JWT_KEY + userId);
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
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(codeEnum)));
    }
}
