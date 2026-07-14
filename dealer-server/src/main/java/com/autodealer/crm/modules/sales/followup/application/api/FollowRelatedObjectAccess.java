package com.autodealer.crm.modules.sales.followup.application.api;

import com.autodealer.crm.modules.sales.followup.application.api.model.FollowRelatedObjectContext;

public interface FollowRelatedObjectAccess {

    FollowRelatedObjectContext requireAccessible(String rawType, Long objectId);

    void validateAssignableOwner(Integer ownerId);
}
