package com.autodealer.crm.service;

import com.autodealer.crm.dto.user.UserSessionDtos.Collection;
import com.autodealer.crm.dto.user.UserSessionDtos.Issued;
import com.autodealer.crm.dto.user.UserSessionDtos.RevokeRequest;
import com.autodealer.crm.model.TUser;
import jakarta.servlet.http.HttpServletRequest;

public interface UserSessionService {
    Issued create(TUser user, boolean rememberMe, HttpServletRequest request);
    boolean validateAndTouch(String rawToken,Integer userId,String sessionId,Long authVersion);
    Collection ownSessions();
    Collection revokeOwn(String sessionId,RevokeRequest request);
    Collection revokeOwnOthers(RevokeRequest request);
    Collection revokeOwnAll(RevokeRequest request);
    Collection managedSessions(Integer userId);
    Collection revokeManaged(Integer userId,String sessionId,RevokeRequest request);
    Collection revokeManagedAll(Integer userId,RevokeRequest request);
    void revokeCurrentForLogout(Integer userId,String sessionId);
    void revokeAllForSecurityChange(Integer userId,Integer operatorId,String reason);
}
