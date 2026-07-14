package com.iams.sec.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * US-SEC-05: a single, org-wide configurable password policy. Single-row
 * table - PasswordPolicyRepository has no findTheOne(); callers use
 * findAll().stream().findFirst(), same as any other Spring Data repository,
 * since a dedicated singleton-lookup method would only wrap one line.
 */
@Getter
@Setter
@Entity
@Table(name = "password_policy")
public class PasswordPolicy extends BaseEntity {

    @Column(name = "min_length", nullable = false)
    private int minLength;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase;

    @Column(name = "require_digit", nullable = false)
    private boolean requireDigit;

    @Column(name = "require_special", nullable = false)
    private boolean requireSpecial;
}
