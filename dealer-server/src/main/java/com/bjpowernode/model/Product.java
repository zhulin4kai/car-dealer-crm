package com.bjpowernode.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    /**
     * 商品类
     *
     * 该类表示商品的基本信息，包括商品的唯一标识、库存单位、名称、类别、规格、价格、库存量、最低库存量、状态以及创建和更新时间
     */
    private Long id;  // 商品的唯一标识符
    private String sku;  // 商品的库存单位
    private String name;  // 商品名称
    private String category;  // 商品类别
    private String specification;  // 商品规格
    private BigDecimal price;  // 商品价格
    private Integer stock;  // 当前商品库存量
    private Integer minStock;  // 商品的最低库存警戒值
    private String status;  // 商品状态，如上架、下架等
    private LocalDateTime createTime;  // 商品信息的创建时间
    private LocalDateTime updateTime;  // 商品信息的最后更新时间
}