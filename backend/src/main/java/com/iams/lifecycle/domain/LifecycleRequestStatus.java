package com.iams.lifecycle.domain;

/** US-LIF-05/09/11: shared by both transfer and disposal requests - the same simple approve/reject shape. */
public enum LifecycleRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
