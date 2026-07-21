package com.iams.sec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-SEC-14 / US-SEC-15: a non-human integration credential. Authentication is by
 * an API key of which only the SHA-256 hash is ever stored ({@code apiKeyHash}) -
 * the raw key exists once, in the creation response. {@code scopes} is the account's
 * entire capability: it holds no human permissions, so it is refused every normal
 * endpoint and may reach only the integration endpoints explicitly opened to a scope
 * it carries. The non-secret {@code apiKeyPrefix} lets an admin recognise a key in a
 * listing without exposing it.
 */
@Getter
@Setter
@Entity
@Table(name = "service_account")
public class ServiceAccount {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "api_key_hash", nullable = false, unique = true)
    private String apiKeyHash;

    @Column(name = "api_key_prefix", nullable = false)
    private String apiKeyPrefix;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Set<String> scopes = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

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
