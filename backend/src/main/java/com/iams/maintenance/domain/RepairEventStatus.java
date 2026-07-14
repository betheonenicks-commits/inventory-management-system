package com.iams.maintenance.domain;

/** US-LIF-06: a repair is OPEN from the moment the asset goes out until it's logged as returned. */
public enum RepairEventStatus {
    OPEN,
    CLOSED
}
