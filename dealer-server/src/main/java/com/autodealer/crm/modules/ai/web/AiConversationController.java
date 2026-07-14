package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiConversationDetailResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiConversationResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiConversationRequest;
import com.autodealer.crm.modules.ai.application.api.dto.EditAiMessageRequest;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunResponse;
import com.autodealer.crm.modules.ai.application.api.dto.RenameAiConversationRequest;
import com.autodealer.crm.modules.ai.application.api.dto.WithdrawAiMessageRequest;
import com.autodealer.crm.modules.ai.application.api.AiConversationService;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
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
    public Result<List<AiConversationResponse>> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return Result.OK(aiConversationService.listConversations(includeArchived));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public Result<AiConversationResponse> create(@Valid @RequestBody CreateAiConversationRequest request) {
        return Result.OK(aiConversationService.createConversation(request));
    }

    @GetMapping("/{conversationNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public Result<AiConversationDetailResponse> detail(@PathVariable String conversationNo) {
        return Result.OK(aiConversationService.getConversation(conversationNo));
    }

    @PatchMapping("/{conversationNo}/title")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public Result<AiConversationResponse> rename(@PathVariable String conversationNo,
                                            @Valid @RequestBody RenameAiConversationRequest request) {
        return Result.OK(aiConversationService.renameConversation(conversationNo, request));
    }

    @PostMapping("/{conversationNo}/archive")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public Result<AiConversationResponse> archive(@PathVariable String conversationNo) {
        return Result.OK(aiConversationService.archiveConversation(conversationNo));
    }

    @PatchMapping("/{conversationNo}/messages/{messageNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public Result<AiRunResponse> editMessage(@PathVariable String conversationNo,
                                        @PathVariable String messageNo,
                                        @Valid @RequestBody EditAiMessageRequest request) {
        return Result.OK(aiConversationService.editMessage(conversationNo, messageNo, request));
    }

    @PostMapping("/{conversationNo}/messages/{messageNo}/withdraw")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public Result<AiConversationDetailResponse> withdrawMessage(
            @PathVariable String conversationNo,
            @PathVariable String messageNo,
            @Valid @RequestBody WithdrawAiMessageRequest request) {
        return Result.OK(aiConversationService.withdrawMessage(conversationNo, messageNo, request));
    }
}
