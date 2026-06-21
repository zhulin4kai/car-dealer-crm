package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClueRemarkQuery extends BaseQuery {

    private Integer clueId;

    private String noteContent;

    private Integer noteWay;
}
