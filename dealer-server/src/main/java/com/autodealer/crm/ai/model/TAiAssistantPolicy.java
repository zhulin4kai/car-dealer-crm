package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiAssistantPolicy implements Serializable {
    private Long id;
    private Boolean enabledTools;
    private String allowedToolNames;
    private Boolean proposalsEnabled;
    private Integer maxToolCallsPerRun;
    private String safetyMode;
    private String networkMode;
    private Integer contextMessageLimit;
    private Integer summaryMaxChars;
    private Integer maxRunSeconds;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
