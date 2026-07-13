package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

/** 登录账号标识的永久归属事实；退休标识不得转移给其他用户。 */
@Data
public class TLoginIdentifier {
    private Long id;
    private Integer userId;
    private String loginAct;
    private String status;
    private Integer activeMarker;
    private LocalDateTime retiredAt;
    private Integer changedBy;
    private String reason;
    private Integer version;
    private LocalDateTime createTime;
}
