package com.autodealer.crm.modules.identity.application.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDisableUsersRequest {

    @NotEmpty(message = "用户ID列表不能为空")
    private List<Integer> ids;
}
