package cn.cordys.common.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulaResultNormalizerTest {

    private final FormulaResultNormalizer normalizer = new FormulaResultNormalizer();

    @Test
    void truncatesPositiveNumberWithoutRounding() {
        Object result = normalizer.normalize(
                12.349D, 2, FormulaResultNormalizer.ExpectedType.UNSPECIFIED);

        assertEquals(12.34D, result);
    }

    @Test
    void truncatesNegativeNumberTowardZero() {
        Object result = normalizer.normalize(
                -12.349D, 2, FormulaResultNormalizer.ExpectedType.UNSPECIFIED);

        assertEquals(-12.34D, result);
    }

    @Test
    void truncatesFractionWhenDecimalPlacesAreDisabled() {
        Object result = normalizer.normalize(
                12.999D, 0, FormulaResultNormalizer.ExpectedType.UNSPECIFIED);

        assertEquals(12D, result);
    }

    @Test
    void removesFloatingPointTailBeforeTruncation() {
        Object result = normalizer.normalize(
                0.1D + 0.2D, 2, FormulaResultNormalizer.ExpectedType.UNSPECIFIED);

        assertEquals(0.3D, result);
    }
}
