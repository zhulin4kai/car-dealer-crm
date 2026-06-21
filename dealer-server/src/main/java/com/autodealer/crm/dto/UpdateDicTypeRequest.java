package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新字典类型请求 DTO。
 */
@Data
public class UpdateDicTypeRequest {

    @NotBlank(message = "类型代码不能为空")
    private String typeCode;

    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    private String remark;
}
