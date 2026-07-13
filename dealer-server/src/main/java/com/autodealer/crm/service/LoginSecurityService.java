package com.autodealer.crm.service;

public interface LoginSecurityService {
    void recordFailure(String loginAct);
    void recordSuccess(Integer userId);
}
