package com.iams.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

public record StockOutCommand(UUID itemId, UUID warehouseId, String subLocation, String lotNumber,
                               BigDecimal quantity, String reasonCode) {
}
