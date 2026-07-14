package com.autodealer.crm.modules.commerce.quote.application.api.dto;

import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuote;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersion;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersionItem;
import lombok.Data;

import java.util.List;

@Data
public class QuoteDetailResponse {
    private TQuote quote;
    private TQuoteVersion currentVersion;
    private List<TQuoteVersionItem> items;
}
