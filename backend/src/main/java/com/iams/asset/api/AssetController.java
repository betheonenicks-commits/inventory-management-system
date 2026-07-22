package com.iams.asset.api;

import com.iams.asset.api.dto.AssetCreateRequest;
import com.iams.asset.api.dto.AssetHistoryEventResponse;
import com.iams.asset.api.dto.AssetResponse;
import com.iams.asset.api.dto.AssetStatusChangeRequest;
import com.iams.asset.api.dto.AssetUpdateRequest;
import com.iams.asset.api.mapper.AssetMapper;
import com.iams.asset.application.AssetQueryService;
import com.iams.asset.application.AssetRegisterCommand;
import com.iams.asset.application.AssetRegistrationService;
import com.iams.asset.application.AssetStatusService;
import com.iams.asset.application.AssetUpdateCommand;
import com.iams.analytics.application.TrackUsage;
import com.iams.asset.domain.Asset;
import com.iams.common.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asset register endpoints (FR-AST-01/07/09/10, US-AST-01/07/09/10). Every
 * list/detail read is org-scoped (FR-USR-04) via AssetQueryService, which
 * resolves the acting user's scope through UserScopeResolver - exact-node
 * match only for now, since OrgNode has no parent/child hierarchy yet.
 */
@RestController
@RequestMapping("/api/v1")
public class AssetController {

    private final AssetRegistrationService registrationService;
    private final AssetQueryService queryService;
    private final AssetStatusService statusService;
    private final AssetMapper mapper;

    public AssetController(AssetRegistrationService registrationService,
                            AssetQueryService queryService,
                            AssetStatusService statusService,
                            AssetMapper mapper) {
        this.registrationService = registrationService;
        this.queryService = queryService;
        this.statusService = statusService;
        this.mapper = mapper;
    }

    @PostMapping("/assets")
    @PreAuthorize("@perm.has('assets:write')")
    @TrackUsage(module = "assets", action = "register")
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest request) {
        AssetRegisterCommand command = new AssetRegisterCommand(
                request.categoryId(), request.name(), request.manufacturer(), request.model(),
                request.serialNumber(), request.vendorName(), request.purchaseOrderReference(),
                request.purchaseDate(), request.purchaseCost(), request.orgNodeId(),
                request.warrantyStartDate(), request.warrantyEndDate(), request.rfidTagId(), request.customFields());
        Asset asset = registrationService.register(command);
        AssetResponse response = mapper.toResponse(asset);
        return ResponseEntity.created(URI.create("/api/v1/assets/" + asset.getId())).body(response);
    }

    // US-USR-05 (AC-USR-05-X): asset records carry valuation data (purchaseCost), so reading
    // them needs a real reason, not just authentication - OR-composed with assets:read:own so
    // whichever role ends up scoped to "my assigned assets only" isn't newly blocked. Deliberately
    // excludes SYSTEM_OPERATOR, which holds neither.
    @GetMapping("/assets")
    @PreAuthorize("@perm.has('assets:read') or @perm.has('assets:read:own')")
    @TrackUsage(module = "assets", action = "list-register")
    public PageResponse<AssetResponse> list(@RequestParam(required = false) UUID categoryId,
                                             @RequestParam(required = false) UUID statusId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) UUID orgNodeId,
                                             @RequestParam(required = false)
                                             @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                             java.time.LocalDate purchasedFrom,
                                             @RequestParam(required = false)
                                             @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                             java.time.LocalDate purchasedTo,
                                             // US-AST-06 (AC-AST-06-H): filter by a category-defined custom field value.
                                             @RequestParam(required = false) String customFieldKey,
                                             @RequestParam(required = false) String customFieldValue,
                                             @PageableDefault(size = 20) Pageable pageable) {
        var page = queryService.list(categoryId, statusId, q, orgNodeId, purchasedFrom, purchasedTo,
                customFieldKey, customFieldValue, pageable);
        List<AssetResponse> data = page.getContent().stream().map(mapper::toResponse).collect(Collectors.toList());
        List<String> sort = pageable.getSort().stream().map(o -> o.getProperty() + "," + o.getDirection()).toList();
        return new PageResponse<>(data, new PageResponse.PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()), sort);
    }

    @GetMapping("/assets/{id}")
    @PreAuthorize("@perm.has('assets:read') or @perm.has('assets:read:own')")
    public AssetResponse get(@PathVariable UUID id) {
        return mapper.toResponse(queryService.get(id));
    }

    @PatchMapping("/assets/{id}")
    @PreAuthorize("@perm.has('assets:write')")
    public AssetResponse update(@PathVariable UUID id, @Valid @RequestBody AssetUpdateRequest request) {
        AssetUpdateCommand command = new AssetUpdateCommand(
                request.version(), request.name(), request.manufacturer(), request.model(),
                request.serialNumber(), request.vendorName(), request.purchaseOrderReference(),
                request.purchaseDate(), request.purchaseCost(), request.orgNodeId(),
                request.warrantyStartDate(), request.warrantyEndDate(), request.rfidTagId(), request.customFields());
        return mapper.toResponse(registrationService.update(id, command));
    }

    @PatchMapping("/assets/{id}/status")
    @PreAuthorize("@perm.has('assets:write')")
    public AssetResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody AssetStatusChangeRequest request) {
        Asset asset = statusService.changeStatus(id, request.statusId(), request.version());
        return mapper.toResponse(asset);
    }

    @GetMapping("/assets/{id}/history")
    @PreAuthorize("@perm.has('assets:read') or @perm.has('assets:read:own')")
    public PageResponse<AssetHistoryEventResponse> history(@PathVariable UUID id,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        var page = queryService.history(id, pageable);
        List<AssetHistoryEventResponse> data = page.getContent().stream().map(mapper::toHistoryResponse).collect(Collectors.toList());
        return new PageResponse<>(data, new PageResponse.PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()), List.of("createdAt,DESC"));
    }

    @GetMapping("/assets/{id}/movements")
    @PreAuthorize("@perm.has('assets:read') or @perm.has('assets:read:own')")
    public PageResponse<AssetHistoryEventResponse> movements(@PathVariable UUID id,
                                                               @PageableDefault(size = 20) Pageable pageable) {
        var page = queryService.movements(id, pageable);
        List<AssetHistoryEventResponse> data = page.getContent().stream().map(mapper::toHistoryResponse).collect(Collectors.toList());
        return new PageResponse<>(data, new PageResponse.PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()), List.of("createdAt,DESC"));
    }
}
