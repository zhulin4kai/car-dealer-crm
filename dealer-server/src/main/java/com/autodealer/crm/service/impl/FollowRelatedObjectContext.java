package com.autodealer.crm.service.impl;

import com.autodealer.crm.enums.FollowRelatedObjectType;

public record FollowRelatedObjectContext(
        FollowRelatedObjectType type,
        Long id,
        Integer ownerId,
        String displayName
) {
}
