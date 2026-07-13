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
    public static final String CREDENTIAL_ACTIVATE = "/api/credentials/activate";
    public static final String CREDENTIAL_FORGOT = "/api/credentials/forgot-password";
    public static final String CREDENTIAL_RESET = "/api/credentials/reset-password";
    public static final String CREDENTIAL_VERIFY_CONTACT = "/api/credentials/verify-contact";
    public static final String BREAK_GLASS_REQUEST = "/api/recovery/break-glass/request";
    public static final String BREAK_GLASS_COMPLETE = "/api/recovery/break-glass/complete";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            LOGIN,
            ERROR,
            INTERNAL_AI_TOOLS,
            CREDENTIAL_ACTIVATE,
            CREDENTIAL_FORGOT,
            CREDENTIAL_RESET,
            CREDENTIAL_VERIFY_CONTACT,
            BREAK_GLASS_REQUEST,
            BREAK_GLASS_COMPLETE
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
