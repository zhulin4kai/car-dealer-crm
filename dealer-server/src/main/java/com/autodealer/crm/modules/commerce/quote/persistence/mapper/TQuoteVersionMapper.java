package com.autodealer.crm.modules.commerce.quote.persistence.mapper;

import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TQuoteVersionMapper {
    TQuoteVersion selectById(@Param("id") Long id);

    List<TQuoteVersion> selectByQuoteId(@Param("quoteId") Long quoteId);

    Integer selectMaxVersionNo(@Param("quoteId") Long quoteId);

    int insert(TQuoteVersion version);

    int updateDraftVersion(TQuoteVersion version);
}
