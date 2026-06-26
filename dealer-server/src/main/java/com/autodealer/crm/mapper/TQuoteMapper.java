package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TQuote;
import com.autodealer.crm.query.QuoteQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TQuoteMapper {
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
