package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除交易请求。
 */
@Data
public class BatchDeleteTranRequest {

    @NotEmpty(message = "请选择要删除的交易")
    private List<Integer> ids;
}
