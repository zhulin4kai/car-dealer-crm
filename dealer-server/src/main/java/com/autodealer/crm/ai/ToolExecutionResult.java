package com.autodealer.crm.ai;

public record ToolExecutionResult(
        Object data,
        String outputSummary,
        String objectRefs
) {
    public static ToolExecutionResult of(Object data, String outputSummary, String objectRefs) {
        return new ToolExecutionResult(data, outputSummary, objectRefs);
    }
}
