package com.bjpowernode.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductStockRecord {
    private Long id;
    private Long productId;
    private Integer quantity;
    private String type; // 入库/出库
    private String remark;
    private LocalDateTime createTime;
} 