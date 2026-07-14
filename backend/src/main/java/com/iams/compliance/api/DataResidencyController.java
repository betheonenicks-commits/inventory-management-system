package com.iams.compliance.api;

import com.iams.compliance.api.dto.DataResidencyResponse;
import com.iams.compliance.api.dto.OutboundIntegrationFlagRequest;
import com.iams.compliance.api.dto.OutboundIntegrationFlagResponse;
import com.iams.compliance.application.DataResidencyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-CMP-05: single view confirming on-premises data residency and flagging enabled outbound flows. */
@RestController
@RequestMapping("/api/v1/compliance/data-residency")
public class DataResidencyController {

    private final DataResidencyService residencyService;
    private final ComplianceMapper mapper;

    public DataResidencyController(DataResidencyService residencyService, ComplianceMapper mapper) {
        this.residencyService = residencyService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@perm.has('compliance:read')")
    public DataResidencyResponse view() {
        return mapper.toResponse(residencyService.view());
    }

    @GetMapping("/outbound-flows")
    @PreAuthorize("@perm.has('compliance:read')")
    public List<OutboundIntegrationFlagResponse> listFlows() {
        return residencyService.list().stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/outbound-flows")
    @PreAuthorize("@perm.has('compliance:write')")
    public OutboundIntegrationFlagResponse saveFlow(@Valid @RequestBody OutboundIntegrationFlagRequest request) {
        return mapper.toResponse(residencyService.save(request.name(), request.enabled(), request.complianceReviewNote()));
    }

    @DeleteMapping("/outbound-flows/{id}")
    @PreAuthorize("@perm.has('compliance:write')")
    public void deleteFlow(@PathVariable UUID id) {
        residencyService.delete(id);
    }
}
