package com.autodealer.crm.modules.identity.persistence.model;

import lombok.Data;
import java.time.LocalDateTime;

/** 独立登录会话事实。只保存 Token HMAC 摘要，不保存原始 JWT。 */
@Data
public class TUserSession {
    private Long id;
    private String sessionId;
    private Integer userId;
    private String tokenDigest;
    private Long issuedAuthVersion;
    private Boolean rememberMe;
    private String deviceSummary;
    private String clientSummary;
    private String networkSummary;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime idleExpiresAt;
    private LocalDateTime absoluteExpiresAt;
    private LocalDateTime revokedAt;
    private Integer revokedBy;
    private String revokeReason;
    private String revokeType;
    private Integer version;
    private LocalDateTime createTime;
}
