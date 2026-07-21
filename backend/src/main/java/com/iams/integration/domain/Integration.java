package com.iams.integration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-SEC-15 / FR-INT-05: a registered external integration. The single security-critical
 * invariant is {@code credentialRef}: it holds ONLY a secrets-manager reference (see
 * {@link SecretReferences}), never a plaintext secret - so a leaked DB dump or config file
 * hands over a pointer, not a live credential (AC-SEC-15-H). {@code config} carries the
 * integration's non-secret settings (a URL, a schedule); it is validated to contain no
 * inline secret either. Every integration is {@code enabled = false} until deliberately
 * turned on (FR-INT-05 "disabled by default").
 */
@Getter
@Setter
@Entity
@Table(name = "integration")
public class Integration {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationType type;

    @Column(length = 500)
    private String description;

    /** US-SEC-15: a secrets-manager reference, NEVER a plaintext secret. Null until configured. */
    @Column(name = "credential_ref")
    private String credentialRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false)
    private Map<String, String> config = new LinkedHashMap<>();

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
