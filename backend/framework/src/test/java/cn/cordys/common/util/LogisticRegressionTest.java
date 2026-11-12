package cn.cordys.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticRegressionTest {
    @Test
    void learnsBothClassesFromScaledFeatures() {
        // 首列显式提供截距；特征使用相近量级，避免大额收入掩盖分类错误。
        List<double[]> features = List.of(
                new double[] { 1, -2 }, new double[] { 1, -1 },
                new double[] { 1, 1 }, new double[] { 1, 2 });

        double[] weights = LogisticRegressionUtils.train(features, List.of(0, 0, 1, 1));

        assertEquals(features.getFirst().length, weights.length);
        assertTrue(Arrays.stream(weights).allMatch(Double::isFinite));
        assertEquals(0, LogisticRegressionUtils.predict(weights, new double[] { 1, -1.5 }));
        assertEquals(1, LogisticRegressionUtils.predict(weights, new double[] { 1, 1.5 }));
    }

    @ParameterizedTest
    @CsvSource({ "-1000, 0", "-1, 0", "0, 1", "1, 1", "1000, 1" })
    void predictionUsesHalfProbabilityThreshold(double feature, int expected) {
        assertEquals(expected, LogisticRegressionUtils.predict(new double[] { 1 }, new double[] { feature }));
    }
}
