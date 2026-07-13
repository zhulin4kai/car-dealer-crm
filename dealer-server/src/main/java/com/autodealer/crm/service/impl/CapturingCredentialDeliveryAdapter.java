package com.autodealer.crm.service.impl;

import com.autodealer.crm.service.CredentialDeliveryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/** 仅 dev/test 的进程内捕获器，方便自动化测试；没有任何 HTTP 暴露。 */
@Component
@Profile({"dev","test"})
public class CapturingCredentialDeliveryAdapter implements CredentialDeliveryPort {
    private final ConcurrentHashMap<Integer,DeliveryMessage> latest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,DeliveryMessage> messages = new ConcurrentHashMap<>();
    @Override public DeliveryStatus deliver(DeliveryMessage message) { messages.putIfAbsent(message.messageId(),message);latest.put(message.userId(),messages.get(message.messageId()));return new DeliveryStatus("CAPTURED"); }
    @Override public DeliveryStatus validateTarget(String phone,String email) { return phone==null&&email==null?new DeliveryStatus("NO_DELIVERY_CONTACT"):new DeliveryStatus("QUEUED"); }
    public DeliveryMessage latestFor(Integer userId) { return latest.get(userId); }
    public DeliveryMessage byMessageId(String messageId) { return messages.get(messageId); }
    public void clear() { latest.clear();messages.clear(); }
}
