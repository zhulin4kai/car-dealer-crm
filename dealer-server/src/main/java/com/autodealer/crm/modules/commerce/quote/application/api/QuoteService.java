package com.autodealer.crm.modules.commerce.quote.application.api;

import com.autodealer.crm.modules.commerce.quote.application.api.dto.CreateQuoteRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.CreateQuoteVersionRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.QuoteDetailResponse;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.UpdateQuoteStatusRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuote;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersion;
import com.autodealer.crm.modules.commerce.quote.application.api.query.QuoteQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface QuoteService {
    PageInfo<TQuote> getQuotePage(QuoteQuery query);

    QuoteDetailResponse getQuoteDetail(Long id);

    QuoteDetailResponse createQuote(CreateQuoteRequest request);

    QuoteDetailResponse createVersion(Long quoteId, CreateQuoteVersionRequest request);

    TQuote transitionStatus(Long quoteId, UpdateQuoteStatusRequest request);

    List<TQuoteVersion> getVersions(Long quoteId);
}
