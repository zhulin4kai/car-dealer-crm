package com.autodealer.crm.modules.identity.application.api.dto.organization;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeEntityStatusRequest {
    @NotNull @Min(0) private Integer expectedVersion;
    @NotBlank @Size(max = 500) private String reason;
}
