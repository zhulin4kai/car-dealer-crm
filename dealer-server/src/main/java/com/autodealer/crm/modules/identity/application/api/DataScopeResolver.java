package com.autodealer.crm.modules.identity.application.api;

public interface DataScopeResolver {
    AuthorizationDataScope resolve(Integer userId, String permissionCode);
}
