package com.autodealer.crm.modules.sales.lead.application.api.query;

import com.autodealer.crm.shared.pagination.BaseQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClueRemarkQuery extends BaseQuery {

    private Integer clueId;

    private String noteContent;

    private Integer noteWay;
}
