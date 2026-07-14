package com.iams.asset.api.mapper;

import com.iams.asset.api.dto.AssetHistoryEventResponse;
import com.iams.asset.api.dto.AssetResponse;
import com.iams.asset.api.dto.AssetStatusInfo;
import com.iams.asset.api.dto.BarcodeInfo;
import com.iams.asset.api.dto.QrCodeInfo;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import org.springframework.stereotype.Component;

/**
 * Hand-written rather than MapStruct-generated: the response has a nested
 * shape (barcode{}, qrCode{} incl. a computed labelUrl) that doesn't map
 * field-for-field from the entity, so a generated mapper would need as much
 * custom-method wiring as this class already is.
 */
@Component
public class AssetMapper {

    public AssetResponse toResponse(Asset asset) {
        String labelUrl = "/api/v1/assets/" + asset.getId() + "/label";
        return new AssetResponse(
                asset.getId(),
                asset.getAssetNumber(),
                asset.getVersion(),
                asset.getName(),
                asset.getCategory().getId(),
                asset.getCategory().getName(),
                asset.getCategory().isRequiresVehicleFields(),
                new AssetStatusInfo(asset.getStatus().getId(), asset.getStatus().getCode(), asset.getStatus().getLabel(), asset.getStatus().isTerminal()),
                asset.getOrgNode().getId(),
                asset.getOrgNode().getName(),
                asset.getAssignedToPersonId(),
                asset.getParentAsset() != null ? asset.getParentAsset().getId() : null,
                asset.getSerialNumber(),
                asset.getManufacturer(),
                asset.getModel(),
                asset.getDescription(),
                new BarcodeInfo("CODE_128", asset.getBarcodeValue()),
                new QrCodeInfo(asset.getQrPayload(), "M", labelUrl),
                asset.getVendorName(),
                asset.getPurchaseOrderReference(),
                asset.getPurchaseDate(),
                asset.getPurchaseCost(),
                asset.getWarrantyStartDate(),
                asset.getWarrantyEndDate(),
                asset.getRfidTagId(),
                asset.getCustomAttributes(),
                asset.getCreatedBy(),
                asset.getCreatedAt(),
                asset.getUpdatedBy(),
                asset.getUpdatedAt()
        );
    }

    public AssetHistoryEventResponse toHistoryResponse(AssetHistoryEvent event) {
        return new AssetHistoryEventResponse(
                event.getId(),
                event.getEventType().name(),
                event.getFieldName(),
                event.getOldValue(),
                event.getNewValue(),
                event.getCorrectionOfEvent() != null ? event.getCorrectionOfEvent().getId() : null,
                event.getCreatedBy(),
                event.getCreatedAt()
        );
    }
}
