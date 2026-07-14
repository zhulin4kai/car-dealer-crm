package com.autodealer.crm.modules.commerce.quote.persistence.mapper;

import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TQuoteVersionItemMapper {
    List<TQuoteVersionItem> selectByVersionId(@Param("quoteVersionId") Long quoteVersionId);

    int insert(TQuoteVersionItem item);

    int deleteByVersionId(@Param("quoteVersionId") Long quoteVersionId);
}
