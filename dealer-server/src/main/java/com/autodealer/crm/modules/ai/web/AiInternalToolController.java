package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiToolExecutionResponse;
import com.autodealer.crm.modules.ai.application.api.dto.ExecuteAiToolRequest;
import com.autodealer.crm.modules.ai.application.api.AiInternalToolService;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.web.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/tools")
public class AiInternalToolController {
    private final AiInternalToolService aiInternalToolService;
    private final String toolToken;

    public AiInternalToolController(AiInternalToolService aiInternalToolService,
                                    @Value("${ai.dealer-ai.tool-token:${ai.dealer-ai.internal-token:dev-internal-token}}")
                                    String toolToken) {
        this.aiInternalToolService = aiInternalToolService;
        this.toolToken = toolToken;
    }

    @PostMapping("/{toolName}/execute")
    public Result<AiToolExecutionResponse> execute(@PathVariable String toolName,
                                              @RequestHeader(value = "X-Dealer-AI-Tool-Token", required = false) String token,
                                              @Valid @RequestBody ExecuteAiToolRequest request) {
        if (!StringUtils.hasText(token) || !token.equals(toolToken)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "内部 AI 工具令牌无效");
        }
        return Result.OK(aiInternalToolService.execute(toolName, request));
    }
}
