package com.autodealer.crm.modules.ai.application.api.tool;

import com.autodealer.crm.modules.ai.persistence.model.TAiRun;

public record ToolExecutionContext(TAiRun run) {
    public Long runId() {
        return run.getId();
    }

    public String runNo() {
        return run.getRunNo();
    }
}
