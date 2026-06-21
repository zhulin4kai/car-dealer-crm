package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    private String remark;
}
