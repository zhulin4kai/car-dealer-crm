package com.autodealer.crm.config.handler;

import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.R;
import com.autodealer.crm.util.JSONUtils;
import com.autodealer.crm.util.ResponseUtils;
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
    private RedisManager redisManager;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 退出成功，执行该方法，在该方法中返回 json 给前端就行了
        if (authentication != null && authentication.getPrincipal() instanceof TUser user) {
            boolean deleted;
            try {
                deleted = redisManager.delete(RedisKeys.userLogin(user.getId()));
            } catch (RuntimeException exception) {
                writeSystemFailure(response);
                return;
            }
            if (!deleted) {
                writeSystemFailure(response);
                return;
            }
        }

        R result = R.OK(CodeEnum.USER_LOGOUT);
        ResponseUtils.write(response, JSONUtils.toJSON(result));
    }

    private void writeSystemFailure(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ResponseUtils.write(response, JSONUtils.toJSON(R.FAIL(CodeEnum.SYSTEM_ERROR)));
    }
}
