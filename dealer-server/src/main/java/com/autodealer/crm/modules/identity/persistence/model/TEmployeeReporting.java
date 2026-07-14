package com.autodealer.crm.modules.identity.persistence.model;

import com.autodealer.crm.modules.identity.application.api.enums.ReportingStatus;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工汇报关系持久化对象，对应 t_employee_reporting。
 */
@Data
public class TEmployeeReporting implements Serializable {
    private Integer id;
    private Integer subordinateEmployeeId;
    private Integer managerEmployeeId;
    private ReportingType relationType;
    private ReportingStatus status;
    private Boolean activeDirectMarker;
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
