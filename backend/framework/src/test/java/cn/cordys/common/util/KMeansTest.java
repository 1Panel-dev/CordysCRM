package cn.cordys.common.util;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class KMeansTest {
    @Test
    void singleClusterContainsEveryPoint() {
        List<double[]> data = List.of(new double[] { -3, 4 }, new double[] { 0, 0 }, new double[] { 5, -2 });

        assertEquals(List.of(0, 0, 0), KMeansUtils.performKMeans(data, 1));
    }

    @Test
    void separatesDistantGroupsWithoutDependingOnClusterLabels() {
        List<double[]> data = List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 },
                new double[] { 10, 10 }, new double[] { 10, 11 });

        // 固定初始质心，使测试只验证聚类行为，不依赖随机抽样。
        try (MockedConstruction<Random> ignored = mockConstruction(Random.class,
                (random, context) -> when(random.nextInt(data.size())).thenReturn(0, 2))) {
            List<Integer> clusters = KMeansUtils.performKMeans(data, 2);

            assertEquals(data.size(), clusters.size());
            assertEquals(clusters.get(0), clusters.get(1));
            assertEquals(clusters.get(2), clusters.get(3));
            assertNotEquals(clusters.get(0), clusters.get(2));
        }
    }
}
