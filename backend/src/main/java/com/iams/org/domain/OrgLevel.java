package com.iams.org.domain;

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
 * US-ORG-02: a renameable label for one depth of the org hierarchy (e.g.
 * "Campus" -> "Parish"). `code` and `rank` are fixed at seed time and never
 * exposed for editing - only `name` (the display label) is renameable.
 * `roomVariants` (US-ORG-06) is only meaningful on the Room-rank level; it's
 * a plain string list the same way Role.permissions is, since nothing needs
 * to query "which level has variant X" independently of a specific level.
 */
@Getter
@Setter
@Entity
@Table(name = "org_level")
public class OrgLevel extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private int rank;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "room_variants", nullable = false)
    private List<String> roomVariants = new ArrayList<>();
}
