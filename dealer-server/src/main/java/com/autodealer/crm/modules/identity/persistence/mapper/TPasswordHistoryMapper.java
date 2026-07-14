package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TPasswordHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TPasswordHistoryMapper {
    int insert(TPasswordHistory history);
    List<TPasswordHistory> selectRecent(@Param("userId") Integer userId, @Param("limit") int limit);
}
