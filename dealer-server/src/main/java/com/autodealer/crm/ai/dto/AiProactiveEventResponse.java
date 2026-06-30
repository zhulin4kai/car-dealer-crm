package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.model.TAiProactiveEvent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiProactiveEventResponse {
    private String eventNo;
    private String subscriptionNo;
    private String eventType;
    private String status;
    private String title;
    private String summary;
    private String detailSummary;
    private String objectType;
    private String objectId;
    private String severity;
    private LocalDateTime generatedTime;
    private LocalDateTime deliveredTime;
    private String errorCode;

    public static AiProactiveEventResponse from(TAiProactiveEvent event, String subscriptionNo) {
        AiProactiveEventResponse response = new AiProactiveEventResponse();
        response.setEventNo(event.getEventNo());
        response.setSubscriptionNo(subscriptionNo);
        response.setEventType(event.getEventType());
        response.setStatus(event.getStatus());
        response.setTitle(event.getTitle());
        response.setSummary(event.getSummary());
        response.setDetailSummary(event.getDetailSummary());
        response.setObjectType(event.getObjectType());
        response.setObjectId(event.getObjectId());
        response.setSeverity(event.getSeverity());
        response.setGeneratedTime(event.getGeneratedTime());
        response.setDeliveredTime(event.getDeliveredTime());
        response.setErrorCode(event.getErrorCode());
        return response;
    }
}
