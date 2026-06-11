package com.autodealer.crm.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易历史表 (阶段变更审计)
 * t_tran_history
 */
@Data
public class TTranHistory implements Serializable {
    private Integer id;
    private Integer tranId;
    private String stage;
    private BigDecimal money;
    private Date expectedDate;
    private Date createTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
