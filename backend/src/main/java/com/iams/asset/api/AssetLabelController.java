package com.iams.asset.api;

import com.iams.analytics.application.TrackUsage;
import com.iams.asset.api.dto.LabelConfigResponse;
import com.iams.asset.application.AssetQueryService;
import com.iams.asset.application.BatchLabelService;
import com.iams.asset.domain.Asset;
import com.iams.asset.infrastructure.label.LabelFormat;
import com.iams.asset.infrastructure.label.LabelProperties;
import com.iams.asset.infrastructure.label.LabelRenderService;
import com.iams.common.exception.ValidationFailedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final BatchLabelService batchLabelService;

    public AssetLabelController(AssetQueryService queryService, LabelRenderService labelRenderService,
                                 BatchLabelService batchLabelService) {
        this.queryService = queryService;
        this.labelRenderService = labelRenderService;
        this.batchLabelService = batchLabelService;
    }

    @GetMapping("/{id}/label")
    @TrackUsage(module = "assets", action = "print-label")
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

    /**
     * US-RPT-11: one print-ready PDF for a whole selection. Assets without
     * printable data are flagged in the X-IAMS-Labels-* headers and excluded
     * rather than failing the batch; per-asset org scope is enforced in the
     * service (an out-of-scope id is excluded, indistinguishable from a
     * missing one). Authenticated read, same as the single-label endpoint.
     */
    @PostMapping("/labels/batch")
    @TrackUsage(module = "assets", action = "print-batch-labels")
    public ResponseEntity<byte[]> batchLabels(@Valid @RequestBody BatchLabelRequest request) {
        String sizeKey = request.size() != null ? request.size() : DEFAULT_SIZE_KEY;
        BatchLabelService.BatchLabelResult result = batchLabelService.render(request.assetIds(), sizeKey);

        String detail = result.excluded().stream()
                .map(e -> e.assetId() + "=" + e.reason().replace(';', ','))
                .collect(Collectors.joining("; "));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("asset-labels-batch.pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-IAMS-Labels-Rendered", String.valueOf(result.renderedCount()))
                .header("X-IAMS-Labels-Excluded", String.valueOf(result.excluded().size()))
                .header("X-IAMS-Labels-Excluded-Detail", detail)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        "X-IAMS-Labels-Rendered, X-IAMS-Labels-Excluded, X-IAMS-Labels-Excluded-Detail")
                .body(result.pdf());
    }

    public record BatchLabelRequest(@NotEmpty List<UUID> assetIds, String size) {
    }

    @GetMapping("/labels/config")
    public LabelConfigResponse labelConfig() {
        List<LabelConfigResponse.SymbologyInfo> symbologies = List.of(
                new LabelConfigResponse.SymbologyInfo("CODE_128", null),
                new LabelConfigResponse.SymbologyInfo("QR", "M"));
        List<LabelConfigResponse.LabelSizeResponse> sizes = labelRenderService.availableSizes().stream()
                .map(s -> new LabelConfigResponse.LabelSizeResponse(s.getKey(), s.getWidthMm(), s.getHeightMm()))
                .toList();
        return new LabelConfigResponse(symbologies, sizes);
    }
}
