package com.iams.compliance.application;

import com.iams.compliance.domain.OutboundIntegrationFlag;
import java.util.List;

/**
 * US-CMP-05 AC-H/X: on-premises confirmation plus any enabled outbound flow,
 * in one view. US-ANL-02 AC-2 extended it to NAME each store, so "analytics
 * data is confirmed on-premises alongside every other store" is a line a
 * Compliance Officer can read, not an inference.
 */
public record DataResidencyView(boolean allStoresOnPremises, List<StoreEntry> stores,
                                List<OutboundIntegrationFlag> enabledOutboundFlows) {

    /** One physical data store of the deployment and what lives in it. */
    public record StoreEntry(String name, String holds, boolean onPremises) {
    }
}
