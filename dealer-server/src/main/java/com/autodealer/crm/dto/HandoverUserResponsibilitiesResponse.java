package com.autodealer.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandoverUserResponsibilitiesResponse {

    private Integer sourceUserId;

    private Integer targetUserId;

    private Integer activityCount;

    private Integer clueCount;

    private Integer customerCount;
}
