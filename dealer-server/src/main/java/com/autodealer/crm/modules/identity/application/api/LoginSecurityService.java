package com.autodealer.crm.modules.identity.application.api;

public interface LoginSecurityService {
    void recordFailure(String loginAct);
    void recordSuccess(Integer userId);
}
