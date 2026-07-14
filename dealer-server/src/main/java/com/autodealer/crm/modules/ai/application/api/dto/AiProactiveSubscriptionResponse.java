package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.persistence.model.TAiProactiveSubscription;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiProactiveSubscriptionResponse {
    private String subscriptionNo;
    private String subscriptionType;
    private String status;
    private String frequency;
    private String quietStartTime;
    private String quietEndTime;
    private Integer dailyLimit;
    private Integer maxResults;
    private Integer duplicateWindowMinutes;
    private String configSummary;
    private LocalDateTime lastTriggeredTime;
    private LocalDateTime nextTriggerTime;

    public static AiProactiveSubscriptionResponse from(TAiProactiveSubscription subscription) {
        AiProactiveSubscriptionResponse response = new AiProactiveSubscriptionResponse();
        response.setSubscriptionNo(subscription.getSubscriptionNo());
        response.setSubscriptionType(subscription.getSubscriptionType());
        response.setStatus(subscription.getStatus());
        response.setFrequency(subscription.getFrequency());
        response.setQuietStartTime(subscription.getQuietStartTime());
        response.setQuietEndTime(subscription.getQuietEndTime());
        response.setDailyLimit(subscription.getDailyLimit());
        response.setMaxResults(subscription.getMaxResults());
        response.setDuplicateWindowMinutes(subscription.getDuplicateWindowMinutes());
        response.setConfigSummary(subscription.getConfigSummary());
        response.setLastTriggeredTime(subscription.getLastTriggeredTime());
        response.setNextTriggerTime(subscription.getNextTriggerTime());
        return response;
    }
}
