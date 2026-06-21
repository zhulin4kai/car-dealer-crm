package com.autodealer.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新交易请求。
 */
@Data
public class UpdateTranRequest {

    @NotNull(message = "交易ID不能为空")
    private Integer id;

    private Integer customerId;

    @Valid
    @Size(min = 1, message = "交易商品列表不能为空")
    private List<TranProductItemRequest> products;

    private String description;

    private LocalDateTime expectedDeliveryDate;
}
