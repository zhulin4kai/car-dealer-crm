package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiProviderConfigResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiProviderConfigTestResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiProviderConfigRequest;
import com.autodealer.crm.modules.ai.application.api.dto.ProviderRuntimeConfig;
import com.autodealer.crm.modules.ai.application.api.dto.RotateAiProviderKeyRequest;
import com.autodealer.crm.modules.ai.application.api.dto.UpdateAiProviderConfigRequest;

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
