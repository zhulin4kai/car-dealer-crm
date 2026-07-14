package com.autodealer.crm.modules.identity.application.api.dto.organization;

import lombok.Data;

@Data
public class PositionResponse {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private Integer positionLevel;
    private Boolean builtIn;
    private Boolean enabled;
    private Integer version;
}
