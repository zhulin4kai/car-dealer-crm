package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExecuteAiToolRequest {
    @NotBlank(message = "AI Run 编号不能为空")
    private String runNo;

    @NotNull(message = "工具参数不能为空")
    private Map<String, Object> arguments = new LinkedHashMap<>();
}
