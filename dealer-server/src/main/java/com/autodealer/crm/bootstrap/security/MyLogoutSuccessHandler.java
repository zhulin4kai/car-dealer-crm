package com.autodealer.crm.bootstrap.security;

import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.shared.infrastructure.json.JSONUtils;
import com.autodealer.crm.shared.web.ResponseUtils;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import com.autodealer.crm.modules.identity.application.api.security.SessionAuthenticationDetails;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * 退出成功处理器
 *
 */
@Component
public class MyLogoutSuccessHandler implements LogoutSuccessHandler {

    @Resource
    private UserSessionService userSessionService;
    @Resource private RedisManager redisManager;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 退出成功，执行该方法，在该方法中返回 json 给前端就行了
        if (authentication != null && authentication.getPrincipal() instanceof TUser user) {
            try {
                if (authentication.getDetails() instanceof SessionAuthenticationDetails details && !details.legacy()) {
                    userSessionService.revokeCurrentForLogout(user.getId(),details.sessionId());
                } else if (!redisManager.delete(RedisKeys.userLogin(user.getId()))) {
                    throw new BusinessException(CodeEnum.SESSION_CACHE_FAILED);
                }
            } catch (BusinessException exception) {
                if (exception.getCodeEnum() == CodeEnum.SESSION_CACHE_FAILED) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    ResponseUtils.write(response, JSONUtils.toJSON(Result.FAIL(CodeEnum.SESSION_CACHE_FAILED)));
                    return;
                }
                writeSystemFailure(response);
                return;
            } catch (RuntimeException exception) {
                writeSystemFailure(response);
                return;
            }
        }

        Result result = Result.OK(CodeEnum.USER_LOGOUT);
        ResponseUtils.write(response, JSONUtils.toJSON(result));
    }

    private void writeSystemFailure(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ResponseUtils.write(response, JSONUtils.toJSON(Result.FAIL(CodeEnum.SYSTEM_ERROR)));
    }
}
