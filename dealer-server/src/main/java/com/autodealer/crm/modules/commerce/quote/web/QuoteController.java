package com.autodealer.crm.modules.commerce.quote.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.CreateQuoteRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.CreateQuoteVersionRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.QuoteDetailResponse;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.UpdateQuoteStatusRequest;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuote;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersion;
import com.autodealer.crm.modules.commerce.quote.application.api.query.QuoteQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.commerce.quote.application.api.QuoteService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_LIST + "')")
    public Result<PageInfo<TQuote>> list(QuoteQuery query) {
        return Result.OK(quoteService.getQuotePage(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_VIEW + "')")
    public Result<QuoteDetailResponse> detail(@PathVariable Long id) {
        return Result.OK(quoteService.getQuoteDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_CREATE + "')")
    public Result<QuoteDetailResponse> create(@Valid @RequestBody CreateQuoteRequest request) {
        return Result.OK(quoteService.createQuote(request));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_EDIT + "')")
    public Result<QuoteDetailResponse> createVersion(@PathVariable Long id,
                                                @Valid @RequestBody CreateQuoteVersionRequest request) {
        return Result.OK(quoteService.createVersion(id, request));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_VIEW + "')")
    public Result<List<TQuoteVersion>> versions(@PathVariable Long id) {
        return Result.OK(quoteService.getVersions(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_EDIT + "')")
    public Result<TQuote> transitionStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateQuoteStatusRequest request) {
        return Result.OK(quoteService.transitionStatus(id, request));
    }
}
