package com.autodealer.crm.model;

import com.autodealer.crm.enums.EmployeeStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工档案持久化对象，对应 t_employee。
 */
@Data
public class TEmployee implements Serializable {
    private Integer id;
    private Integer userId;
    private String employeeNo;
    private String name;
    private String phone;
    private String email;
    private String avatarUrl;
    private EmployeeStatus employmentStatus;
    private Boolean profileCompleted;
    private LocalDate hireDate;
    private LocalDate leaveDate;
    private Integer version;
    private Integer profileVersion;
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
