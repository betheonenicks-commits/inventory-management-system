package com.iams.search.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.Vendor;
import com.iams.inventory.domain.VendorRepository;
import com.iams.org.domain.Person;
import com.iams.org.application.PersonService;
import com.iams.usr.application.OrgScopeGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EPIC-SRC (BR-03). Global search (US-SRC-01) is one query fanned across the
 * entity types the caller may see, and each group is included only when the
 * caller holds the permission that group's own GET endpoint now requires -
 * so search can't be a side door around US-USR-05 (AC-USR-05-X): assets need
 * assets:read/assets:read:own (same gate as GET /assets), people need the
 * PII gate GET /persons now carries (org:read / assets:write / reports:read),
 * and vendors need inventory:read. A group the caller can't open in the UI
 * is omitted, not teased - a SYSTEM_OPERATOR searching gets an empty result,
 * not another route to asset valuations or person PII.
 * <p>
 * Code lookup (US-SRC-02) is exact-match across every unique identifying
 * code an asset carries - typed asset number, scanned barcode/QR payload, or
 * a future RFID tag (US-SRC-05) - and enforces org scope on the hit the same
 * way a direct GET /assets/{id} would: refused, not hidden.
 */
@Service
public class SearchService {

    /** A global search is a preview across modules, not a register - each group is capped. */
    private static final int GROUP_LIMIT = 20;

    private final AssetRepository assetRepository;
    private final VendorRepository vendorRepository;
    private final PersonService personService;
    private final OrgScopeGuard scopeGuard;
    private final CurrentUserProvider currentUserProvider;

    public SearchService(AssetRepository assetRepository, VendorRepository vendorRepository,
                          PersonService personService, OrgScopeGuard scopeGuard,
                          CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.vendorRepository = vendorRepository;
        this.personService = personService;
        this.scopeGuard = scopeGuard;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Rows are mapped to plain hit records INSIDE this transaction -
     * Person.orgNode is LAZY with no fetch join on its list query, so mapping
     * in the controller after the session closed would be the same
     * LazyInitializationException class this codebase has hit repeatedly.
     */
    @Transactional(readOnly = true)
    public GlobalSearchResult global(String q) {
        if (q == null || q.isBlank()) {
            throw ValidationFailedException.singleField("q", "A search term is required");
        }
        var caller = currentUserProvider.current();
        boolean assetsVisible = caller.hasPermission("assets:read") || caller.hasPermission("assets:read:own");
        List<AssetHit> assets = assetsVisible
                ? assetRepository.search(null, null, q, null, scopeGuard.currentScopePathPrefix(),
                        null, null, PageRequest.of(0, GROUP_LIMIT, Sort.by("name"))).getContent().stream()
                        .map(SearchService::toAssetHit).toList()
                : List.of();
        boolean vendorsVisible = caller.hasPermission("inventory:read");
        List<VendorHit> vendors = vendorsVisible
                ? vendorRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(q).stream()
                        .map(v -> new VendorHit(v.getId(), v.getName(), v.isActive())).toList()
                : List.of();
        boolean peopleVisible = caller.hasPermission("org:read") || caller.hasPermission("assets:write")
                || caller.hasPermission("reports:read");
        List<PersonHit> people = peopleVisible
                ? personService.list(q).stream().limit(GROUP_LIMIT)
                        .map(p -> new PersonHit(p.getId(), p.getFullName(),
                                p.getOrgNode() != null ? p.getOrgNode().getName() : null))
                        .toList()
                : List.of();
        return new GlobalSearchResult(assets, vendors, vendorsVisible, people);
    }

    @Transactional(readOnly = true)
    public AssetHit byCode(String code) {
        if (code == null || code.isBlank()) {
            throw ValidationFailedException.singleField("code", "A code is required");
        }
        Asset asset = assetRepository.findByAnyCodeWithAssociations(code.trim())
                .orElseThrow(() -> NotFoundException.of("Asset", code.trim()));
        UUID orgNodeId = asset.getOrgNode() != null ? asset.getOrgNode().getId() : null;
        scopeGuard.requireWithinScope(orgNodeId, "asset", asset.getId());
        return toAssetHit(asset);
    }

    private static AssetHit toAssetHit(Asset asset) {
        return new AssetHit(asset.getId(), asset.getAssetNumber(), asset.getName(),
                asset.getCategory().getName(), asset.getStatus().getLabel(), asset.getOrgNode().getName(),
                asset.getSerialNumber(), asset.getPurchaseDate());
    }

    /** vendorsSearched distinguishes "no vendor matched" from "caller may not see vendors at all". */
    public record GlobalSearchResult(List<AssetHit> assets, List<VendorHit> vendors, boolean vendorsSearched,
                                      List<PersonHit> people) {
    }

    public record AssetHit(UUID id, String assetNumber, String name, String categoryName, String statusLabel,
                            String orgNodeName, String serialNumber, java.time.LocalDate purchaseDate) {
    }

    public record VendorHit(UUID id, String name, boolean active) {
    }

    public record PersonHit(UUID id, String fullName, String orgNodeName) {
    }
}
