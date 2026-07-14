package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserLifecycleDtos.*;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.UserLifecycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/lifecycle")
public class UserLifecycleController {
    private final UserLifecycleService service;
    public UserLifecycleController(UserLifecycleService service) { this.service = service; }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_VIEW + "')")
    @GetMapping public Result<Context> context(@PathVariable Integer userId,
                                          @RequestParam(required = false) Integer organizationUnitId) {
        return Result.OK(service.getContext(userId, organizationUnitId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "') and hasAuthority('" + PermissionCodes.EMPLOYEE_ASSIGNMENT + "')")
    @PostMapping("/transfer") public Result<Context> transfer(@PathVariable Integer userId,@Valid @RequestBody AssignmentCommand request) { return Result.OK(service.transfer(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/precheck") public Result<DeparturePrecheck> precheck(@PathVariable Integer userId,@Valid @RequestBody DeparturePrecheckRequest request) { return Result.OK(service.precheckDeparture(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/start") public Result<Context> start(@PathVariable Integer userId,@Valid @RequestBody StartDepartureRequest request) { return Result.OK(service.startDeparture(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/handover") public Result<HandoverResult> handover(@PathVariable Integer userId,@Valid @RequestBody ConfirmHandoverRequest request) { return Result.OK(service.confirmHandover(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/complete") public Result<Context> complete(@PathVariable Integer userId,@Valid @RequestBody CompleteDepartureRequest request) { return Result.OK(service.completeDeparture(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "') and hasAuthority('" + PermissionCodes.EMPLOYEE_ASSIGNMENT + "')")
    @PostMapping("/rehire")
    public ResponseEntity<Result<RehireResult>> rehire(@PathVariable Integer userId,
                                                   @Valid @RequestBody RehireRequest request) {
        RehireResult result = service.rehire(userId, request);
        Result<RehireResult> body = Result.OK(result);
        if ("NOT_REQUIRED".equals(result.getCredentialDeliveryStatus())) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
}
