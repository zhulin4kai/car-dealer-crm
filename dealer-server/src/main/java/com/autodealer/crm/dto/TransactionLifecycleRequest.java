package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 交易履约终止类命令请求。
 */
@Data
public class TransactionLifecycleRequest {

    @NotBlank(message = "原因不能为空")
    private String reason;
}
