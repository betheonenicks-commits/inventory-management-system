package com.iams.compliance.api;

import com.iams.compliance.api.dto.EntityPurgeResultResponse;
import com.iams.compliance.api.dto.PurgeResultResponse;
import com.iams.compliance.api.dto.RetentionPolicyRequest;
import com.iams.compliance.api.dto.RetentionPolicyResponse;
import com.iams.compliance.application.RetentionPolicyService;
import com.iams.compliance.domain.RetentionEntityType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-CMP-01: per-entity-type retention policy + manual purge trigger. */
@RestController
@RequestMapping("/api/v1/compliance/retention-policies")
public class RetentionPolicyController {

    private final RetentionPolicyService policyService;
    private final ComplianceMapper mapper;

    public RetentionPolicyController(RetentionPolicyService policyService, ComplianceMapper mapper) {
        this.policyService = policyService;
        this.mapper = mapper;
    }

    @PutMapping
    @PreAuthorize("@perm.has('compliance:write')")
    public RetentionPolicyResponse save(@Valid @RequestBody RetentionPolicyRequest request) {
        return mapper.toResponse(policyService.save(request.entityType(), request.retentionPeriodDays(), request.expiryAction()));
    }

    @GetMapping
    @PreAuthorize("@perm.has('compliance:read')")
    public List<RetentionPolicyResponse> list() {
        return policyService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('compliance:read')")
    public RetentionPolicyResponse get(@PathVariable UUID id) {
        return mapper.toResponse(policyService.get(id));
    }

    @PostMapping("/security-event-log/purge")
    @PreAuthorize("@perm.has('compliance:write')")
    public PurgeResultResponse purgeSecurityEventLog() {
        return new PurgeResultResponse(policyService.runPurge());
    }

    /**
     * US-CMP-01: run the configured purge for any executable entity type (SECURITY_EVENT_LOG or
     * PERSON today). US-CMP-06: for PERSON, records under an active legal hold are reported in
     * {@code skipped}, not deleted/anonymized.
     */
    @PostMapping("/{entityType}/purge")
    @PreAuthorize("@perm.has('compliance:write')")
    public EntityPurgeResultResponse purge(@PathVariable RetentionEntityType entityType) {
        return EntityPurgeResultResponse.from(policyService.purge(entityType));
    }
}
