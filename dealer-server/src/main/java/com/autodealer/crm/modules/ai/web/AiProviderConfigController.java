package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiProviderConfigResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiProviderConfigTestResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiProviderConfigRequest;
import com.autodealer.crm.modules.ai.application.api.dto.RotateAiProviderKeyRequest;
import com.autodealer.crm.modules.ai.application.api.dto.UpdateAiProviderConfigRequest;
import com.autodealer.crm.modules.ai.application.api.AiProviderConfigService;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/provider-configs")
public class AiProviderConfigController {
    private final AiProviderConfigService providerConfigService;

    public AiProviderConfigController(AiProviderConfigService providerConfigService) {
        this.providerConfigService = providerConfigService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_VIEW + "')")
    public Result<List<AiProviderConfigResponse>> list() {
        return Result.OK(providerConfigService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public Result<AiProviderConfigResponse> create(@Valid @RequestBody CreateAiProviderConfigRequest request) {
        return Result.OK(providerConfigService.create(request));
    }

    @PutMapping("/{configNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public Result<AiProviderConfigResponse> update(@PathVariable String configNo,
                                              @Valid @RequestBody UpdateAiProviderConfigRequest request) {
        return Result.OK(providerConfigService.update(configNo, request));
    }

    @PostMapping("/{configNo}/rotate-key")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_ROTATE_KEY + "')")
    public Result<AiProviderConfigResponse> rotateKey(@PathVariable String configNo,
                                                 @Valid @RequestBody RotateAiProviderKeyRequest request) {
        return Result.OK(providerConfigService.rotateKey(configNo, request));
    }

    @PostMapping("/{configNo}/test")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public Result<AiProviderConfigTestResponse> test(@PathVariable String configNo) {
        return Result.OK(providerConfigService.test(configNo));
    }

    @PostMapping("/{configNo}/activate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public Result<AiProviderConfigResponse> activate(@PathVariable String configNo) {
        return Result.OK(providerConfigService.activate(configNo));
    }

    @PostMapping("/{configNo}/disable")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public Result<AiProviderConfigResponse> disable(@PathVariable String configNo) {
        return Result.OK(providerConfigService.disable(configNo));
    }
}
