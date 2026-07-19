package com.iams.org.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Minimal person record (FR-ORG-04), trimmed to what asset assignment
 * (FR-LIF-04) needs. No login is required to exist here - that is the point:
 * assets can be assigned to people who never touch the system directly.
 */
@Getter
@Setter
@Entity
@Table(name = "person")
public class Person extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false)
    private PersonType personType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_node_id")
    private OrgNode orgNode;

    /**
     * US-ORG-03 (AC-ORG-03-H): the person's department/cost centre - its own
     * dimension, independent of the physical org_node above. A plain id column
     * with an FK (same shape as Asset.assignedToDepartmentId), not a mapped
     * association, since nothing here navigates back to the Department.
     */
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** US-CMP-02: set once, on approval - the person's id is the stable pseudonym after this. */
    @Column(name = "anonymized_at")
    private Instant anonymizedAt;
}
