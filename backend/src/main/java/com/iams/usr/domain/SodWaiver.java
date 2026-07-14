package com.iams.usr.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * FR-USR-09: a recorded Separation-of-Duties waiver, e.g. for a single-admin
 * site that can't fully separate a control. `scope` names which conflict
 * class is waived (e.g. "AUDIT_APPROVAL") - free-form, not an enum, since
 * more scopes arrive with epics that don't exist yet. `signedOffBy` must
 * hold the IT_SECURITY_OFFICER role and can never be the same user who
 * recorded the waiver (SodWaiverService enforces both - never self-asserted,
 * per this story's exception AC).
 */
@Getter
@Setter
@Entity
@Table(name = "sod_waiver")
public class SodWaiver extends BaseEntity {

    @Column(nullable = false)
    private String scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_off_by", nullable = false)
    private AppUser signedOffBy;

    @Column(nullable = false)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
