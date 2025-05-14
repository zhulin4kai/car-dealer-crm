package com.bjpowernode.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductCategory {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer sort;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 