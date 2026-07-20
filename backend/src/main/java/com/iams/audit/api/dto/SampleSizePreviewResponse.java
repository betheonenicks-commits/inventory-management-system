package com.iams.audit.api.dto;

/**
 * US-AUD-20: how many of {@code populationSize} assets a statistical sample at the
 * given confidence level and margin of error would cover.
 */
public record SampleSizePreviewResponse(
        long populationSize,
        int confidenceLevel,
        double marginOfError,
        long sampleSize
) {
}
