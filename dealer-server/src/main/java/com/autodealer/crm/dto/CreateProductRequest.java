package com.autodealer.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建商品请求 DTO，只包含客户端可提交的字段。
 */
@Data
public class CreateProductRequest {

    @NotBlank(message = "SKU不能为空")
    private String sku;

    @NotBlank(message = "产品名称不能为空")
    private String name;

    private Long categoryId;

    private String specification;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Min(value = 0, message = "最低库存不能为负数")
    private Integer minStock;

    @NotBlank(message = "商品状态不能为空")
    @Pattern(regexp = "ON_SALE|OFF_SALE", message = "商品状态必须使用ON_SALE或OFF_SALE")
    private String status;
}
