package com.iams.inventory.domain;

/** US-INV-02/05/08: what kind of movement a ledger row represents. TRANSFER_OUT/TRANSFER_IN always come in a linked pair (see InventoryTransaction.linkedTransactionId). */
public enum InventoryTransactionType {
    STOCK_IN,
    STOCK_OUT,
    ADJUSTMENT,
    TRANSFER_OUT,
    TRANSFER_IN
}
