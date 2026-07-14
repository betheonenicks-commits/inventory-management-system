package com.iams.usr.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A role in the flat, non-inheriting model (FR-USR-01/02/07): each role's
 * permissions apply on their own, never implied by another role. The nine
 * default roles, the two system-provided custom roles (IT Security Officer,
 * Compliance Officer), and the non-human Integration Service role are seeded
 * as is_system = true; custom roles (US-USR-02) are created through the same
 * table with is_system = false.
 */
@Getter
@Setter
@Entity
@Table(name = "role_definition")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    /** Only a SUPER_ADMIN actor may assign a sensitive role (US-USR-01 exception AC). */
    @Column(name = "is_sensitive", nullable = false)
    private boolean sensitive;

    /** False only for INTEGRATION_SERVICE (FR-SEC-14: never assignable to a human user). */
    @Column(name = "is_assignable_to_humans", nullable = false)
    private boolean assignableToHumans = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> permissions = new ArrayList<>();
}
