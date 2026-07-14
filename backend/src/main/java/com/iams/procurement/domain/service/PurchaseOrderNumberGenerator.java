package com.iams.procurement.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Year;
import org.springframework.stereotype.Component;

/** Generates the system-assigned PO-YYYY-NNNNNN identifier (US-LIF-02), same shape as AssetNumberGenerator. */
@Component
public class PurchaseOrderNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public String next() {
        Number nextVal = (Number) entityManager
                .createNativeQuery("SELECT nextval('purchase_order_number_seq')")
                .getSingleResult();
        int year = Year.now().getValue();
        return "PO-%d-%06d".formatted(year, nextVal.longValue());
    }
}
