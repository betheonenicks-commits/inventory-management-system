package com.iams.procurement.domain;

/** US-LIF-02/03: a PO is OPEN until every line is fully received or cancelled, then CLOSED; CANCELLED before any receipt. */
public enum PurchaseOrderStatus {
    OPEN,
    CLOSED,
    CANCELLED
}
