package com.autodealer.crm.ai;

import com.autodealer.crm.ai.model.TAiRun;

public record ToolExecutionContext(TAiRun run) {
    public Long runId() {
        return run.getId();
    }

    public String runNo() {
        return run.getRunNo();
    }
}
