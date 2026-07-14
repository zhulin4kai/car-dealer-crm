package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.credential.CredentialDtos.*;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.CredentialService;
import com.autodealer.crm.modules.identity.application.internal.AdminAccessRecoveryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
public class CredentialController {
    private final CredentialService service;
    private final AdminAccessRecoveryService adminAccessRecovery;
    public CredentialController(CredentialService service,AdminAccessRecoveryService adminAccessRecovery){this.service=service;this.adminAccessRecovery=adminAccessRecovery;}
    @PostMapping("/api/credentials/activate") public Result<CommandResult> activate(@Valid @RequestBody ActivateRequest q){return Result.OK(service.activate(q));}
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/credentials/forgot-password") public Result<RequestAccepted> forgot(@Valid @RequestBody ForgotRequest q){return Result.OK(service.forgot(q));}
    @PostMapping("/api/credentials/reset-password") public Result<CommandResult> reset(@Valid @RequestBody ResetRequest q){return Result.OK(service.reset(q));}
    @PostMapping("/api/credentials/verify-contact") public Result<CommandResult> verifyContact(@Valid @RequestBody VerifyContactRequest q){return Result.OK(service.verifyContact(q));}
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/recovery/break-glass/request") public Result<ManagedDeliveryResult> requestBreakGlass(@Valid @RequestBody BreakGlassRequest q){return Result.OK(service.requestBreakGlass(q));}
    @PostMapping("/api/recovery/break-glass/complete") public Result<CommandResult> completeBreakGlass(@Valid @RequestBody BreakGlassCompleteRequest q){return Result.OK(service.completeBreakGlass(q));}
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/api/recovery/admin-access") public Result<AdminAccessRecoveryResult> recoverAdminAccess(@Valid @RequestBody AdminAccessRecoveryRequest q){return Result.OK(adminAccessRecovery.recover(q));}
    @PutMapping("/api/credentials/first-password-change") public Result<CommandResult> first(@Valid @RequestBody ChangePasswordRequest q){return Result.OK(service.firstChange(q));}
    @PutMapping("/api/credentials/change-password") public Result<CommandResult> change(@Valid @RequestBody ChangePasswordRequest q){return Result.OK(service.changeOwn(q));}
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/profile/contact-verification") public Result<ManagedDeliveryResult> requestContactVerification(@Valid @RequestBody ContactVerificationRequest q){return Result.OK(service.requestContactVerification(q));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_ADD+"')")
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/users/{id}/invitation") public Result<ManagedDeliveryResult> reinvite(@PathVariable Integer id,@Valid @RequestBody ManagedRequest q){return Result.OK(service.reinvite(id,q));}
    @PreAuthorize("hasAuthority('"+PermissionCodes.USER_PASSWORD+"')")
    @ResponseStatus(HttpStatus.ACCEPTED) @PostMapping("/api/users/{id}/password-reset") public Result<PasswordResetDeliveryResult> resetManaged(@PathVariable Integer id,@Valid @RequestBody ManagedRequest q){return Result.OK(service.adminReset(id,q));}
}
