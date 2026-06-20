package com.autodealer.crm.config.handler;

import com.autodealer.crm.result.R;
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

    private static final String PUBLIC_FAILURE_MESSAGE = "账号或密码错误";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.warn("登录失败: loginAct={}, exception={}, reason={}",
                sanitizeForLog(request.getParameter("loginAct")),
                exception.getClass().getSimpleName(),
                exception.getMessage());

        R result = R.FAIL(PUBLIC_FAILURE_MESSAGE);
        ResponseUtils.write(response, JSONUtils.toJSON(result));
    }

    private String sanitizeForLog(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
