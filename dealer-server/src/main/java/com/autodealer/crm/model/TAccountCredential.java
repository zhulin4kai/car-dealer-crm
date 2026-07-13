package com.autodealer.crm.model;

import com.autodealer.crm.enums.CredentialPurpose;
import com.autodealer.crm.enums.CredentialStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 一次性账号凭证事实。投递前保存预交付承诺，Worker 提交后绑定实际 HMAC 摘要，绝不保存原始凭证。 */
@Data
public class TAccountCredential {
    private Long id;
    private Integer userId;
    private CredentialPurpose purpose;
    private String tokenDigest;
    private CredentialStatus status;
    private Boolean activeMarker;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime revokedAt;
    private Integer issuedBy;
    private String reason;
    /** 联系方式验证凭证签发时绑定的 HMAC 摘要；数据库不保存明文联系方式。 */
    private String targetValueDigest;
    /** 联系方式验证凭证签发时绑定的员工资料版本。 */
    private Integer targetProfileVersion;
    private Integer version;
    private LocalDateTime createTime;
}
