package com.iams.audit.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AuditBatchScanRequest(
        @NotEmpty @Valid List<AuditScanRequest> scans
) {
}
