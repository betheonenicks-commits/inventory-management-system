package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** US-LIF-04: assign an asset's custodianship to a Department. */
public record AssetDepartmentAssignmentRequest(
        @NotNull UUID departmentId,
        @NotNull Long version
) {
}
