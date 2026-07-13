package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

/** 不可变密码历史；只保存单向密码哈希。 */
@Data
public class TPasswordHistory {
    private Long id;
    private Integer userId;
    private String passwordHash;
    private Integer changedBy;
    private String changeReason;
    private LocalDateTime changedAt;
}
