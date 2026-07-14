package com.autodealer.crm.bootstrap.security;

import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos;
import com.autodealer.crm.modules.audit.application.api.LoginAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.SessionAuthenticationDetails;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.shared.infrastructure.json.JSONUtils;
import com.autodealer.crm.shared.web.ResponseUtils;
import com.autodealer.crm.modules.identity.application.api.LoginSecurityService;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(MyAuthenticationSuccessHandler.class);

    @Resource
    private LoginAuditRecorder loginAuditRecorder;
    @Resource private LoginSecurityService loginSecurityService;
    @Resource private UserSessionService userSessionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 登录成功，执行该方法，在该方法中返回 json 给前端
        TUser tUser = (TUser) authentication.getPrincipal();
        try { if (loginSecurityService != null) loginSecurityService.recordSuccess(tUser.getId()); }
        catch (RuntimeException exception) { log.error("event=login_security_state result=failed userId={}",tUser.getId(),exception); writeSystemFailure(response); return; }

        com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.Issued issued;
        try {
            issued=userSessionService.create(tUser,Boolean.parseBoolean(request.getParameter("rememberMe")),request);
        } catch (RuntimeException exception) {
            log.error("event=login_session_write result=failed userId={}", tUser.getId(), exception);
            writeSystemFailure(response);
            return;
        }
        ((org.springframework.security.authentication.AbstractAuthenticationToken)authentication)
                .setDetails(new SessionAuthenticationDetails(issued.sessionId(),false));

        try {
            loginAuditRecorder.recordSuccess(tUser, request);
        } catch (RuntimeException exception) {
            try {
                userSessionService.revokeCurrentForLogout(tUser.getId(),issued.sessionId());
            } catch (RuntimeException cleanupException) {
                log.warn("event=login_session_cleanup result=failed userId={}",
                        tUser.getId(), cleanupException);
            }
            writeSystemFailure(response);
            return;
        }

        Result result = Result.OK(issued.token());

        // 把 Result 以 json 返回给前端
        ResponseUtils.write(response, JSONUtils.toJSON(result));
    }

    private void writeSystemFailure(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ResponseUtils.write(response, JSONUtils.toJSON(Result.FAIL(CodeEnum.SYSTEM_ERROR)));
    }
}
