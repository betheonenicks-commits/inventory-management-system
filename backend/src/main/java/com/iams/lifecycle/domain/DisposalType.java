package com.iams.lifecycle.domain;

/** US-LIF-09: RETIRE targets the asset's RETIRED status, DISPOSE and DONATE both target DISPOSED - only the recorded reason distinguishes a donation from a plain disposal. */
public enum DisposalType {
    RETIRE,
    DISPOSE,
    DONATE
}
