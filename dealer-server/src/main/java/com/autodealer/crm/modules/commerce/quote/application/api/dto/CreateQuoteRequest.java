package com.autodealer.crm.modules.commerce.quote.application.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateQuoteRequest {
    @NotNull(message = "客户ID不能为空")
    private Integer customerId;

    private Long opportunityId;

    @NotNull(message = "报价有效期不能为空")
    private LocalDateTime validUntil;

    private String remark;

    @NotEmpty(message = "报价商品不能为空")
    @Valid
    private List<QuoteItemRequest> items;
}
