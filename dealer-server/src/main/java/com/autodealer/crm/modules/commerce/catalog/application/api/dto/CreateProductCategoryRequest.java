package com.autodealer.crm.modules.commerce.catalog.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建商品分类请求 DTO。
 */
@Data
public class CreateProductCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotBlank(message = "分类代码不能为空")
    private String code;

    private String description;

    private Integer sort;

    private String status;
}
