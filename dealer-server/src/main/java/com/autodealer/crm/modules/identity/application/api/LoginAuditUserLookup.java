package com.autodealer.crm.modules.identity.application.api;

import java.util.Optional;

/** 登录审计关联身份摘要的只读公开端口，不暴露身份持久化对象。 */
public interface LoginAuditUserLookup {

    Optional<UserSummary> findByLoginAct(String loginAct);

    record UserSummary(Integer userId, String userName) {
    }
}
