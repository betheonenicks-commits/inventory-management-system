package com.iams.compliance.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * US-CMP-04: the date and outcome of the latest WCAG 2.1 AA audit. Single-row
 * table, same convention as {@code PasswordPolicy} - no dedicated
 * singleton-lookup repository method, callers use findAll().stream().findFirst().
 */
@Getter
@Setter
@Entity
@Table(name = "accessibility_audit_record")
public class AccessibilityAuditRecord extends BaseEntity {

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessibilityAuditOutcome outcome;

    @Column(length = 1000)
    private String notes;
}
