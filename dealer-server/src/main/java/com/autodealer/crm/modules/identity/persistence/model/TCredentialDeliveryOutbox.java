package com.autodealer.crm.modules.identity.persistence.model;

import com.autodealer.crm.modules.identity.application.api.enums.CredentialPurpose;
import lombok.Data;

import java.time.LocalDateTime;

/** 提交后凭证投递事实；nonce 可派生原始凭证，但数据库从不保存原始凭证。 */
@Data
public class TCredentialDeliveryOutbox {
    private Long id;
    private String messageId;
    private Long credentialId;
    private Integer userId;
    private CredentialPurpose purpose;
    private String derivationNonce;
    private String phoneDigest;
    private String emailDigest;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime claimedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private String lastErrorCode;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime editTime;
}
