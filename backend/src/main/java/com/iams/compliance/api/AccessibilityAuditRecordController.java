package com.iams.compliance.api;

import com.iams.compliance.api.dto.AccessibilityAuditRecordRequest;
import com.iams.compliance.api.dto.AccessibilityAuditRecordResponse;
import com.iams.compliance.application.AccessibilityAuditRecordService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-CMP-04: date/outcome of the latest WCAG 2.1 AA audit. */
@RestController
@RequestMapping("/api/v1/compliance/accessibility-audit")
public class AccessibilityAuditRecordController {

    private final AccessibilityAuditRecordService recordService;
    private final ComplianceMapper mapper;

    public AccessibilityAuditRecordController(AccessibilityAuditRecordService recordService, ComplianceMapper mapper) {
        this.recordService = recordService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@perm.has('compliance:read')")
    public AccessibilityAuditRecordResponse current() {
        // AC-CMP-04-X: "states that plainly rather than implying compliance."
        return recordService.current().map(mapper::toResponse).orElseGet(AccessibilityAuditRecordResponse::notYetRecorded);
    }

    @PutMapping
    @PreAuthorize("@perm.has('compliance:write')")
    public AccessibilityAuditRecordResponse record(@Valid @RequestBody AccessibilityAuditRecordRequest request) {
        return mapper.toResponse(recordService.record(request.auditDate(), request.outcome(), request.notes()));
    }
}
