package com.autodealer.crm.modules.dictionary.application.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建字典类型请求 DTO，只包含客户端可提交的字段。
 */
@Data
public class CreateDicTypeRequest {

    @NotBlank(message = "类型代码不能为空")
    private String typeCode;

    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    @Size(max = 64, message = "适用模块长度不能超过64")
    private String applicableModule;

    private Boolean enabled;

    private String remark;
}
