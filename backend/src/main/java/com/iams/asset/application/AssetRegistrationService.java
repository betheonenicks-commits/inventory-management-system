package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetCustomFieldDefinitionRepository;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.asset.domain.service.AssetNumberGenerator;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns asset creation (US-AST-01) and general field updates (US-AST-09 etc.).
 * Status transitions specifically go through AssetStatusService, since they
 * need a dedicated STATUS_CHANGE history event and (eventually) transition
 * rules a generic PATCH shouldn't have to know about.
 */
@Service
public class AssetRegistrationService {

    // Newly registered assets start here, not IN_USE - they haven't been
    // assigned to anyone yet. Assignment (US-LIF-04, a later epic) is what
    // moves them to IN_USE.
    private static final String INITIAL_STATUS_CODE = "IN_STORAGE";
    // Falls back to the seeded root node (see V2 migration) if the caller
    // doesn't supply one - full org-scope selection is a later ORG-epic UI concern.
    private static final UUID DEFAULT_ORG_NODE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final AssetRepository assetRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetStatusDefRepository statusDefRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final AssetCustomFieldDefinitionRepository fieldDefinitionRepository;
    private final AssetNumberGenerator assetNumberGenerator;
    private final CustomFieldValidationService customFieldValidationService;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;

    public AssetRegistrationService(AssetRepository assetRepository,
                                     AssetCategoryRepository categoryRepository,
                                     AssetStatusDefRepository statusDefRepository,
                                     OrgNodeRepository orgNodeRepository,
                                     AssetCustomFieldDefinitionRepository fieldDefinitionRepository,
                                     AssetNumberGenerator assetNumberGenerator,
                                     CustomFieldValidationService customFieldValidationService,
                                     AssetHistoryRecorder historyRecorder,
                                     CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
        this.statusDefRepository = statusDefRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.assetNumberGenerator = assetNumberGenerator;
        this.customFieldValidationService = customFieldValidationService;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public Asset register(AssetRegisterCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw ValidationFailedException.singleField("name", "This field is required");
        }
        if (command.categoryId() == null) {
            throw ValidationFailedException.singleField("categoryId", "This field is required");
        }
        validatePurchaseCost(command.purchaseCost());
        validateWarrantyDates(command.warrantyStartDate(), command.warrantyEndDate());

        AssetCategory category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> NotFoundException.of("AssetCategory", command.categoryId()));

        // Validated BEFORE anything is persisted - a missing required custom
        // field must result in zero rows written (AC-AST-01-X).
        customFieldValidationService.validate(
                fieldDefinitionRepository.findByCategoryIdOrderByDisplayOrder(category.getId()),
                command.customFields());

        OrgNode orgNode = orgNodeRepository.findById(
                        command.orgNodeId() != null ? command.orgNodeId() : DEFAULT_ORG_NODE_ID)
                .orElseThrow(() -> NotFoundException.of("OrgNode", command.orgNodeId()));

        var initialStatus = statusDefRepository.findByCode(INITIAL_STATUS_CODE)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: asset_status_def " + INITIAL_STATUS_CODE));

        UUID actor = currentUserProvider.current().id();
        String assetNumber = assetNumberGenerator.next();

        Asset asset = new Asset();
        asset.setAssetNumber(assetNumber);
        asset.setName(command.name());
        asset.setCategory(category);
        asset.setStatus(initialStatus);
        asset.setOrgNode(orgNode);
        asset.setManufacturer(command.manufacturer());
        asset.setModel(command.model());
        asset.setSerialNumber(command.serialNumber());
        asset.setVendorName(command.vendorName());
        asset.setPurchaseOrderReference(command.purchaseOrderReference());
        asset.setPurchaseDate(command.purchaseDate());
        asset.setPurchaseCost(command.purchaseCost());
        asset.setWarrantyStartDate(command.warrantyStartDate());
        asset.setWarrantyEndDate(command.warrantyEndDate());
        asset.setRfidTagId(command.rfidTagId());
        asset.setCustomAttributes(command.customFields() == null ? new HashMap<>() : new HashMap<>(command.customFields()));
        // The QR payload IS the asset number (FR-AST-01) - never a separately
        // allocated code. Barcode value is the same human-facing identifier.
        asset.setBarcodeValue(assetNumber);
        asset.setQrPayload(assetNumber);
        asset.setCreatedBy(actor);

        asset = assetRepository.save(asset);

        historyRecorder.record(asset, AssetHistoryEventType.LIFECYCLE_EVENT, "status", null, initialStatus.getCode());

        return asset;
    }

    @Transactional
    public Asset update(UUID assetId, AssetUpdateCommand command) {
        Asset asset = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));

        if (asset.getVersion() != command.version()) {
            throw new OptimisticLockConflictException(command.version(), asset.getVersion(), asset);
        }

        validatePurchaseCost(command.purchaseCost());
        validateWarrantyDates(
                command.warrantyStartDate() != null ? command.warrantyStartDate() : asset.getWarrantyStartDate(),
                command.warrantyEndDate() != null ? command.warrantyEndDate() : asset.getWarrantyEndDate());

        if (command.customFields() != null) {
            customFieldValidationService.validate(
                    fieldDefinitionRepository.findByCategoryIdOrderByDisplayOrder(asset.getCategory().getId()),
                    command.customFields());
        }

        applyFieldChange(asset, "name", asset.getName(), command.name(), asset::setName);
        applyFieldChange(asset, "manufacturer", asset.getManufacturer(), command.manufacturer(), asset::setManufacturer);
        applyFieldChange(asset, "model", asset.getModel(), command.model(), asset::setModel);
        applyFieldChange(asset, "serialNumber", asset.getSerialNumber(), command.serialNumber(), asset::setSerialNumber);
        applyFieldChange(asset, "vendorName", asset.getVendorName(), command.vendorName(), asset::setVendorName);
        applyFieldChange(asset, "purchaseOrderReference", asset.getPurchaseOrderReference(),
                command.purchaseOrderReference(), asset::setPurchaseOrderReference);
        applyFieldChange(asset, "rfidTagId", asset.getRfidTagId(), command.rfidTagId(), asset::setRfidTagId);

        if (command.purchaseDate() != null) {
            historyRecorder.record(asset, AssetHistoryEventType.FIELD_UPDATE, "purchaseDate",
                    String.valueOf(asset.getPurchaseDate()), String.valueOf(command.purchaseDate()));
            asset.setPurchaseDate(command.purchaseDate());
        }
        if (command.purchaseCost() != null) {
            historyRecorder.record(asset, AssetHistoryEventType.FIELD_UPDATE, "purchaseCost",
                    String.valueOf(asset.getPurchaseCost()), String.valueOf(command.purchaseCost()));
            asset.setPurchaseCost(command.purchaseCost());
        }
        if (command.orgNodeId() != null && !command.orgNodeId().equals(asset.getOrgNode().getId())) {
            OrgNode newNode = orgNodeRepository.findById(command.orgNodeId())
                    .orElseThrow(() -> NotFoundException.of("OrgNode", command.orgNodeId()));
            historyRecorder.record(asset, AssetHistoryEventType.LOCATION_CHANGE, "orgNode",
                    asset.getOrgNode().getCode(), newNode.getCode());
            asset.setOrgNode(newNode);
        }
        if (command.warrantyStartDate() != null) {
            asset.setWarrantyStartDate(command.warrantyStartDate());
        }
        if (command.warrantyEndDate() != null) {
            historyRecorder.record(asset, AssetHistoryEventType.FIELD_UPDATE, "warrantyEndDate",
                    String.valueOf(asset.getWarrantyEndDate()), String.valueOf(command.warrantyEndDate()));
            asset.setWarrantyEndDate(command.warrantyEndDate());
        }
        if (command.customFields() != null) {
            asset.setCustomAttributes(new HashMap<>(command.customFields()));
        }

        asset.setUpdatedBy(currentUserProvider.current().id());

        try {
            return assetRepository.saveAndFlush(asset);
        } catch (OptimisticLockingFailureException e) {
            Asset current = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
            throw new OptimisticLockConflictException(command.version(), current.getVersion(), current);
        }
    }

    private void applyFieldChange(Asset asset, String fieldName, String oldValue, String newValue, java.util.function.Consumer<String> setter) {
        if (newValue != null && !newValue.equals(oldValue)) {
            historyRecorder.record(asset, AssetHistoryEventType.FIELD_UPDATE, fieldName, oldValue, newValue);
            setter.accept(newValue);
        }
    }

    private void validatePurchaseCost(BigDecimal cost) {
        if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
            throw ValidationFailedException.singleField("purchaseCost", "Must not be negative");
        }
    }

    private void validateWarrantyDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw ValidationFailedException.singleField("warrantyEndDate", "Must not be before warrantyStartDate");
        }
    }
}
