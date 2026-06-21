package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统状态开关命令，isopen 取值为 "Y" 或 "N"。
 */
@Data
public class ToggleSystemStatusRequest {

    @NotBlank(message = "开关状态不能为空")
    private String isopen;
}
