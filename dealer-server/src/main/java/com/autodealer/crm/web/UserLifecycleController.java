package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.user.UserLifecycleDtos.*;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.UserLifecycleService;
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
    @GetMapping public R<Context> context(@PathVariable Integer userId,
                                          @RequestParam(required = false) Integer organizationUnitId) {
        return R.OK(service.getContext(userId, organizationUnitId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "') and hasAuthority('" + PermissionCodes.EMPLOYEE_ASSIGNMENT + "')")
    @PostMapping("/transfer") public R<Context> transfer(@PathVariable Integer userId,@Valid @RequestBody AssignmentCommand request) { return R.OK(service.transfer(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/precheck") public R<DeparturePrecheck> precheck(@PathVariable Integer userId,@Valid @RequestBody DeparturePrecheckRequest request) { return R.OK(service.precheckDeparture(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/start") public R<Context> start(@PathVariable Integer userId,@Valid @RequestBody StartDepartureRequest request) { return R.OK(service.startDeparture(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/handover") public R<HandoverResult> handover(@PathVariable Integer userId,@Valid @RequestBody ConfirmHandoverRequest request) { return R.OK(service.confirmHandover(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping("/departure/complete") public R<Context> complete(@PathVariable Integer userId,@Valid @RequestBody CompleteDepartureRequest request) { return R.OK(service.completeDeparture(userId,request)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_STATUS + "') and hasAuthority('" + PermissionCodes.EMPLOYEE_ASSIGNMENT + "')")
    @PostMapping("/rehire")
    public ResponseEntity<R<RehireResult>> rehire(@PathVariable Integer userId,
                                                   @Valid @RequestBody RehireRequest request) {
        RehireResult result = service.rehire(userId, request);
        R<RehireResult> body = R.OK(result);
        if ("NOT_REQUIRED".equals(result.getCredentialDeliveryStatus())) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
}
