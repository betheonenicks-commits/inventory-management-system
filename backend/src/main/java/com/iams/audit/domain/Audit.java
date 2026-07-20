package com.iams.audit.domain;

import com.iams.asset.domain.AssetCategory;
import com.iams.common.domain.BaseEntity;
import com.iams.org.domain.OrgNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-AUD-01/03/04: an audit's type and scope. Scope is expressed as an
 * optional org-node subtree, an optional category filter, or both combined
 * (e.g. "Building B, IT Equipment") - {@link AuditExpectedAsset} resolves and
 * freezes the actual matching assets at creation time (US-AUD-04). An
 * explicit asset-list scope (the third option FR-AUD-01 names) is captured
 * the same way: the expected-asset snapshot is what matters after creation,
 * not how it was originally selected, so no separate scope-type discriminator
 * is stored. Actor references (submittedBy/approvedBy/nominalApproverId) are
 * plain UUID columns, not JPA relations - same convention as
 * Asset.assignedToPersonId - since nothing here needs to navigate back to
 * AppUser through JPA.
 */
@Getter
@Setter
@Entity
@Table(name = "audit")
public class Audit extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false)
    private AuditType auditType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_org_node_id")
    private OrgNode scopeOrgNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_category_id")
    private AssetCategory scopeCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status = AuditStatus.IN_PROGRESS;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "nominal_approver_id", nullable = false)
    private UUID nominalApproverId;

    /** US-AUD-22: the approver actually routed to for this submission - may differ from nominalApproverId after an SoD reroute. */
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "effective_approver_id")
    private UUID effectiveApproverId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** US-AUD-13: typed-name signature, captured alongside password re-authentication. */
    @Column(name = "signature_name")
    private String signatureName;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "last_rejection_reason", length = 500)
    private String lastRejectionReason;

    /**
     * US-DSH-05 (audit calendar): optional planned date, settable at creation.
     * Audits never had a scheduling concept before EPIC-DSH - they're created
     * straight into IN_PROGRESS - so this is deliberately informational only:
     * no state machine reads it, nothing blocks on it, the calendar widget
     * just plots it (V39).
     */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    // US-AUD-20: set only when the audit was created in statistical-sampling mode.
    // All null on a normal 100%-verification audit - sampling is never assumed.
    @Column(name = "sampling_confidence_level")
    private Integer samplingConfidenceLevel;

    @Column(name = "sampling_margin_of_error")
    private java.math.BigDecimal samplingMarginOfError;

    @Column(name = "sampling_population_size")
    private Integer samplingPopulationSize;
}
