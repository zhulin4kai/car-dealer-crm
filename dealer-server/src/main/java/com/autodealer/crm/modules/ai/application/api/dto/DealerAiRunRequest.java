package com.autodealer.crm.modules.ai.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DealerAiRunRequest {
    @JsonProperty("run_id")
    private String runId;

    @JsonProperty("user_prompt")
    private String userPrompt;

    @JsonProperty("conversation_no")
    private String conversationNo;

    @JsonProperty("conversation_summary")
    private String conversationSummary;

    @JsonProperty("message_history")
    private List<DealerAiMessageHistory> messageHistory;

    private Map<String, String> context;

    @JsonProperty("tool_schemas")
    private List<Map<String, Object>> toolSchemas;

    @JsonProperty("allow_proposals")
    private boolean allowProposals;

    @JsonProperty("provider_runtime_config")
    private ProviderRuntimeConfig providerRuntimeConfig;

    @JsonProperty("assistant_policy")
    private Map<String, Object> assistantPolicy;
}
