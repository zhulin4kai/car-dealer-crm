package com.autodealer.crm.dto.organization;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ActingReportingCollectionResponse {
    private Integer employeeId;
    private Integer employeeVersion;
    private List<ActingReportingRelationResponse> relations = new ArrayList<>();
    private List<String> allowedActions = new ArrayList<>();
    private Map<String, String> unavailableReasons = new LinkedHashMap<>();
}
