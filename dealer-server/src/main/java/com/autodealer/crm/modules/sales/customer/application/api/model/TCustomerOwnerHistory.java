package com.autodealer.crm.modules.sales.customer.application.api.model;

import com.autodealer.crm.modules.identity.application.api.model.TUser;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class TCustomerOwnerHistory implements Serializable {

    private Integer id;

    private Integer customerId;

    private Integer fromOwnerId;

    private Integer toOwnerId;

    private String reason;

    private Integer operatorId;

    private Date transferTime;

    private TUser fromOwnerDO;

    private TUser toOwnerDO;

    private TUser operatorDO;

    private static final long serialVersionUID = 1L;
}
