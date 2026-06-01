package com.autodealer.crm.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 交易审批表
 * t_tran_approve
 */
@Data
public class TTranApprove implements Serializable {
    /**
     * 主键，自动增长，审批ID
     */
    private Integer id;

    /**
     * 交易ID
     */
    private Integer tranId;

    /**
     * 审批结果：1-通过，0-拒绝
     */
    private Boolean approveResult;

    /**
     * 审批意见
     */
    private String approveComment;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 审批人
     */
    private Integer approveBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    private static final long serialVersionUID = 1L;
} 