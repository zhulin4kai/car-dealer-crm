package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.user.UserHistoryDtos.Collection;
import com.autodealer.crm.dto.user.UserHistoryDtos.Query;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.UserHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserHistoryController {
    private final UserHistoryService service;

    public UserHistoryController(UserHistoryService service) { this.service = service; }

    @PreAuthorize("hasAuthority('" + PermissionCodes.AUDIT_OPERATION_DETAIL + "')")
    @GetMapping("/api/users/{id}/history")
    public R<Collection> get(@PathVariable Integer id, @ModelAttribute Query query) {
        return R.OK(service.getUserHistory(id, query));
    }
}
