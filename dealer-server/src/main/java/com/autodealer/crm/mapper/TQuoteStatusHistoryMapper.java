package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TQuoteStatusHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TQuoteStatusHistoryMapper {
    int insert(TQuoteStatusHistory history);
}
