package com.autodealer.crm.config.handler;

import com.autodealer.crm.audit.LoginAuditRecorder;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.R;
import com.autodealer.crm.util.JSONUtils;
import com.autodealer.crm.util.JWTUtils;
import com.autodealer.crm.util.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Resource
    private RedisManager redisManager;

    @Resource
    private LoginAuditRecorder loginAuditRecorder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 登录成功，执行该方法，在该方法中返回 json 给前端
        TUser tUser = (TUser) authentication.getPrincipal();

        // 3、设置 jwt 的过期时间(如果选择了记住我，过期时间是 7 天，否则是 30 分钟)
        String rememberMe = request.getParameter("rememberMe");
        long expirationSeconds = Boolean.parseBoolean(rememberMe) ? Constants.EXPIRE_TIME : Constants.DEFAULT_EXPIRE_TIME;

        String jwt = JWTUtils.createJWT(tUser.getId(), tUser.getLoginAct(), expirationSeconds);

        boolean stored;
        try {
            stored = redisManager.set(RedisKeys.userLogin(tUser.getId()), jwt, expirationSeconds);
        } catch (RuntimeException exception) {
            writeSystemFailure(response);
            return;
        }
        if (!stored) {
            writeSystemFailure(response);
            return;
        }

        try {
            loginAuditRecorder.recordSuccess(tUser, request);
        } catch (RuntimeException exception) {
            redisManager.delete(RedisKeys.userLogin(tUser.getId()));
            writeSystemFailure(response);
            return;
        }

        R result = R.OK(jwt);

        // 把 R 以 json 返回给前端
        ResponseUtils.write(response, JSONUtils.toJSON(result));
    }

    private void writeSystemFailure(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(CodeEnum.SYSTEM_ERROR)));
    }
}
