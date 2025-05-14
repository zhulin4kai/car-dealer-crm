package com.bjpowernode.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String sku;
    private String name;
    private String category;
    private String specification;
    private BigDecimal price;
    private Integer stock;
    private Integer minStock;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}