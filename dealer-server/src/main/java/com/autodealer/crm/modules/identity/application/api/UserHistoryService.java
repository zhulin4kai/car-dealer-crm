package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryDtos.Collection;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryDtos.Query;

public interface UserHistoryService {
    Collection getUserHistory(Integer userId, Query query);
}
