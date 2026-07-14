package com.autodealer.crm.modules.commerce.inventory.application.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品入库请求 DTO，统一收口入库命令参数与校验。
 */
@Data
public class RestockRequest {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须为正数")
    private Integer quantity;

    @NotBlank(message = "入库备注不能为空")
    private String remark;
}
