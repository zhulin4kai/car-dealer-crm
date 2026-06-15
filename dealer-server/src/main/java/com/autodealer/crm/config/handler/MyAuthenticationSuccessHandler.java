package com.autodealer.crm.config.handler;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.RedisService;
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
import java.util.concurrent.TimeUnit;

@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Resource
    private RedisService redisService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //登录成功，执行该方法，在该方法中返回json给前端，就行了
        TUser tUser = (TUser) authentication.getPrincipal();

        //1、生成jwt
        //JWT 只保存后续鉴权必需的数据，菜单树通过 /api/login/info 实时查询，避免 token 过大。
        TUser tokenUser = new TUser();
        tokenUser.setId(tUser.getId());
        tokenUser.setLoginAct(tUser.getLoginAct());
        tokenUser.setLoginPwd(tUser.getLoginPwd());
        tokenUser.setName(tUser.getName());
        tokenUser.setPhone(tUser.getPhone());
        tokenUser.setEmail(tUser.getEmail());
        tokenUser.setAccountNoExpired(tUser.getAccountNoExpired());
        tokenUser.setCredentialsNoExpired(tUser.getCredentialsNoExpired());
        tokenUser.setAccountNoLocked(tUser.getAccountNoLocked());
        tokenUser.setAccountEnabled(tUser.getAccountEnabled());
        tokenUser.setRoleList(tUser.getRoleList());
        tokenUser.setPermissionList(tUser.getPermissionList());
        String userJSON = JSONUtils.toJSON(tokenUser);
        String jwt = JWTUtils.createJWT(userJSON);

        //2、写入redis
        redisService.setValue(Constants.REDIS_JWT_KEY + tUser.getId(), jwt);

        //3、设置jwt的过期时间(如果选择了记住我，过期时间是7天，否则是30分钟)
        String rememberMe = request.getParameter("rememberMe");
        if (Boolean.parseBoolean(rememberMe)) {
            redisService.expire(Constants.REDIS_JWT_KEY + tUser.getId(), Constants.EXPIRE_TIME, TimeUnit.SECONDS);
        } else {
            redisService.expire(Constants.REDIS_JWT_KEY + tUser.getId(), Constants.DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
        }

        //登录成功的统一结果
        R result = R.OK(jwt);

        //把R对象转成json
        String resultJSON = JSONUtils.toJSON(result);

        //把R以json返回给前端
        ResponseUtils.write(response, resultJSON);
    }
}
