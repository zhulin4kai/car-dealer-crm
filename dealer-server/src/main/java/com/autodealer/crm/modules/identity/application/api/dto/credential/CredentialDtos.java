package com.autodealer.crm.modules.identity.application.api.dto.credential;

import com.autodealer.crm.modules.identity.application.api.dto.ChangePasswordRequest;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public final class CredentialDtos {
    private CredentialDtos() {}
    @Data public static class ActivateRequest { @NotBlank private String credential; @NotBlank private String newPassword; }
    @Data public static class ForgotRequest { @NotBlank @Size(max=64) private String loginAct; }
    @Data public static class ResetRequest { @NotBlank private String credential; @NotBlank private String newPassword; }
    @Data public static class ChangePasswordRequest { @NotBlank private String currentPassword; @NotBlank private String newPassword; }
    @Data public static class ManagedRequest { @NotNull @Min(0) private Integer accountVersion; @NotBlank @Size(max=500) private String reason; }
    public enum ContactChannel { PHONE, EMAIL }
    @Data public static class ContactVerificationRequest { @NotNull private ContactChannel channel; }
    @Data public static class VerifyContactRequest { @NotBlank private String credential; }
    @Data public static class BreakGlassRequest { @NotBlank @Size(max=64) private String loginAct; @NotBlank @Size(min=32,max=512) private String recoveryKey; }
    @Data public static class BreakGlassCompleteRequest { @NotBlank private String credential; @NotBlank private String newPassword; }
    @Data public static class AdminAccessRecoveryRequest { @NotBlank @Size(min=32,max=512) private String recoveryKey; @NotBlank @Size(max=500) private String reason;
        @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("管理员恢复请求不允许字段: "+name);} }
    public record CommandResult(boolean completed) {}
    public record RequestAccepted(boolean accepted,String deliveryStatus) {}
    public record ManagedDeliveryResult(boolean accepted,String deliveryStatus) {}
    public record PasswordResetDeliveryResult(boolean accepted,String deliveryStatus,boolean mustChangePassword) {}
    public record AdminAccessRecoveryResult(Integer recoveredUserId,String loginAct,String accountStatus,
                                            boolean accepted,String deliveryStatus) {}
}
