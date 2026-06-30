package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiConversationDetailResponse;
import com.autodealer.crm.ai.dto.AiConversationResponse;
import com.autodealer.crm.ai.dto.AiRunResponse;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.dto.CancelAiRunRequest;
import com.autodealer.crm.ai.dto.CreateAiConversationRequest;
import com.autodealer.crm.ai.dto.CreateAiRunRequest;
import com.autodealer.crm.ai.dto.RenameAiConversationRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiConversationService {
    List<AiConversationResponse> listConversations(boolean includeArchived);

    AiConversationResponse createConversation(CreateAiConversationRequest request);

    AiConversationDetailResponse getConversation(String conversationNo);

    AiConversationResponse renameConversation(String conversationNo, RenameAiConversationRequest request);

    AiConversationResponse archiveConversation(String conversationNo);

    AiRunResponse createRun(CreateAiRunRequest request);

    AiRunResponse getRun(String runNo);

    AiRunTraceResponse getRunTrace(String runNo);

    SseEmitter streamRun(String runNo);

    AiRunResponse cancelRun(String runNo, CancelAiRunRequest request);
}
