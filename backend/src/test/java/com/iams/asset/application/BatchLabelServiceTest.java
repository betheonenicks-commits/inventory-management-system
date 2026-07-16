package com.iams.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.infrastructure.label.LabelProperties;
import com.iams.asset.infrastructure.label.LabelRenderService;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BatchLabelServiceTest {

    @Mock private AssetQueryService queryService;
    @Mock private LabelRenderService labelRenderService;

    private BatchLabelService service;
    private LabelProperties.Size size;

    @BeforeEach
    void setUp() {
        service = new BatchLabelService(queryService, labelRenderService);
        size = new LabelProperties.Size();
        size.setKey("50x25");
        size.setWidthMm(50);
        size.setHeightMm(25);
    }

    private Asset asset(String number) {
        Asset asset = new Asset();
        asset.setAssetNumber(number);
        asset.setBarcodeValue(number);
        asset.setQrPayload("{\"assetNumber\":\"" + number + "\"}");
        return asset;
    }

    @Test
    void render_excludesBrokenAssetsInsteadOfFailingTheBatch() {
        UUID good = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        UUID outOfScope = UUID.randomUUID();
        UUID blankData = UUID.randomUUID();
        when(labelRenderService.findSize("50x25")).thenReturn(Optional.of(size));
        when(queryService.get(good)).thenReturn(asset("AST-1"));
        when(queryService.get(missing)).thenThrow(NotFoundException.of("Asset", missing));
        when(queryService.get(outOfScope)).thenThrow(new AccessDeniedException("outside scope"));
        Asset blank = asset("AST-2");
        blank.setBarcodeValue("  ");
        when(queryService.get(blankData)).thenReturn(blank);
        when(labelRenderService.renderBatchPdf(anyList(), eq(size))).thenReturn(new byte[] {1});

        BatchLabelService.BatchLabelResult result =
                service.render(List.of(good, missing, outOfScope, blankData), "50x25");

        assertThat(result.renderedCount()).isEqualTo(1);
        assertThat(result.excluded()).hasSize(3);
        // Missing and out-of-scope share one merged reason - exclusion never leaks which
        assertThat(result.excluded().stream().filter(e -> e.reason().equals("Not found or not accessible"))).hasSize(2);
        assertThat(result.excluded().stream().filter(e -> e.reason().equals("No valid label data"))).hasSize(1);
    }

    @Test
    void render_failsOnlyWhenNothingSurvives() {
        UUID id = UUID.randomUUID();
        when(labelRenderService.findSize("50x25")).thenReturn(Optional.of(size));
        when(queryService.get(id)).thenThrow(NotFoundException.of("Asset", id));

        assertThatThrownBy(() -> service.render(List.of(id), "50x25"))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("None of the requested assets");
    }

    @Test
    void render_rejectsUnknownSizeAndEmptyAndOversizedBatches() {
        assertThatThrownBy(() -> service.render(List.of(), "50x25")).isInstanceOf(ValidationFailedException.class);

        when(labelRenderService.findSize("nope")).thenReturn(Optional.empty());
        when(labelRenderService.availableSizes()).thenReturn(List.of(size));
        assertThatThrownBy(() -> service.render(List.of(UUID.randomUUID()), "nope"))
                .isInstanceOf(ValidationFailedException.class);

        List<UUID> tooMany = IntStream.range(0, BatchLabelService.MAX_BATCH + 1)
                .mapToObj(i -> UUID.randomUUID()).toList();
        assertThatThrownBy(() -> service.render(tooMany, "50x25"))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("limited to");
    }

    @Test
    void render_deduplicatesRepeatedIds() {
        UUID id = UUID.randomUUID();
        when(labelRenderService.findSize("50x25")).thenReturn(Optional.of(size));
        when(queryService.get(id)).thenReturn(asset("AST-1"));
        when(labelRenderService.renderBatchPdf(anyList(), eq(size))).thenReturn(new byte[] {1});

        BatchLabelService.BatchLabelResult result = service.render(List.of(id, id, id), "50x25");

        assertThat(result.renderedCount()).isEqualTo(1);
    }
}
