package com.autodealer.crm.modules.sales.lead.application.api.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TClueOwnerHistory implements Serializable {

    private Integer id;

    private Integer clueId;

    private Integer fromOwnerId;

    private Integer toOwnerId;

    private Integer assignedBy;

    private String reason;

    private Date assignedTime;

    private String fromOwnerName;

    private String toOwnerName;

    private String assignedByName;

    private static final long serialVersionUID = 1L;
}
