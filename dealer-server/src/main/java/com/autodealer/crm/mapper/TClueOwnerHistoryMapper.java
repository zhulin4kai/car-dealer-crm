package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TClueOwnerHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TClueOwnerHistoryMapper {

    int insert(TClueOwnerHistory record);

    List<TClueOwnerHistory> selectByClueId(@Param("clueId") Integer clueId);
}
