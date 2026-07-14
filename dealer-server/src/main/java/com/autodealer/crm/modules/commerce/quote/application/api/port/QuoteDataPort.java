package com.autodealer.crm.modules.commerce.quote.application.api.port;

import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuote;
import com.autodealer.crm.modules.commerce.quote.application.api.query.QuoteQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuoteDataPort {
    List<TQuote> selectByQuery(@Param("query") QuoteQuery query,
                               @Param("dataScopeUserId") Integer dataScopeUserId);

    TQuote selectById(@Param("id") Long id);

    int countActiveByOpportunityId(@Param("opportunityId") Long opportunityId);

    int insert(TQuote quote);

    int updateCurrentVersion(@Param("id") Long id,
                             @Param("currentVersionId") Long currentVersionId,
                             @Param("updateTime") LocalDateTime updateTime,
                             @Param("updateBy") Integer updateBy);

    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("targetStatus") String targetStatus,
                              @Param("updateTime") LocalDateTime updateTime,
                              @Param("updateBy") Integer updateBy);
}
