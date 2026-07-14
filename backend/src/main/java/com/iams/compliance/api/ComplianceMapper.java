package com.iams.compliance.api;

import com.iams.compliance.api.dto.AccessibilityAuditRecordResponse;
import com.iams.compliance.api.dto.DataResidencyResponse;
import com.iams.compliance.api.dto.LegalHoldResponse;
import com.iams.compliance.api.dto.OutboundIntegrationFlagResponse;
import com.iams.compliance.api.dto.PersonAnonymizationResponse;
import com.iams.compliance.api.dto.PrivacyNoticeConfigResponse;
import com.iams.compliance.api.dto.RetentionPolicyResponse;
import com.iams.compliance.application.DataResidencyView;
import com.iams.compliance.domain.AccessibilityAuditRecord;
import com.iams.compliance.domain.LegalHold;
import com.iams.compliance.domain.OutboundIntegrationFlag;
import com.iams.compliance.domain.PrivacyNoticeConfig;
import com.iams.compliance.domain.RetentionPolicy;
import com.iams.org.domain.Person;
import org.springframework.stereotype.Component;

@Component
public class ComplianceMapper {

    public RetentionPolicyResponse toResponse(RetentionPolicy policy) {
        return new RetentionPolicyResponse(policy.getId(), policy.getVersion(), policy.getEntityType(),
                policy.getRetentionPeriodDays(), policy.getExpiryAction());
    }

    public LegalHoldResponse toResponse(LegalHold hold) {
        return new LegalHoldResponse(hold.getId(), hold.getVersion(), hold.getScopeType(), hold.getScopeId(),
                hold.getReason(), hold.isActive(), hold.getLiftedBy(), hold.getLiftedAt(), hold.getLiftReason());
    }

    public PersonAnonymizationResponse toResponse(Person person) {
        return new PersonAnonymizationResponse(person.getId(), person.getFullName(), person.isActive(), person.getAnonymizedAt());
    }

    public PrivacyNoticeConfigResponse toResponse(PrivacyNoticeConfig config) {
        return new PrivacyNoticeConfigResponse(config.getId(), config.getVersion(), config.getFieldName(), config.getNoticeText());
    }

    public AccessibilityAuditRecordResponse toResponse(AccessibilityAuditRecord record) {
        return new AccessibilityAuditRecordResponse(record.getId(), record.getVersion(), record.getAuditDate(),
                record.getOutcome(), record.getNotes());
    }

    public OutboundIntegrationFlagResponse toResponse(OutboundIntegrationFlag flag) {
        return new OutboundIntegrationFlagResponse(flag.getId(), flag.getVersion(), flag.getName(), flag.isEnabled(),
                flag.getComplianceReviewNote());
    }

    public DataResidencyResponse toResponse(DataResidencyView view) {
        return new DataResidencyResponse(view.allStoresOnPremises(), view.enabledOutboundFlows().stream().map(this::toResponse).toList());
    }
}
