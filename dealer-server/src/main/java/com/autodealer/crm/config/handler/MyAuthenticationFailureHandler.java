package com.autodealer.crm.config.handler;

import com.autodealer.crm.result.R;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.util.JSONUtils;
import com.autodealer.crm.util.ResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
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

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.warn("登录失败: loginAct={}, exception={}, reason={}",
                sanitizeForLog(request.getParameter("loginAct")),
                exception.getClass().getSimpleName(),
                exception.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(CodeEnum.AUTH_LOGIN_FAILED)));
    }

    private String sanitizeForLog(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
