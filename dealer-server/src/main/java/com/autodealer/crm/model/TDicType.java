package com.autodealer.crm.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 字典类型表
 * t_dic_type
 */
@Data
public class TDicType implements Serializable {

    /**
     * 主键，自动增长，字典类型ID
     */
    private Integer id;

    /**
     * 字典类型代码
     */
    private String typeCode;

    /**
     * 字典类型名称
     */
    private String typeName;

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

    /**
     * 一对多关联
     */
    private List<TDicValue> dicValueList;

    private static final long serialVersionUID = 1L;
}
