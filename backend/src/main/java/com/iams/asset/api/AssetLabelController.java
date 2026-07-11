package com.iams.asset.api;

import com.iams.asset.application.AssetQueryService;
import com.iams.asset.domain.Asset;
import com.iams.asset.infrastructure.label.LabelFormat;
import com.iams.asset.infrastructure.label.LabelProperties;
import com.iams.asset.infrastructure.label.LabelRenderService;
import com.iams.common.exception.ValidationFailedException;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renders an asset's barcode+QR label on demand (FR-AST-02, FR-SCN-07).
 * Deliberately a pure read: printing/downloading a label never mutates
 * anything and never blocks or depends on registration having "worked
 * perfectly" - it only needs the asset's already-persisted number values.
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetLabelController {

    private static final String DEFAULT_SIZE_KEY = "50x25";

    private final AssetQueryService queryService;
    private final LabelRenderService labelRenderService;

    public AssetLabelController(AssetQueryService queryService, LabelRenderService labelRenderService) {
        this.queryService = queryService;
        this.labelRenderService = labelRenderService;
    }

    @GetMapping("/{id}/label")
    public ResponseEntity<byte[]> label(@PathVariable UUID id,
                                         @RequestParam(defaultValue = "png") String format,
                                         @RequestParam(required = false) String size) {
        Asset asset = queryService.get(id);

        LabelFormat labelFormat;
        try {
            labelFormat = LabelFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ValidationFailedException.singleField("format", "Must be one of: png, svg, pdf");
        }

        String sizeKey = size != null ? size : DEFAULT_SIZE_KEY;
        LabelProperties.Size labelSize = labelRenderService.findSize(sizeKey)
                .orElseThrow(() -> ValidationFailedException.singleField("size",
                        "Unsupported label size. Supported: " + labelRenderService.availableSizes().stream()
                                .map(LabelProperties.Size::getKey).toList()));

        byte[] body = labelRenderService.render(asset.getBarcodeValue(), asset.getQrPayload(), asset.getAssetNumber(), labelFormat, labelSize);

        String extension = labelFormat.name().toLowerCase();
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(asset.getAssetNumber() + "-label." + extension)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(labelFormat.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
