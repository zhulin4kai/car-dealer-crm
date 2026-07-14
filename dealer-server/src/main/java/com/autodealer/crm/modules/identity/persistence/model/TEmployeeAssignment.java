package com.autodealer.crm.modules.identity.persistence.model;

import com.autodealer.crm.modules.identity.application.api.enums.AssignmentStatus;
import com.autodealer.crm.modules.identity.application.api.enums.AssignmentType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工任职事实持久化对象，对应 t_employee_assignment。
 */
@Data
public class TEmployeeAssignment implements Serializable {
    private Integer id;
    private Integer employeeId;
    private Integer organizationUnitId;
    private Integer positionId;
    private AssignmentType assignmentType;
    private AssignmentStatus status;
    private Boolean activePrimaryMarker;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String reason;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
