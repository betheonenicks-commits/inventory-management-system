package com.iams.migration.application;

import com.iams.asset.application.AssetRegisterCommand;
import com.iams.asset.application.AssetRegistrationService;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.migration.domain.ImportEntityType;
import com.iams.org.domain.OrgNodeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * US-MIG-01/03 for the Asset entity: the one place that knows the Asset import
 * column contract. {@link #COLUMNS} is the single source of truth the downloadable
 * template (US-MIG-01) and the dry-run validator (US-MIG-03) both read, so "the
 * template's columns match exactly what the validator checks" holds by construction.
 * <p>
 * A row references a category and org node by their human-facing {@code code}
 * (a spreadsheet can't carry a UUID); everything else maps straight onto the
 * existing {@link AssetRegisterCommand}. Field-level validation (required name,
 * cost/warranty rules, custom-field schema) is NOT re-implemented here - it is
 * delegated to {@link AssetRegistrationService#validate}, the same method a real
 * POST /assets create runs, so the two can never drift.
 */
@Component
public class AssetImportProcessor implements EntityImportProcessor {

    public static final String TEMPLATE_VERSION = "1.0";

    /** Ordered import columns. name/categoryCode are required; the rest are optional. */
    public static final List<String> COLUMNS = List.of(
            "name", "categoryCode", "manufacturer", "model", "serialNumber",
            "vendorName", "purchaseOrderReference", "purchaseDate", "purchaseCost",
            "orgNodeCode", "warrantyStartDate", "warrantyEndDate", "rfidTagId");

    /** An illustrative first data row so a user sees the expected formats, not a bare header. */
    public static final List<String> SAMPLE_ROW = List.of(
            "Dell Latitude 5540", "IT-LAPTOP", "Dell", "Latitude 5540", "SN-EXAMPLE-001",
            "Dell Direct", "PO-2026-0001", "2026-01-15", "1299.00",
            "ROOT", "2026-01-15", "2029-01-15", "");

    private final AssetCategoryRepository categoryRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final AssetRegistrationService assetRegistrationService;

    public AssetImportProcessor(AssetCategoryRepository categoryRepository, OrgNodeRepository orgNodeRepository,
                                 AssetRegistrationService assetRegistrationService) {
        this.categoryRepository = categoryRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.assetRegistrationService = assetRegistrationService;
    }

    @Override
    public ImportEntityType entityType() {
        return ImportEntityType.ASSET;
    }

    @Override
    public List<String> columns() {
        return COLUMNS;
    }

    @Override
    public List<String> requiredColumns() {
        return List.of("name", "categoryCode");
    }

    @Override
    public List<String> sampleRow() {
        return SAMPLE_ROW;
    }

    @Override
    public String templateVersion() {
        return TEMPLATE_VERSION;
    }

    /**
     * Turn one raw column-value row into a create command, resolving codes to ids
     * and parsing typed cells. Throws {@link ValidationFailedException} naming the
     * offending column on any resolution/parse failure - the caller records it as a
     * per-row error. Does NOT persist.
     */
    public AssetRegisterCommand buildCommand(Map<String, String> row) {
        UUID categoryId = null;
        String categoryCode = trimToNull(row.get("categoryCode"));
        if (categoryCode != null) {
            categoryId = categoryRepository.findByCode(categoryCode)
                    .orElseThrow(() -> ValidationFailedException.singleField("categoryCode",
                            "Unknown category code '" + categoryCode + "'"))
                    .getId();
        }
        // A blank categoryCode leaves categoryId null; AssetRegistrationService.validate
        // then reports the required-field error, so there is one required-field authority.

        UUID orgNodeId = null;
        String orgNodeCode = trimToNull(row.get("orgNodeCode"));
        if (orgNodeCode != null) {
            orgNodeId = orgNodeRepository.findByCode(orgNodeCode)
                    .orElseThrow(() -> ValidationFailedException.singleField("orgNodeCode",
                            "Unknown org node code '" + orgNodeCode + "'"))
                    .getId();
        }

        return new AssetRegisterCommand(
                categoryId,
                trimToNull(row.get("name")),
                trimToNull(row.get("manufacturer")),
                trimToNull(row.get("model")),
                trimToNull(row.get("serialNumber")),
                trimToNull(row.get("vendorName")),
                trimToNull(row.get("purchaseOrderReference")),
                parseDate(row.get("purchaseDate"), "purchaseDate"),
                parseCost(row.get("purchaseCost")),
                orgNodeId,
                parseDate(row.get("warrantyStartDate"), "warrantyStartDate"),
                parseDate(row.get("warrantyEndDate"), "warrantyEndDate"),
                trimToNull(row.get("rfidTagId")),
                null); // v1 template carries no per-category custom fields
    }

    /** Dry-run validation for one row: exactly what a create would enforce, nothing written. */
    @Override
    public void validate(Map<String, String> row) {
        assetRegistrationService.validate(buildCommand(row));
    }

    /** Commit one row: the real create (its own transaction inside AssetRegistrationService). */
    @Override
    public void create(Map<String, String> row) {
        assetRegistrationService.register(buildCommand(row));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static LocalDate parseDate(String value, String field) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw ValidationFailedException.singleField(field, "Expected a date in yyyy-MM-dd format, got '" + v + "'");
        }
    }

    private static BigDecimal parseCost(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw ValidationFailedException.singleField("purchaseCost", "Expected a number, got '" + v + "'");
        }
    }
}
