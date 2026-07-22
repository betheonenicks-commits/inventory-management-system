package com.iams.migration.application;

import com.iams.inventory.application.VendorService;
import com.iams.migration.domain.ImportEntityType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * US-MIG-01/03 for the Vendor entity - the second importer, demonstrating that a
 * new importable entity is just a new {@link EntityImportProcessor} bean with no
 * change to the engine. Vendors have no category/org-node resolution and no custom
 * fields, so a row maps straight to {@link VendorService#create}; field validation
 * is delegated to {@link VendorService#validate} so the dry run matches a real create.
 */
@Component
public class VendorImportProcessor implements EntityImportProcessor {

    private static final String TEMPLATE_VERSION = "1.0";
    private static final List<String> COLUMNS = List.of("name", "contactEmail", "contactPhone");
    private static final List<String> SAMPLE_ROW = List.of("Dell Direct", "orders@delldirect.example", "+1-555-0100");

    private final VendorService vendorService;

    public VendorImportProcessor(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @Override
    public ImportEntityType entityType() {
        return ImportEntityType.VENDOR;
    }

    @Override
    public List<String> columns() {
        return COLUMNS;
    }

    @Override
    public List<String> requiredColumns() {
        return List.of("name");
    }

    @Override
    public List<String> sampleRow() {
        return SAMPLE_ROW;
    }

    @Override
    public String templateVersion() {
        return TEMPLATE_VERSION;
    }

    @Override
    public void validate(Map<String, String> row) {
        vendorService.validate(trimToNull(row.get("name")));
    }

    @Override
    public void create(Map<String, String> row) {
        vendorService.create(trimToNull(row.get("name")), trimToNull(row.get("contactEmail")), trimToNull(row.get("contactPhone")));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
