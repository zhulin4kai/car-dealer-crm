package com.autodealer.crm.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.autodealer.crm.enums.TranStage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TranQuery extends BaseQuery {
    /** 交易编号 */
    @Pattern(regexp = "^TN\\d{8}\\d+$", message = "交易编号格式不正确")
    private String tranNo;
    
    /** 客户ID */
    private Integer customerId;
    
    /** 客户名称 */
    private String customerName;
    
    /** 交易阶段 */
    private TranStage stage;
    
    /** 交易金额范围-最小值 */
    private BigDecimal minMoney;
    
    /** 交易金额范围-最大值 */
    private BigDecimal maxMoney;
    
    /** 预计成交日期-开始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedDateStart;
    
    /** 预计成交日期-结束 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedDateEnd;
    
    /** 创建时间-开始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeStart;
    
    /** 创建时间-结束 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;
    
    /** 创建人ID */
    private Integer createBy;
    
    /** 产品ID */
    private Long productId;
    
    /** 产品名称 */
    private String productName;
    
    /** 生产状态 */
    private String productionStatus;
    
    /** 发票状态 */
    private String invoiceStatus;

    @JsonIgnore
    private boolean transactionAllScope;

    @JsonIgnore
    private boolean transactionApprovalScope;

    @JsonIgnore
    private List<TranStage> transactionFinanceStages = Collections.emptyList();
}
