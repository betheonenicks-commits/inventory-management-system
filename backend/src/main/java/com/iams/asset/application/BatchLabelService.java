package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.infrastructure.label.LabelProperties;
import com.iams.asset.infrastructure.label.LabelRenderService;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * US-RPT-11: batch label printing. The AC's failure contract drives the
 * shape - one bad asset is flagged and excluded, never a failed batch. An
 * asset is excluded when it isn't visible to the caller (missing OR outside
 * their org scope - one merged reason so exclusion never leaks which) or when
 * its persisted label data is blank. Only if NOTHING survives is the request
 * itself an error.
 */
@Service
public class BatchLabelService {

    /** One print run, not a bulk-export mechanism - matches the AC's "large intake" scale with headroom. */
    static final int MAX_BATCH = 500;

    private final AssetQueryService queryService;
    private final LabelRenderService labelRenderService;

    public BatchLabelService(AssetQueryService queryService, LabelRenderService labelRenderService) {
        this.queryService = queryService;
        this.labelRenderService = labelRenderService;
    }

    // Deliberately NOT @Transactional: each queryService.get() runs its own
    // read transaction. Wrapping the loop in one shared tx breaks the AC's
    // exclusion contract - a caught NotFoundException from one bad asset
    // still marks the shared tx rollback-only, and the eventual commit dies
    // with UnexpectedRollbackException (found live, 500 on the mixed batch).
    // There is no cross-asset consistency requirement here.
    public BatchLabelResult render(List<UUID> assetIds, String sizeKey) {
        if (assetIds == null || assetIds.isEmpty()) {
            throw ValidationFailedException.singleField("assetIds", "At least one asset id is required");
        }
        Set<UUID> distinctIds = new LinkedHashSet<>(assetIds);
        if (distinctIds.size() > MAX_BATCH) {
            throw ValidationFailedException.singleField("assetIds",
                    "A batch is limited to " + MAX_BATCH + " assets - split larger runs");
        }
        LabelProperties.Size size = labelRenderService.findSize(sizeKey)
                .orElseThrow(() -> ValidationFailedException.singleField("size",
                        "Unsupported label size. Supported: " + labelRenderService.availableSizes().stream()
                                .map(LabelProperties.Size::getKey).toList()));

        List<LabelRenderService.BatchLabel> printable = new ArrayList<>();
        List<ExcludedAsset> excluded = new ArrayList<>();
        for (UUID id : distinctIds) {
            Asset asset;
            try {
                asset = queryService.get(id);
            } catch (NotFoundException | AccessDeniedException e) {
                excluded.add(new ExcludedAsset(id, null, "Not found or not accessible"));
                continue;
            }
            if (isBlank(asset.getBarcodeValue()) || isBlank(asset.getAssetNumber())) {
                excluded.add(new ExcludedAsset(id, asset.getAssetNumber(), "No valid label data"));
                continue;
            }
            printable.add(new LabelRenderService.BatchLabel(asset.getBarcodeValue(), asset.getQrPayload(),
                    asset.getAssetNumber()));
        }

        if (printable.isEmpty()) {
            throw ValidationFailedException.singleField("assetIds",
                    "None of the requested assets have printable labels (" + excluded.size() + " excluded)");
        }
        return new BatchLabelResult(labelRenderService.renderBatchPdf(printable, size), printable.size(), excluded);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record BatchLabelResult(byte[] pdf, int renderedCount, List<ExcludedAsset> excluded) {
    }

    public record ExcludedAsset(UUID assetId, String assetNumber, String reason) {
    }
}
