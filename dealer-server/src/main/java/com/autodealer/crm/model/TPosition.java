package com.autodealer.crm.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位目录持久化对象，对应 t_position。
 */
@Data
public class TPosition implements Serializable {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private Integer positionLevel;
    private Boolean builtIn;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
