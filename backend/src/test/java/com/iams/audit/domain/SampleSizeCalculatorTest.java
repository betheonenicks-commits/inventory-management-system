package com.iams.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iams.common.exception.ValidationFailedException;
import org.junit.jupiter.api.Test;

class SampleSizeCalculatorTest {

    // Well-known Cochran + finite-population-correction results at p=0.5, margin 5%.
    @Test
    void knownSampleSizesForFivethousand() {
        assertThat(SampleSizeCalculator.sampleSize(5000, 90, 5.0)).isEqualTo(257);
        assertThat(SampleSizeCalculator.sampleSize(5000, 95, 5.0)).isEqualTo(357);
        assertThat(SampleSizeCalculator.sampleSize(5000, 99, 5.0)).isEqualTo(586);
    }

    @Test
    void higherConfidenceAndTighterMarginBothRaiseTheSample() {
        long base = SampleSizeCalculator.sampleSize(5000, 95, 5.0);
        assertThat(SampleSizeCalculator.sampleSize(5000, 99, 5.0)).isGreaterThan(base); // more confidence
        assertThat(SampleSizeCalculator.sampleSize(5000, 95, 2.5)).isGreaterThan(base); // tighter margin
    }

    @Test
    void finitePopulationCorrectionCapsAtThePopulation() {
        // A tiny scope: the required sample collapses to the whole population.
        assertThat(SampleSizeCalculator.sampleSize(10, 95, 5.0)).isEqualTo(10);
        assertThat(SampleSizeCalculator.sampleSize(1, 95, 5.0)).isEqualTo(1);
        assertThat(SampleSizeCalculator.sampleSize(100, 95, 5.0)).isEqualTo(80);
    }

    @Test
    void emptyPopulationSamplesNothing() {
        assertThat(SampleSizeCalculator.sampleSize(0, 95, 5.0)).isZero();
    }

    @Test
    void rejectsUnsupportedConfidenceLevel() {
        assertThatThrownBy(() -> SampleSizeCalculator.sampleSize(5000, 92, 5.0))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("Confidence level");
    }

    @Test
    void rejectsOutOfRangeMarginOfError() {
        assertThatThrownBy(() -> SampleSizeCalculator.sampleSize(5000, 95, 0.0))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("Margin of error");
        assertThatThrownBy(() -> SampleSizeCalculator.sampleSize(5000, 95, 50.1))
                .isInstanceOf(ValidationFailedException.class);
        // The boundary (exactly 50%) is allowed.
        assertThat(SampleSizeCalculator.sampleSize(5000, 95, 50.0)).isGreaterThanOrEqualTo(1);
    }
}
