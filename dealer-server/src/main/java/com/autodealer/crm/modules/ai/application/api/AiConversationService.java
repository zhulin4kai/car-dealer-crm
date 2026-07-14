package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiConversationDetailResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiConversationResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunTraceResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CancelAiRunRequest;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiConversationRequest;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiRunRequest;
import com.autodealer.crm.modules.ai.application.api.dto.EditAiMessageRequest;
import com.autodealer.crm.modules.ai.application.api.dto.RenameAiConversationRequest;
import com.autodealer.crm.modules.ai.application.api.dto.WithdrawAiMessageRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiConversationService {
    List<AiConversationResponse> listConversations(boolean includeArchived);

    AiConversationResponse createConversation(CreateAiConversationRequest request);

    AiConversationDetailResponse getConversation(String conversationNo);

    AiConversationResponse renameConversation(String conversationNo, RenameAiConversationRequest request);

    AiConversationResponse archiveConversation(String conversationNo);

    AiRunResponse editMessage(String conversationNo, String messageNo, EditAiMessageRequest request);

    AiConversationDetailResponse withdrawMessage(String conversationNo, String messageNo,
                                                  WithdrawAiMessageRequest request);

    AiRunResponse createRun(CreateAiRunRequest request);

    AiRunResponse getRun(String runNo);

    AiRunTraceResponse getRunTrace(String runNo);

    SseEmitter streamRun(String runNo, int afterSequence);

    AiRunResponse cancelRun(String runNo, CancelAiRunRequest request);
}
