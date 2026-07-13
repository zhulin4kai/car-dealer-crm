package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.user.UserSessionDtos.Collection;
import com.autodealer.crm.dto.user.UserSessionDtos.RevokeRequest;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.UserSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserSessionController {
    private final UserSessionService sessions;
    public UserSessionController(UserSessionService sessions){this.sessions=sessions;}

    @GetMapping("/api/me/sessions") public R<Collection> own(){return R.OK(sessions.ownSessions());}
    @PostMapping("/api/me/sessions/{sessionId}/revoke") public R<Collection> revokeOwn(@PathVariable String sessionId,@Valid @RequestBody RevokeRequest request){return R.OK(sessions.revokeOwn(sessionId,request));}
    @PostMapping("/api/me/sessions/revoke-others") public R<Collection> revokeOwnOthers(@Valid @RequestBody RevokeRequest request){return R.OK(sessions.revokeOwnOthers(request));}
    @PostMapping("/api/me/sessions/revoke-all") public R<Collection> revokeOwnAll(@Valid @RequestBody RevokeRequest request){return R.OK(sessions.revokeOwnAll(request));}

    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_VIEW+"')")
    @GetMapping("/api/users/{userId}/sessions") public R<Collection> managed(@PathVariable Integer userId){return R.OK(sessions.managedSessions(userId));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_STATUS+"')")
    @PostMapping("/api/users/{userId}/sessions/{sessionId}/revoke") public R<Collection> revokeManaged(@PathVariable Integer userId,@PathVariable String sessionId,@Valid @RequestBody RevokeRequest request){return R.OK(sessions.revokeManaged(userId,sessionId,request));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_STATUS+"')")
    @PostMapping("/api/users/{userId}/sessions/revoke-all") public R<Collection> revokeManagedAll(@PathVariable Integer userId,@Valid @RequestBody RevokeRequest request){return R.OK(sessions.revokeManagedAll(userId,request));}
}
