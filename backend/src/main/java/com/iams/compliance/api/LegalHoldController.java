package com.iams.compliance.api;

import com.iams.compliance.api.dto.LegalHoldLiftRequest;
import com.iams.compliance.api.dto.LegalHoldPlaceRequest;
import com.iams.compliance.api.dto.LegalHoldResponse;
import com.iams.compliance.application.LegalHoldService;
import com.iams.compliance.domain.LegalHoldScopeType;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** US-CMP-06: place/lift a legal hold on an asset or audit record. */
@RestController
@RequestMapping("/api/v1/compliance/legal-holds")
public class LegalHoldController {

    private final LegalHoldService holdService;
    private final ComplianceMapper mapper;

    public LegalHoldController(LegalHoldService holdService, ComplianceMapper mapper) {
        this.holdService = holdService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('compliance:write')")
    public ResponseEntity<LegalHoldResponse> place(@Valid @RequestBody LegalHoldPlaceRequest request) {
        var hold = holdService.place(request.scopeType(), request.scopeId(), request.reason());
        return ResponseEntity.created(URI.create("/api/v1/compliance/legal-holds/" + hold.getId())).body(mapper.toResponse(hold));
    }

    @GetMapping
    @PreAuthorize("@perm.has('compliance:read')")
    public List<LegalHoldResponse> list(@RequestParam(required = false) LegalHoldScopeType scopeType) {
        return holdService.list(scopeType).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('compliance:read')")
    public LegalHoldResponse get(@PathVariable UUID id) {
        return mapper.toResponse(holdService.get(id));
    }

    @PostMapping("/{id}/lift")
    @PreAuthorize("@perm.has('compliance:write')")
    public LegalHoldResponse lift(@PathVariable UUID id, @Valid @RequestBody LegalHoldLiftRequest request) {
        return mapper.toResponse(holdService.lift(id, request.liftReason()));
    }
}
