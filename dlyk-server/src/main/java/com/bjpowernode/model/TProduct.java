package com.bjpowernode.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 产品表
 * t_product
 */
@Data
public class TProduct implements Serializable {
    /**
     * 主键，自动增长，线索ID
     */
    private Integer id;

    /**
     * 产品名称
     */
    private String name;

    /**
     * 官方指导起始价
     */
    private BigDecimal guidePriceS;

    /**
     * 官方指导最高价
     */
    private BigDecimal guidePriceE;

    /**
     * 经销商报价
     */
    private BigDecimal quotation;

    /**
     * 状态 0在售 1售罄
     */
    private Integer state;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getGuidePriceS() {
        return guidePriceS;
    }

    public void setGuidePriceS(BigDecimal guidePriceS) {
        this.guidePriceS = guidePriceS;
    }

    public BigDecimal getGuidePriceE() {
        return guidePriceE;
    }

    public void setGuidePriceE(BigDecimal guidePriceE) {
        this.guidePriceE = guidePriceE;
    }

    public BigDecimal getQuotation() {
        return quotation;
    }

    public void setQuotation(BigDecimal quotation) {
        this.quotation = quotation;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Integer createBy) {
        this.createBy = createBy;
    }

    public Date getEditTime() {
        return editTime;
    }

    public void setEditTime(Date editTime) {
        this.editTime = editTime;
    }

    public Integer getEditBy() {
        return editBy;
    }

    public void setEditBy(Integer editBy) {
        this.editBy = editBy;
    }
}