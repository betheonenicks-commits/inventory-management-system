package com.iams.procurement.application;

import java.math.BigDecimal;

public record PurchaseOrderLineCommand(String description, int quantityOrdered, BigDecimal unitCost) {
}
