package com.iams.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * subLocation/lotNumber default to "" when not provided (never null - see
 * InventoryStockBalance's own Javadoc). currencyCode/fxRate are US-INV-10:
 * null currencyCode means "already in the reporting currency" (fxRate forced
 * to 1); a non-null, non-reporting currencyCode requires a real fxRate.
 */
public record StockInCommand(UUID itemId, UUID warehouseId, String subLocation, String lotNumber, LocalDate expiryDate,
                              BigDecimal quantity, BigDecimal unitCost, String currencyCode, BigDecimal fxRate,
                              String reasonCode) {
}
