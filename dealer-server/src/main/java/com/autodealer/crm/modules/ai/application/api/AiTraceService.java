package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiApprovalCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiCreateRunCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiExecutionEventCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiMessageCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiProposalCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunTraceResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiToolCallCommand;
import com.autodealer.crm.modules.ai.application.api.enums.AiEntryPoint;
import com.autodealer.crm.modules.ai.application.api.enums.AiRunStatus;
import com.autodealer.crm.modules.ai.persistence.model.TAiActionProposal;
import com.autodealer.crm.modules.ai.persistence.model.TAiApproval;
import com.autodealer.crm.modules.ai.persistence.model.TAiConversation;
import com.autodealer.crm.modules.ai.persistence.model.TAiExecutionEvent;
import com.autodealer.crm.modules.ai.persistence.model.TAiMessage;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.persistence.model.TAiToolCall;

import java.util.List;

public interface AiTraceService {
    TAiConversation createConversation(AiEntryPoint entryPoint, String contextObjectType,
                                       String contextObjectId, String title);

    TAiConversation findOrCreateConversation(AiEntryPoint entryPoint, String contextObjectType,
                                             String contextObjectId, String title);

    TAiConversation getOwnedConversation(String conversationNo);

    TAiConversation lockOwnedConversation(String conversationNo);

    TAiConversation getConversationById(Long conversationId);

    List<TAiConversation> listOwnedConversations(boolean includeArchived);

    TAiConversation renameConversation(String conversationNo, String title);

    TAiConversation archiveConversation(String conversationNo);

    List<TAiMessage> listConversationMessages(Long conversationId);

    List<TAiMessage> listRecentVisibleMessages(Long conversationId, Long excludeRunId, int limit);

    List<TAiMessage> listActiveContextMessages(Long conversationId);

    TAiMessage getOwnedUserMessage(Long conversationId, String messageNo);

    void supersedeMessage(TAiMessage message, int expectedVersion);

    void withdrawMessage(TAiMessage message, int expectedVersion);

    TAiRun getLatestRunByConversationId(Long conversationId);

    TAiRun getLatestActiveRunBeforeTurn(Long conversationId, int turnNo);

    List<TAiRun> invalidateContextFromTurn(Long conversationId, int turnNo, String reason);

    List<TAiRun> listRunsByConversationId(Long conversationId);

    int nextTurnNo(Long conversationId);

    void updateConversationAfterRun(Long conversationId, String lastRunNo, String summaryText);

    TAiRun createRun(AiCreateRunCommand command);

    TAiRun getOwnedRun(String runNo);

    TAiRun getRunById(Long runId);

    List<TAiRun> listMyRuns(int page, int size);

    AiRunTraceResponse getOwnedRunTrace(String runNo);

    AiRunTraceResponse getRunTrace(TAiRun run);

    void updateRunStatus(Long runId, AiRunStatus status, String errorCode, String errorMessage);

    boolean updateRunStatusIfNotTerminal(Long runId, AiRunStatus status, String errorCode, String errorMessage);

    boolean startRunIfCreated(Long runId);

    boolean cancelRunIfCancellable(Long runId, String reason);

    TAiMessage appendMessage(AiMessageCommand command);

    TAiToolCall recordToolCall(AiToolCallCommand command);

    TAiActionProposal saveProposal(AiProposalCommand command);

    TAiApproval recordApproval(AiApprovalCommand command);

    TAiExecutionEvent recordExecutionEvent(AiExecutionEventCommand command);
}
