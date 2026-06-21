package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新商品分类请求 DTO。
 */
@Data
public class UpdateProductCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotBlank(message = "分类代码不能为空")
    private String code;

    private String description;

    private Integer sort;

    private String status;
}
