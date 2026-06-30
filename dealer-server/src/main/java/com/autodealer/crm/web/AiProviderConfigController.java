package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiProviderConfigResponse;
import com.autodealer.crm.ai.dto.AiProviderConfigTestResponse;
import com.autodealer.crm.ai.dto.CreateAiProviderConfigRequest;
import com.autodealer.crm.ai.dto.RotateAiProviderKeyRequest;
import com.autodealer.crm.ai.dto.UpdateAiProviderConfigRequest;
import com.autodealer.crm.ai.service.AiProviderConfigService;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.R;
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
    public R<List<AiProviderConfigResponse>> list() {
        return R.OK(providerConfigService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public R<AiProviderConfigResponse> create(@Valid @RequestBody CreateAiProviderConfigRequest request) {
        return R.OK(providerConfigService.create(request));
    }

    @PutMapping("/{configNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public R<AiProviderConfigResponse> update(@PathVariable String configNo,
                                              @Valid @RequestBody UpdateAiProviderConfigRequest request) {
        return R.OK(providerConfigService.update(configNo, request));
    }

    @PostMapping("/{configNo}/rotate-key")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_ROTATE_KEY + "')")
    public R<AiProviderConfigResponse> rotateKey(@PathVariable String configNo,
                                                 @Valid @RequestBody RotateAiProviderKeyRequest request) {
        return R.OK(providerConfigService.rotateKey(configNo, request));
    }

    @PostMapping("/{configNo}/test")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public R<AiProviderConfigTestResponse> test(@PathVariable String configNo) {
        return R.OK(providerConfigService.test(configNo));
    }

    @PostMapping("/{configNo}/activate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public R<AiProviderConfigResponse> activate(@PathVariable String configNo) {
        return R.OK(providerConfigService.activate(configNo));
    }

    @PostMapping("/{configNo}/disable")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROVIDER_CONFIG_MANAGE + "')")
    public R<AiProviderConfigResponse> disable(@PathVariable String configNo) {
        return R.OK(providerConfigService.disable(configNo));
    }
}
