package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignUserRolesRequest {

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    private List<Integer> roleIds;
}
