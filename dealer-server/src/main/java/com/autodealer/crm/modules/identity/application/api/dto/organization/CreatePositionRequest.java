package com.autodealer.crm.modules.identity.application.api.dto.organization;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePositionRequest {
    @NotBlank @Size(max = 64)
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*", message = "只能包含英文、数字、下划线和连字符")
    private String code;
    @NotBlank @Size(max = 64) private String name;
    @Size(max = 255) private String description;
    @NotNull @Min(0) private Integer positionLevel;
}
