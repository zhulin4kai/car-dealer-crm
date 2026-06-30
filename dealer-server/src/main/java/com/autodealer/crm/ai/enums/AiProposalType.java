package com.autodealer.crm.ai.enums;

public enum AiProposalType {
    CREATE_COMMUNICATION_RECORD("create_communication_record_proposal"),
    CREATE_FOLLOW_TASK("create_follow_task_proposal");

    private final String code;

    AiProposalType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
