package com.autodealer.crm.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TProductCategory {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer sort;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 