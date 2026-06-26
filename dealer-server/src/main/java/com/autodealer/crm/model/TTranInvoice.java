package com.autodealer.crm.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易发票表
 * t_tran_invoice
 */
@Data
public class TTranInvoice implements Serializable {
    /**
     * 主键，自动增长，发票ID
     */
    private Integer id;

    /**
     * 交易ID
     */
    private Integer tranId;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 发票类型
     */
    private String type;

    /**
     * 发票抬头
     */
    private String title;

    /**
     * 税号
     */
    private String taxNumber;

    /**
     * 开户行
     */
    private String bankName;

    /**
     * 银行账号
     */
    private String bankAccount;

    /**
     * 地址
     */
    private String address;

    /**
     * 电话
     */
    private String phone;

    /**
     * 原发票ID，红冲或重开时关联原票/红冲记录
     */
    private Integer originalInvoiceId;

    /**
     * 发票金额
     */
    private BigDecimal amount;

    /**
     * 发票状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 开票时间
     */
    private Date issueTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 编辑人
     */
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
