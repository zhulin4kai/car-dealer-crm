package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryDtos.Collection;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryDtos.Query;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.UserHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserHistoryController {
    private final UserHistoryService service;

    public UserHistoryController(UserHistoryService service) { this.service = service; }

    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_OPERATION_DETAIL + "')")
    @GetMapping("/api/users/{id}/history")
    public Result<Collection> get(@PathVariable Integer id, @ModelAttribute Query query) {
        return Result.OK(service.getUserHistory(id, query));
    }
}
