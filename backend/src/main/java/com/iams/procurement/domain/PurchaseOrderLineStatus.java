package com.iams.procurement.domain;

/** US-LIF-03/16: per-line receiving state - independent of sibling lines on the same PO. */
public enum PurchaseOrderLineStatus {
    OPEN,
    PARTIALLY_RECEIVED,
    FULLY_RECEIVED,
    CANCELLED
}
