package com.iams.inventory.api;

import com.iams.inventory.api.dto.InventoryItemResponse;
import com.iams.inventory.api.dto.InventoryStockBalanceResponse;
import com.iams.inventory.api.dto.InventoryTransactionResponse;
import com.iams.inventory.api.dto.LowStockItemResponse;
import com.iams.inventory.api.dto.ManualAdjustmentResponse;
import com.iams.inventory.api.dto.VendorResponse;
import com.iams.inventory.api.dto.WarehouseResponse;
import com.iams.inventory.application.InventoryStockService;
import com.iams.inventory.domain.InventoryItem;
import com.iams.inventory.domain.InventoryManualAdjustmentRequest;
import com.iams.inventory.domain.InventoryStockBalance;
import com.iams.inventory.domain.InventoryTransaction;
import com.iams.inventory.domain.Vendor;
import com.iams.inventory.domain.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getVersion(),
                warehouse.getName(),
                warehouse.getCode(),
                warehouse.getOrgNode().getId(),
                warehouse.getOrgNode().getName(),
                warehouse.isActive()
        );
    }

    public VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getVersion(),
                vendor.getName(),
                vendor.getContactEmail(),
                vendor.getContactPhone(),
                vendor.isActive()
        );
    }

    public InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getVersion(),
                item.getName(),
                item.getSku(),
                item.getUnitOfMeasure(),
                item.getReorderLevel(),
                item.getCostingMethod(),
                item.isActive()
        );
    }

    public InventoryStockBalanceResponse toResponse(InventoryStockBalance balance) {
        return new InventoryStockBalanceResponse(
                balance.getId(),
                balance.getInventoryItem().getId(),
                balance.getInventoryItem().getName(),
                balance.getInventoryItem().getSku(),
                balance.getInventoryItem().getUnitOfMeasure(),
                balance.getWarehouse().getId(),
                balance.getWarehouse().getName(),
                balance.getSubLocation(),
                balance.getLotNumber(),
                balance.getExpiryDate(),
                balance.getQuantityOnHand(),
                balance.getAverageUnitCost(),
                balance.getVersion()
        );
    }

    public InventoryTransactionResponse toResponse(InventoryTransaction transaction) {
        return new InventoryTransactionResponse(
                transaction.getId(),
                transaction.getInventoryItem().getId(),
                transaction.getInventoryItem().getName(),
                transaction.getWarehouse().getId(),
                transaction.getWarehouse().getName(),
                transaction.getSubLocation(),
                transaction.getLotNumber(),
                transaction.getExpiryDate(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getUnitCost(),
                transaction.getCurrencyCode(),
                transaction.getFxRate(),
                transaction.getReportingUnitCost(),
                transaction.getReasonCode(),
                transaction.getPerformedByUserId(),
                transaction.getPerformedByUsername(),
                transaction.getPerformedAt(),
                transaction.getLinkedTransactionId()
        );
    }

    public LowStockItemResponse toResponse(InventoryStockService.LowStockItem lowStockItem) {
        InventoryItem item = lowStockItem.item();
        return new LowStockItemResponse(
                item.getId(),
                item.getName(),
                item.getSku(),
                item.getUnitOfMeasure(),
                item.getReorderLevel(),
                lowStockItem.totalQuantity()
        );
    }

    public ManualAdjustmentResponse toResponse(InventoryManualAdjustmentRequest request) {
        return new ManualAdjustmentResponse(
                request.getId(),
                request.getVersion(),
                request.getInventoryItem().getId(),
                request.getInventoryItem().getName(),
                request.getWarehouse().getId(),
                request.getWarehouse().getName(),
                request.getSubLocation(),
                request.getLotNumber(),
                request.getQuantityDelta(),
                request.getReason(),
                request.getStatus(),
                request.getNominalApproverId(),
                request.getEffectiveApproverId(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getDecidedBy(),
                request.getDecidedAt(),
                request.getRejectionReason(),
                request.getResultingTransactionId()
        );
    }
}
