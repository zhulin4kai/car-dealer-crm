package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiProviderConfigResponse;
import com.autodealer.crm.ai.dto.AiProviderConfigTestResponse;
import com.autodealer.crm.ai.dto.CreateAiProviderConfigRequest;
import com.autodealer.crm.ai.dto.ProviderRuntimeConfig;
import com.autodealer.crm.ai.dto.RotateAiProviderKeyRequest;
import com.autodealer.crm.ai.dto.UpdateAiProviderConfigRequest;

import java.util.List;

public interface AiProviderConfigService {
    List<AiProviderConfigResponse> list();

    AiProviderConfigResponse create(CreateAiProviderConfigRequest request);

    AiProviderConfigResponse update(String configNo, UpdateAiProviderConfigRequest request);

    AiProviderConfigResponse rotateKey(String configNo, RotateAiProviderKeyRequest request);

    AiProviderConfigTestResponse test(String configNo);

    AiProviderConfigResponse activate(String configNo);

    AiProviderConfigResponse disable(String configNo);

    ProviderRuntimeConfig getEnabledRuntimeConfig();
}
