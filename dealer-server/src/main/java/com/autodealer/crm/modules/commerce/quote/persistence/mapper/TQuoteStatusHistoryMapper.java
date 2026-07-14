package com.autodealer.crm.modules.commerce.quote.persistence.mapper;

import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteStatusHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TQuoteStatusHistoryMapper {
    int insert(TQuoteStatusHistory history);
}
