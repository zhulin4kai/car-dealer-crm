package com.autodealer.crm.modules.identity.application.api.dto.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateEmployeeOrganizationRequest {
    @NotNull @Min(0) private Integer expectedVersion;
    @Valid @NotNull private AssignmentInput primaryAssignment;
    @Valid @NotNull private List<AssignmentInput> additionalAssignments = new ArrayList<>();
    @Valid private ReportingInput reporting;
    @NotBlank @Size(max = 500) private String reason;
}
