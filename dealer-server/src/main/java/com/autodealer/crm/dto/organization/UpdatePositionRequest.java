package com.autodealer.crm.dto.organization;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePositionRequest {
    @NotBlank @Size(max = 64) private String name;
    @Size(max = 255) private String description;
    @NotNull @Min(0) private Integer positionLevel;
    @NotNull @Min(0) private Integer expectedVersion;
}
