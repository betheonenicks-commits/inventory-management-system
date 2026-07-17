package com.iams.compliance.api.dto;

import java.util.List;

public record DataResidencyResponse(boolean allStoresOnPremises, List<StoreResponse> stores,
                                    List<OutboundIntegrationFlagResponse> enabledOutboundFlows) {

    public record StoreResponse(String name, String holds, boolean onPremises) {
    }
}
