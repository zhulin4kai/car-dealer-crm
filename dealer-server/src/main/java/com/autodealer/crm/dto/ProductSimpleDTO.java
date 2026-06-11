package com.autodealer.crm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 产品简要信息DTO（用于线索/客户中展示意向产品、Excel导入等场景）
 */
@Data
public class ProductSimpleDTO {
    private Integer id;
    private String name;
    private BigDecimal guidePriceS;
    private BigDecimal guidePriceE;
    private BigDecimal quotation;
    private Integer state;
    private Date createTime;
    private Date editTime;
}
