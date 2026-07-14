package com.autodealer.crm.modules.sales.lead.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 线索关闭、恢复等生命周期命令请求。
 */
@Data
public class ClueLifecycleRequest {

    @NotBlank(message = "原因不能为空")
    private String reason;
}
