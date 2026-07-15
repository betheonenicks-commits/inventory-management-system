package com.iams.inventory.api;

import com.iams.inventory.api.dto.InventoryStockBalanceResponse;
import com.iams.inventory.api.dto.InventoryTransactionResponse;
import com.iams.inventory.api.dto.LowStockItemResponse;
import com.iams.inventory.api.dto.StockInRequest;
import com.iams.inventory.api.dto.StockOutRequest;
import com.iams.inventory.api.dto.StockTransferRequest;
import com.iams.inventory.application.InventoryStockService;
import com.iams.inventory.application.StockInCommand;
import com.iams.inventory.application.StockOutCommand;
import com.iams.inventory.application.StockTransferCommand;
import com.iams.inventory.domain.InventoryStockBalance;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** US-INV-02/03/04/06/08/09/10: stock movements, balances, low-stock, and expiring-lot views. */
@RestController
@RequestMapping("/api/v1/inventory-stock")
public class InventoryStockController {

    private final InventoryStockService stockService;
    private final InventoryMapper mapper;

    public InventoryStockController(InventoryStockService stockService, InventoryMapper mapper) {
        this.stockService = stockService;
        this.mapper = mapper;
    }

    @PostMapping("/stock-in")
    @PreAuthorize("@perm.has('inventory:write')")
    public InventoryStockBalanceResponse stockIn(@Valid @RequestBody StockInRequest request) {
        InventoryStockBalance balance = stockService.stockIn(new StockInCommand(request.itemId(), request.warehouseId(),
                request.subLocation(), request.lotNumber(), request.expiryDate(), request.quantity(), request.unitCost(),
                request.currencyCode(), request.fxRate(), request.reasonCode()));
        return mapper.toResponse(balance);
    }

    @PostMapping("/stock-out")
    @PreAuthorize("@perm.has('inventory:write')")
    public InventoryStockBalanceResponse stockOut(@Valid @RequestBody StockOutRequest request) {
        InventoryStockBalance balance = stockService.stockOut(new StockOutCommand(request.itemId(), request.warehouseId(),
                request.subLocation(), request.lotNumber(), request.quantity(), request.reasonCode()));
        return mapper.toResponse(balance);
    }

    @PostMapping("/transfer")
    @PreAuthorize("@perm.has('inventory:write')")
    public List<InventoryStockBalanceResponse> transfer(@Valid @RequestBody StockTransferRequest request) {
        InventoryStockService.TransferResult result = stockService.transfer(new StockTransferCommand(request.itemId(),
                request.fromWarehouseId(), request.fromSubLocation(), request.fromLotNumber(), request.toWarehouseId(),
                request.toSubLocation(), request.toLotNumber(), request.quantity(), request.reasonCode()));
        return List.of(mapper.toResponse(result.source()), mapper.toResponse(result.destination()));
    }

    /** Both filters narrow to their intersection when given together - neither silently overrides the other. */
    @GetMapping("/balances")
    @PreAuthorize("@perm.has('inventory:read')")
    public List<InventoryStockBalanceResponse> balances(@RequestParam(required = false) UUID itemId,
                                                          @RequestParam(required = false) UUID warehouseId) {
        List<InventoryStockBalance> balances = itemId != null
                ? stockService.balancesForItem(itemId)
                : warehouseId != null ? stockService.balancesForWarehouse(warehouseId) : List.of();
        if (itemId != null && warehouseId != null) {
            balances = balances.stream().filter(b -> b.getWarehouse().getId().equals(warehouseId)).toList();
        }
        return balances.stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/transactions")
    @PreAuthorize("@perm.has('inventory:read')")
    public List<InventoryTransactionResponse> transactions(@RequestParam UUID itemId) {
        return stockService.transactionsForItem(itemId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("@perm.has('inventory:read')")
    public List<LowStockItemResponse> lowStock() {
        return stockService.lowStockItems().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/expiring-lots")
    @PreAuthorize("@perm.has('inventory:read')")
    public List<InventoryStockBalanceResponse> expiringLots(@RequestParam(defaultValue = "30") int withinDays) {
        return stockService.expiringLots(withinDays).stream().map(mapper::toResponse).toList();
    }
}
