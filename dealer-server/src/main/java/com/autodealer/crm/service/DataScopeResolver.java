package com.autodealer.crm.service;

public interface DataScopeResolver {
    AuthorizationDataScope resolve(Integer userId, String permissionCode);
}
