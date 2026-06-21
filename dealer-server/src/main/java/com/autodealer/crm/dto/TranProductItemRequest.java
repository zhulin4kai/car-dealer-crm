package com.autodealer.crm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 交易产品项请求，用于创建/更新交易时提交的产品明细。
 */
@Data
public class TranProductItemRequest {

    @NotNull(message = "产品ID不能为空")
    private Integer productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
}
