package com.iams.inventory.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.Vendor;
import com.iams.inventory.domain.VendorRepository;
import com.iams.procurement.domain.PurchaseOrder;
import com.iams.procurement.domain.PurchaseOrderRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-INV-07/08: vendor CRUD, independent of items, plus their full purchase-order history. */
@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CurrentUserProvider currentUserProvider;

    public VendorService(VendorRepository vendorRepository, PurchaseOrderRepository purchaseOrderRepository,
                          CurrentUserProvider currentUserProvider) {
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * The field validation a create enforces, isolated so the bulk importer's
     * dry run (US-MIG-03) can run exactly it per row - the import validator and a
     * real create share this one path and cannot drift.
     */
    public void validate(String name) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "A vendor name is required");
        }
    }

    @Transactional
    public Vendor create(String name, String contactEmail, String contactPhone) {
        validate(name);
        UUID actor = currentUserProvider.current().id();
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setContactEmail(contactEmail);
        vendor.setContactPhone(contactPhone);
        vendor.setActive(true);
        vendor.setCreatedBy(actor);
        return vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    public Vendor get(UUID id) {
        return vendorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Vendor", id));
    }

    @Transactional(readOnly = true)
    public List<Vendor> list() {
        return vendorRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Vendor update(UUID id, String name, String contactEmail, String contactPhone) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "A vendor name is required");
        }
        Vendor vendor = get(id);
        vendor.setName(name);
        vendor.setContactEmail(contactEmail);
        vendor.setContactPhone(contactPhone);
        vendor.setUpdatedBy(currentUserProvider.current().id());
        return vendorRepository.saveAndFlush(vendor);
    }

    /** AC-INV-08-H: stops appearing in new-PO vendor pickers, but historical POs remain intact and visible - never a delete. */
    @Transactional
    public Vendor deactivate(UUID id) {
        Vendor vendor = get(id);
        vendor.setActive(false);
        vendor.setUpdatedBy(currentUserProvider.current().id());
        return vendorRepository.saveAndFlush(vendor);
    }

    /** US-INV-07: every historical PO for this vendor, most recent first. */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> purchaseHistory(UUID vendorId) {
        if (!vendorRepository.existsById(vendorId)) {
            throw NotFoundException.of("Vendor", vendorId);
        }
        return purchaseOrderRepository.findByVendorIdWithRequestOrderByCreatedAtDesc(vendorId);
    }
}
