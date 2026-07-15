package com.iams.search.api;

import com.iams.search.application.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EPIC-SRC. No @PreAuthorize on either endpoint, by the same precedent as
 * GET /assets and GET /persons (both deliberately open to any authenticated
 * user - see the permission-matrix session): everything returned here is
 * already reachable through those ungated reads, org-scoped per caller, and
 * the vendor group is permission-filtered inside SearchService.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchService.GlobalSearchResult global(@RequestParam String q) {
        return searchService.global(q);
    }

    /** US-SRC-02: exact lookup by asset number / barcode / QR payload / RFID tag. A 404 lets the UI offer "register this asset?" to authorized roles. */
    @GetMapping("/asset-code/{code}")
    public SearchService.AssetHit byCode(@PathVariable String code) {
        return searchService.byCode(code);
    }
}
