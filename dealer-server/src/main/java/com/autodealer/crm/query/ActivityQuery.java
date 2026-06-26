package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityQuery extends BaseQuery {

    private Integer id;

    private Integer ownerId;

    private String name;

    private String status;

    private String channel;

    private Date startTime;

    private Date endTime;

    private BigDecimal cost;

    private Date createTime;

    private String description;
}
