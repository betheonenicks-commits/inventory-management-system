package com.iams.audit.domain;

import com.iams.asset.domain.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-AUD-04: the expected-asset set frozen at audit creation. Deliberately
 * NOT BaseEntity, same reasoning as AssetHistoryEvent - this is a snapshot
 * row, never updated, so no version/updated_* columns exist to update. An
 * asset added to the org node after the audit starts is never added here
 * (AC-AUD-04-X); this table is the audit's own record of what to look for.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_expected_asset")
public class AuditExpectedAsset {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false, updatable = false)
    private Audit audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private Asset asset;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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
