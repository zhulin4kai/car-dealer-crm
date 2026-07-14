package com.autodealer.crm.modules.sales.activity.application.api.query;

import com.autodealer.crm.shared.pagination.BaseQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityRemarkQuery extends BaseQuery {

    private Integer id;

    private Integer activityId;

    private String noteContent;

}
