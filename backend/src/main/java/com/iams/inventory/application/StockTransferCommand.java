package com.iams.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

/** US-INV-08: an inter-warehouse (or inter-sub-location) transfer, recorded as one atomic linked TRANSFER_OUT/TRANSFER_IN pair. */
public record StockTransferCommand(UUID itemId, UUID fromWarehouseId, String fromSubLocation, String fromLotNumber,
                                    UUID toWarehouseId, String toSubLocation, String toLotNumber,
                                    BigDecimal quantity, String reasonCode) {
}
