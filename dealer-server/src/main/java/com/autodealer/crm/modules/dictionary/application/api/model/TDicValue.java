package com.autodealer.crm.modules.dictionary.application.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 字典值表
 * t_dic_value
 */
@Data
public class TDicValue implements Serializable {

    /**
     * 主键，自动增长，字典值ID
     */
    private Integer id;

    /**
     * 字典类型代码
     */
    private String typeCode;

    /**
     * 字典值
     */
    private String typeValue;

    /**
     * 稳定业务编码
     */
    private String valueCode;

    /**
     * 排序号
     */
    private Integer order;

    /**
     * 适用模块
     */
    private String applicableModule;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否系统内置
     */
    private Boolean builtIn;

    /**
     * 停用原因
     */
    private String disableReason;

    /**
     * 停用操作人
     */
    private Integer disabledBy;

    /**
     * 停用时间
     */
    private LocalDateTime disabledTime;

    /**
     * 备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
