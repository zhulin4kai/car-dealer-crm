package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.CreateQuoteRequest;
import com.autodealer.crm.dto.CreateQuoteVersionRequest;
import com.autodealer.crm.dto.QuoteDetailResponse;
import com.autodealer.crm.dto.UpdateQuoteStatusRequest;
import com.autodealer.crm.model.TQuote;
import com.autodealer.crm.model.TQuoteVersion;
import com.autodealer.crm.query.QuoteQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.QuoteService;
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
    public R<PageInfo<TQuote>> list(QuoteQuery query) {
        return R.OK(quoteService.getQuotePage(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_VIEW + "')")
    public R<QuoteDetailResponse> detail(@PathVariable Long id) {
        return R.OK(quoteService.getQuoteDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_CREATE + "')")
    public R<QuoteDetailResponse> create(@Valid @RequestBody CreateQuoteRequest request) {
        return R.OK(quoteService.createQuote(request));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_EDIT + "')")
    public R<QuoteDetailResponse> createVersion(@PathVariable Long id,
                                                @Valid @RequestBody CreateQuoteVersionRequest request) {
        return R.OK(quoteService.createVersion(id, request));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_VIEW + "')")
    public R<List<TQuoteVersion>> versions(@PathVariable Long id) {
        return R.OK(quoteService.getVersions(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCodes.QUOTE_EDIT + "')")
    public R<TQuote> transitionStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateQuoteStatusRequest request) {
        return R.OK(quoteService.transitionStatus(id, request));
    }
}
