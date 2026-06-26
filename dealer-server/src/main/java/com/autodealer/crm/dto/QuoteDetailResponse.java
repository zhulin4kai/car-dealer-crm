package com.autodealer.crm.dto;

import com.autodealer.crm.model.TQuote;
import com.autodealer.crm.model.TQuoteVersion;
import com.autodealer.crm.model.TQuoteVersionItem;
import lombok.Data;

import java.util.List;

@Data
public class QuoteDetailResponse {
    private TQuote quote;
    private TQuoteVersion currentVersion;
    private List<TQuoteVersionItem> items;
}
