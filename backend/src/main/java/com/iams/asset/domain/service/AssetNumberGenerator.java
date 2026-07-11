package com.iams.asset.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Year;
import org.springframework.stereotype.Component;

/**
 * Generates the system-assigned AST-YYYY-NNNNNN identifier (FR-AST-01). The
 * QR payload is this same value, per FR-AST-01 - never a separately allocated
 * code. Allocated inside the same transaction as the asset insert; a rolled-
 * back registration burns a sequence value, which is harmless since
 * uniqueness (not density) is the only real requirement on asset_number.
 */
@Component
public class AssetNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public String next() {
        Number nextVal = (Number) entityManager
                .createNativeQuery("SELECT nextval('asset_number_seq')")
                .getSingleResult();
        int year = Year.now().getValue();
        return "AST-%d-%06d".formatted(year, nextVal.longValue());
    }
}
