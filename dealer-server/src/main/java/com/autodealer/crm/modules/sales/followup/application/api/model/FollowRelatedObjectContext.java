package com.autodealer.crm.modules.sales.followup.application.api.model;

import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowRelatedObjectType;

public record FollowRelatedObjectContext(
        FollowRelatedObjectType type,
        Long id,
        Integer ownerId,
        String displayName
) {
}
