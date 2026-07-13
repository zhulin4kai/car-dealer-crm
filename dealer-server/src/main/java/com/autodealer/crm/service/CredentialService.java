package com.autodealer.crm.service;

import com.autodealer.crm.dto.credential.CredentialDtos.*;

public interface CredentialService {
    CommandResult activate(ActivateRequest request);
    RequestAccepted forgot(ForgotRequest request);
    CommandResult reset(ResetRequest request);
    CommandResult firstChange(ChangePasswordRequest request);
    CommandResult changeOwn(ChangePasswordRequest request);
    ManagedDeliveryResult reinvite(Integer userId, ManagedRequest request);
    PasswordResetDeliveryResult adminReset(Integer userId, ManagedRequest request);
    ManagedDeliveryResult issueInvitation(Integer userId,String reason);
    void revokeAll(Integer userId);
    ManagedDeliveryResult requestContactVerification(ContactVerificationRequest request);
    CommandResult verifyContact(VerifyContactRequest request);
    ManagedDeliveryResult requestBreakGlass(BreakGlassRequest request);
    CommandResult completeBreakGlass(BreakGlassCompleteRequest request);
    ManagedDeliveryResult issueDegradedAdminRecovery(Integer userId,String reason);
}
