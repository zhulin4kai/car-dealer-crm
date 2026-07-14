package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.Collection;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.Issued;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.RevokeRequest;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
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
