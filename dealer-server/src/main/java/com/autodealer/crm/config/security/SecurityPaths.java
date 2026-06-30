package com.autodealer.crm.config.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Set;

public class SecurityPaths {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String LOGIN = "/api/login";
    public static final String LOGIN_FREE = "/api/login/free";
    public static final String LOGOUT = "/api/logout";
    public static final String ERROR = "/error";
    public static final String INTERNAL_AI_TOOLS = "/internal/ai/tools/**";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            LOGIN,
            LOGIN_FREE,
            ERROR,
            INTERNAL_AI_TOOLS
    );

    private SecurityPaths() {

    }

    public static boolean isPublicPath(HttpServletRequest request) {
        if (DispatcherType.ASYNC.equals(request.getDispatcherType())) {
            return true;
        }
        if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        return PUBLIC_PATHS.stream().anyMatch(path -> PATH_MATCHER.match(path, request.getRequestURI()));
    }

    public static boolean isLogoutPath(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && PATH_MATCHER.match(LOGOUT, request.getRequestURI());
    }
}
