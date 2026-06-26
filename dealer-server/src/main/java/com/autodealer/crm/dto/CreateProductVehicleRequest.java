package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProductVehicleRequest {
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotBlank(message = "VIN不能为空")
    private String vin;

    @NotBlank(message = "车辆颜色不能为空")
    private String color;

    private String configuration;

    @NotBlank(message = "库位不能为空")
    private String location;

    @NotBlank(message = "入库备注不能为空")
    private String remark;
}
