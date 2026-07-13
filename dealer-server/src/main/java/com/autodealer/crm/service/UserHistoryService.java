package com.autodealer.crm.service;

import com.autodealer.crm.dto.user.UserHistoryDtos.Collection;
import com.autodealer.crm.dto.user.UserHistoryDtos.Query;

public interface UserHistoryService {
    Collection getUserHistory(Integer userId, Query query);
}
