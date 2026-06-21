package com.autodealer.crm.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BaseQuery {

    @JsonIgnore
    private Integer dataScopeUserId;

    @lombok.Builder.Default
    private Integer current = 1;
    @lombok.Builder.Default
    private Integer pageSize = 10;
}
