package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
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

    private String remark;
}
