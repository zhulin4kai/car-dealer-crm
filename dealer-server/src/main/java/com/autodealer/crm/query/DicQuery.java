package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DicQuery extends BaseQuery {
    private Integer id;
    private String typeCode;
    private String typeName;
    private String valueId;
    private String typeValue;
    private String valueCode;
    private Boolean enabled;
    private String applicableModule;
    private String text;
    private String orderNo;
    private String remark;

    // 分页参数
    private Integer page;
    private Integer size;
}
