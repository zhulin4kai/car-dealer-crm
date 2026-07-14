package com.autodealer.crm.modules.fulfillment.delivery.application.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateDeliveryRequest {
    @NotNull(message = "交易ID不能为空")
    private Integer tranId;

    @NotNull(message = "交付车辆不能为空")
    private Long vehicleId;

    @NotNull(message = "预计交付时间不能为空")
    private LocalDateTime plannedDeliveryTime;

    @Valid
    private List<DeliveryCheckItemRequest> checkItems;
}
