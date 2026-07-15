package com.iams.inventory.domain;

/** US-INV-06: WEIGHTED_AVERAGE is the only one actually recalculated on receipt; LAST_COST just stores the latest unit cost verbatim. */
public enum CostingMethod {
    WEIGHTED_AVERAGE,
    LAST_COST
}
