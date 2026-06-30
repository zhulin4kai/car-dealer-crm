package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiApprovalCommand;
import com.autodealer.crm.ai.dto.AiCreateRunCommand;
import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiMessageCommand;
import com.autodealer.crm.ai.dto.AiProposalCommand;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.dto.AiToolCallCommand;
import com.autodealer.crm.ai.enums.AiEntryPoint;
import com.autodealer.crm.ai.enums.AiRunStatus;
import com.autodealer.crm.ai.model.TAiActionProposal;
import com.autodealer.crm.ai.model.TAiApproval;
import com.autodealer.crm.ai.model.TAiConversation;
import com.autodealer.crm.ai.model.TAiExecutionEvent;
import com.autodealer.crm.ai.model.TAiMessage;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiToolCall;

import java.util.List;

public interface AiTraceService {
    TAiConversation createConversation(AiEntryPoint entryPoint, String contextObjectType,
                                       String contextObjectId, String title);

    TAiConversation findOrCreateConversation(AiEntryPoint entryPoint, String contextObjectType,
                                             String contextObjectId, String title);

    TAiConversation getOwnedConversation(String conversationNo);

    TAiConversation getConversationById(Long conversationId);

    List<TAiConversation> listOwnedConversations(boolean includeArchived);

    TAiConversation renameConversation(String conversationNo, String title);

    TAiConversation archiveConversation(String conversationNo);

    List<TAiMessage> listConversationMessages(Long conversationId);

    List<TAiMessage> listRecentVisibleMessages(Long conversationId, Long excludeRunId, int limit);

    TAiRun getLatestRunByConversationId(Long conversationId);

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

    boolean cancelRunIfCancellable(Long runId, String reason);

    TAiMessage appendMessage(AiMessageCommand command);

    TAiToolCall recordToolCall(AiToolCallCommand command);

    TAiActionProposal saveProposal(AiProposalCommand command);

    TAiApproval recordApproval(AiApprovalCommand command);

    TAiExecutionEvent recordExecutionEvent(AiExecutionEventCommand command);
}
