package com.autodealer.crm.modules.sales.customer.application.api.dto;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Date;

/**
 * 线索转客户请求，操作者由服务端 CurrentUserProvider 提供，
 * 禁止客户端提交 createBy。
 */
@Data
public class ConvertCustomerRequest {

    @NotNull(message = "线索ID不能为空")
    private Integer clueId;

    private Long product;

    @Positive(message = "购买数量必须大于0")
    private Integer quantity = 1;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date nextContactTime;
}
