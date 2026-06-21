package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityRemarkQuery extends BaseQuery {

    private Integer id;

    private Integer activityId;

    private String noteContent;

}
