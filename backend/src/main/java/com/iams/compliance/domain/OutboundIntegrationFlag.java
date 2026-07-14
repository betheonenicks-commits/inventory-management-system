package com.iams.compliance.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * US-CMP-05: a named outbound data flow (e.g. "ACCOUNTING_EXPORT") a
 * Compliance Officer has registered and can flag as enabled, with a
 * compliance-review note - EPIC-INT itself (the integrations that would
 * actually run these flows) doesn't exist yet in this codebase, so this is
 * the compliance-side registry the data-residency view reads from, not a
 * live integration switch.
 */
@Getter
@Setter
@Entity
@Table(name = "outbound_integration_flag")
public class OutboundIntegrationFlag extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "compliance_review_note", length = 1000)
    private String complianceReviewNote;
}
