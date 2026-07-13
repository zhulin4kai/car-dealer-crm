package com.autodealer.crm.config.handler;

import com.autodealer.crm.audit.LoginAuditRecorder;
import com.autodealer.crm.result.R;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.LoginSecurityService;
import com.autodealer.crm.util.JSONUtils;
import com.autodealer.crm.util.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登录失败的处理器
 *
 */
@Component
@Slf4j
public class MyAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Resource
    private LoginAuditRecorder loginAuditRecorder;
    @Resource private LoginSecurityService loginSecurityService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String loginAct = request.getParameter("loginAct");
        log.warn("登录失败: loginAct={}, exception={}, reason={}",
                sanitizeForLog(loginAct),
                exception.getClass().getSimpleName(),
                exception.getMessage());

        try {
            loginAuditRecorder.recordFailure(loginAct, exception, request);
        } catch (RuntimeException auditException) {
            log.warn("登录失败审计写入失败 loginAct={}", sanitizeForLog(loginAct), auditException);
        }
        try { if (loginSecurityService != null && exception instanceof BadCredentialsException) loginSecurityService.recordFailure(loginAct); }
        catch (RuntimeException securityException) { log.error("登录失败计数写入失败 loginAct={}",sanitizeForLog(loginAct),securityException); }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(CodeEnum.AUTH_LOGIN_FAILED)));
    }

    private String sanitizeForLog(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
