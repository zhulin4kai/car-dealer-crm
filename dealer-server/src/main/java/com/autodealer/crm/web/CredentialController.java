package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.credential.CredentialDtos.*;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.CredentialService;
import com.autodealer.crm.service.impl.AdminAccessRecoveryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
public class CredentialController {
    private final CredentialService service;
    private final AdminAccessRecoveryService adminAccessRecovery;
    public CredentialController(CredentialService service,AdminAccessRecoveryService adminAccessRecovery){this.service=service;this.adminAccessRecovery=adminAccessRecovery;}
    @PostMapping("/api/credentials/activate") public R<CommandResult> activate(@Valid @RequestBody ActivateRequest q){return R.OK(service.activate(q));}
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/credentials/forgot-password") public R<RequestAccepted> forgot(@Valid @RequestBody ForgotRequest q){return R.OK(service.forgot(q));}
    @PostMapping("/api/credentials/reset-password") public R<CommandResult> reset(@Valid @RequestBody ResetRequest q){return R.OK(service.reset(q));}
    @PostMapping("/api/credentials/verify-contact") public R<CommandResult> verifyContact(@Valid @RequestBody VerifyContactRequest q){return R.OK(service.verifyContact(q));}
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/recovery/break-glass/request") public R<ManagedDeliveryResult> requestBreakGlass(@Valid @RequestBody BreakGlassRequest q){return R.OK(service.requestBreakGlass(q));}
    @PostMapping("/api/recovery/break-glass/complete") public R<CommandResult> completeBreakGlass(@Valid @RequestBody BreakGlassCompleteRequest q){return R.OK(service.completeBreakGlass(q));}
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/api/recovery/admin-access") public R<AdminAccessRecoveryResult> recoverAdminAccess(@Valid @RequestBody AdminAccessRecoveryRequest q){return R.OK(adminAccessRecovery.recover(q));}
    @PutMapping("/api/credentials/first-password-change") public R<CommandResult> first(@Valid @RequestBody ChangePasswordRequest q){return R.OK(service.firstChange(q));}
    @PutMapping("/api/credentials/change-password") public R<CommandResult> change(@Valid @RequestBody ChangePasswordRequest q){return R.OK(service.changeOwn(q));}
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/profile/contact-verification") public R<ManagedDeliveryResult> requestContactVerification(@Valid @RequestBody ContactVerificationRequest q){return R.OK(service.requestContactVerification(q));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_ADD+"')")
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/users/{id}/invitation") public R<ManagedDeliveryResult> reinvite(@PathVariable Integer id,@Valid @RequestBody ManagedRequest q){return R.OK(service.reinvite(id,q));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_PASSWORD+"')")
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/users/{id}/password-reset") public R<PasswordResetDeliveryResult> resetManaged(@PathVariable Integer id,@Valid @RequestBody ManagedRequest q){return R.OK(service.adminReset(id,q));}
}
