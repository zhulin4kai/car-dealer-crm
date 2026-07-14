package com.autodealer.crm.modules.dictionary.application.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建字典值请求 DTO，只包含客户端可提交的字段。
 */
@Data
public class CreateDicValueRequest {

    @NotBlank(message = "字典类型代码不能为空")
    private String typeCode;

    @NotBlank(message = "字典值不能为空")
    private String typeValue;

    @NotBlank(message = "字典值业务代码不能为空")
    private String valueCode;

    @NotNull(message = "排序不能为空")
    private Integer order;

    @Size(max = 64, message = "适用模块长度不能超过64")
    private String applicableModule;

    private Boolean enabled;

    private String remark;
}
