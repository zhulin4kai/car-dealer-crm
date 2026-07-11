package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiConversationDetailResponse;
import com.autodealer.crm.ai.dto.AiConversationResponse;
import com.autodealer.crm.ai.dto.CreateAiConversationRequest;
import com.autodealer.crm.ai.dto.EditAiMessageRequest;
import com.autodealer.crm.ai.dto.AiRunResponse;
import com.autodealer.crm.ai.dto.RenameAiConversationRequest;
import com.autodealer.crm.ai.dto.WithdrawAiMessageRequest;
import com.autodealer.crm.ai.service.AiConversationService;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.R;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/conversations")
public class AiConversationController {
    private final AiConversationService aiConversationService;

    public AiConversationController(AiConversationService aiConversationService) {
        this.aiConversationService = aiConversationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public R<List<AiConversationResponse>> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return R.OK(aiConversationService.listConversations(includeArchived));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiConversationResponse> create(@Valid @RequestBody CreateAiConversationRequest request) {
        return R.OK(aiConversationService.createConversation(request));
    }

    @GetMapping("/{conversationNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public R<AiConversationDetailResponse> detail(@PathVariable String conversationNo) {
        return R.OK(aiConversationService.getConversation(conversationNo));
    }

    @PatchMapping("/{conversationNo}/title")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiConversationResponse> rename(@PathVariable String conversationNo,
                                            @Valid @RequestBody RenameAiConversationRequest request) {
        return R.OK(aiConversationService.renameConversation(conversationNo, request));
    }

    @PostMapping("/{conversationNo}/archive")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiConversationResponse> archive(@PathVariable String conversationNo) {
        return R.OK(aiConversationService.archiveConversation(conversationNo));
    }

    @PatchMapping("/{conversationNo}/messages/{messageNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiRunResponse> editMessage(@PathVariable String conversationNo,
                                        @PathVariable String messageNo,
                                        @Valid @RequestBody EditAiMessageRequest request) {
        return R.OK(aiConversationService.editMessage(conversationNo, messageNo, request));
    }

    @PostMapping("/{conversationNo}/messages/{messageNo}/withdraw")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiConversationDetailResponse> withdrawMessage(
            @PathVariable String conversationNo,
            @PathVariable String messageNo,
            @Valid @RequestBody WithdrawAiMessageRequest request) {
        return R.OK(aiConversationService.withdrawMessage(conversationNo, messageNo, request));
    }
}
