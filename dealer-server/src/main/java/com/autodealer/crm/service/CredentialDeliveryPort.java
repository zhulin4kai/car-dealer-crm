package com.autodealer.crm.service;

import com.autodealer.crm.enums.CredentialPurpose;
import java.time.LocalDateTime;

/** 原始凭证只允许越过该出站端口，不得出现在 HTTP DTO、数据库或日志。 */
public interface CredentialDeliveryPort {
    DeliveryStatus deliver(DeliveryMessage message);
    default DeliveryStatus validateTarget(String phone,String email) { return new DeliveryStatus("QUEUED"); }
    record DeliveryMessage(String messageId,Integer userId,String loginAct,String phone,String email,
                           CredentialPurpose purpose,String rawCredential,LocalDateTime expiresAt) {}
    record DeliveryStatus(String code) {}
}
