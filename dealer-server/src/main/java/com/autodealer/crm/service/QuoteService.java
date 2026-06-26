package com.autodealer.crm.service;

import com.autodealer.crm.dto.CreateQuoteRequest;
import com.autodealer.crm.dto.CreateQuoteVersionRequest;
import com.autodealer.crm.dto.QuoteDetailResponse;
import com.autodealer.crm.dto.UpdateQuoteStatusRequest;
import com.autodealer.crm.model.TQuote;
import com.autodealer.crm.model.TQuoteVersion;
import com.autodealer.crm.query.QuoteQuery;
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
