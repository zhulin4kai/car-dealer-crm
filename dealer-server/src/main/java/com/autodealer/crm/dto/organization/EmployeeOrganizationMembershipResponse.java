package com.autodealer.crm.dto.organization;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class EmployeeOrganizationMembershipResponse {
    private EmployeeSummaryResponse employee;
    private EmployeeAssignmentResponse primaryAssignment;
    private List<EmployeeAssignmentResponse> additionalAssignments = new ArrayList<>();
    private EmployeeReportingResponse reporting;
    private Integer version;
    private List<String> allowedActions = new ArrayList<>();
    private Map<String, String> unavailableReasons = new LinkedHashMap<>();
}
