package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.Collection;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.RevokeRequest;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserSessionController {
    private final UserSessionService sessions;
    public UserSessionController(UserSessionService sessions){this.sessions=sessions;}

    @GetMapping("/api/me/sessions") public Result<Collection> own(){return Result.OK(sessions.ownSessions());}
    @PostMapping("/api/me/sessions/{sessionId}/revoke") public Result<Collection> revokeOwn(@PathVariable String sessionId,@Valid @RequestBody RevokeRequest request){return Result.OK(sessions.revokeOwn(sessionId,request));}
    @PostMapping("/api/me/sessions/revoke-others") public Result<Collection> revokeOwnOthers(@Valid @RequestBody RevokeRequest request){return Result.OK(sessions.revokeOwnOthers(request));}
    @PostMapping("/api/me/sessions/revoke-all") public Result<Collection> revokeOwnAll(@Valid @RequestBody RevokeRequest request){return Result.OK(sessions.revokeOwnAll(request));}

    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_VIEW+"')")
    @GetMapping("/api/users/{userId}/sessions") public Result<Collection> managed(@PathVariable Integer userId){return Result.OK(sessions.managedSessions(userId));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_STATUS+"')")
    @PostMapping("/api/users/{userId}/sessions/{sessionId}/revoke") public Result<Collection> revokeManaged(@PathVariable Integer userId,@PathVariable String sessionId,@Valid @RequestBody RevokeRequest request){return Result.OK(sessions.revokeManaged(userId,sessionId,request));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_STATUS+"')")
    @PostMapping("/api/users/{userId}/sessions/revoke-all") public Result<Collection> revokeManagedAll(@PathVariable Integer userId,@Valid @RequestBody RevokeRequest request){return Result.OK(sessions.revokeManagedAll(userId,request));}
}
