package com.bjpowernode.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class Product {
    /**
     * 商品类
     *
     * 该类表示商品的基本信息，包括商品的唯一标识、库存单位、名称、类别、规格、价格、库存量、最低库存量、状态以及创建和更新时间
     */
    private Long id;  // 商品的唯一标识符
    
    @NotBlank(message = "SKU不能为空")
    private String sku;  // 商品的库存单位
    
    @NotBlank(message = "产品名称不能为空")
    private String name;  // 商品名称
    
    private String category;  // 商品类别
    private String specification;  // 商品规格
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;  // 商品价格
    
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;  // 当前商品库存量
    
    @Min(value = 0, message = "最低库存不能为负数")
    private Integer minStock;  // 商品的最低库存警戒值
    
    private String status;  // 商品状态，如上架、下架等
    private LocalDateTime createTime;  // 商品信息的创建时间
    private LocalDateTime updateTime;  // 商品信息的最后更新时间
}