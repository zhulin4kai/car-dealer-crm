package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新字典值请求 DTO。
 */
@Data
public class UpdateDicValueRequest {

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

    @Size(max = 255, message = "停用原因长度不能超过255")
    private String disableReason;

    private String remark;
}
