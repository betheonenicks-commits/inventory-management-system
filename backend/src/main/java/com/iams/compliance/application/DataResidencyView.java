package com.iams.compliance.application;

import com.iams.compliance.domain.OutboundIntegrationFlag;
import java.util.List;

/** US-CMP-05 AC-H/X: on-premises confirmation plus any enabled outbound flow, in one view. */
public record DataResidencyView(boolean allStoresOnPremises, List<OutboundIntegrationFlag> enabledOutboundFlows) {
}
