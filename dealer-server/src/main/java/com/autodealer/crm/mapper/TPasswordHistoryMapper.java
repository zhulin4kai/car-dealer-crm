package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TPasswordHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TPasswordHistoryMapper {
    int insert(TPasswordHistory history);
    List<TPasswordHistory> selectRecent(@Param("userId") Integer userId, @Param("limit") int limit);
}
