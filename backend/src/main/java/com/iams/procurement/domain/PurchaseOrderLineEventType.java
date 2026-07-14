package com.iams.procurement.domain;

/** US-LIF-16: the three immutable-record-producing actions a line can undergo after PO creation. */
public enum PurchaseOrderLineEventType {
    RECEIVED,
    CANCELLED,
    RETURNED_TO_VENDOR
}
