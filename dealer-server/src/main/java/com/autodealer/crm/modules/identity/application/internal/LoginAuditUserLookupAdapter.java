package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.LoginAuditUserLookup;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 使用身份持久化层实现登录审计所需的最小只读查询。 */
@Component
public class LoginAuditUserLookupAdapter implements LoginAuditUserLookup {

    private final TUserMapper userMapper;

    public LoginAuditUserLookupAdapter(TUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<UserSummary> findByLoginAct(String loginAct) {
        TUser user = userMapper.selectByLoginAct(loginAct);
        return Optional.ofNullable(user)
                .map(value -> new UserSummary(value.getId(), value.getName()));
    }
}
