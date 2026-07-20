package com.iams.audit.domain;

import com.iams.common.exception.ValidationFailedException;

/**
 * US-AUD-20: statistical sample size for auditing a large scope, so a full 100%
 * scan isn't the only option. Cochran's formula at p=0.5 (maximum variability -
 * the conservative choice when the true missing/found split is unknown) with the
 * finite-population correction, so the sample never exceeds - and for small scopes
 * collapses toward - the population itself.
 * <p>
 * Pure and side-effect-free by design: the critical business math lives here alone,
 * unit-tested to 100%, and both the preview endpoint and audit creation call it so a
 * previewed size can never disagree with the size actually frozen.
 */
public final class SampleSizeCalculator {

    private SampleSizeCalculator() {
    }

    /** Two-tailed z-scores for the confidence levels the UI offers. */
    private static double zScore(int confidenceLevel) {
        return switch (confidenceLevel) {
            case 90 -> 1.645;
            case 95 -> 1.960;
            case 99 -> 2.576;
            default -> throw ValidationFailedException.singleField("confidenceLevel",
                    "Confidence level must be one of 90, 95, or 99");
        };
    }

    /**
     * @param population        total assets in scope (0 → sample of 0)
     * @param confidenceLevel   90, 95, or 99
     * @param marginOfErrorPct  acceptable error, in percent, in (0, 50]
     * @return assets to verify — always within [1, population] for a non-empty population
     */
    public static long sampleSize(long population, int confidenceLevel, double marginOfErrorPct) {
        double z = zScore(confidenceLevel); // validates confidenceLevel first
        if (marginOfErrorPct <= 0 || marginOfErrorPct > 50) {
            throw ValidationFailedException.singleField("marginOfError",
                    "Margin of error must be greater than 0 and at most 50 percent");
        }
        if (population <= 0) {
            return 0;
        }
        double e = marginOfErrorPct / 100.0;
        double n0 = (z * z * 0.25) / (e * e);          // infinite-population estimate at p=0.5
        double n = n0 / (1 + (n0 - 1) / population);    // finite-population correction
        long sample = (long) Math.ceil(n);
        return Math.max(1, Math.min(sample, population));
    }
}
