package com.iams.compliance.api.dto;

import java.util.List;

public record DataResidencyResponse(boolean allStoresOnPremises, List<OutboundIntegrationFlagResponse> enabledOutboundFlows) {
}
