package io.cordys.crm.utils;

import io.cordys.common.util.KMeansUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class KMeansTest {

    @Test
    public void testKMeans() {

        // 构造测试数据：二维点 (Age, Income)
        List<double[]> data = Arrays.asList(
                new double[]{25, 50000},
                new double[]{30, 60000},
                new double[]{35, 70000},
                new double[]{40, 80000},
                new double[]{45, 90000},
                new double[]{50, 100000}
        );

        // 执行 K-means 算法，分为 2 个簇
        List<Integer> clusters = KMeansUtils.performKMeans(data, 2);

        // 校验结果：检查是否每个簇的数量符合预期
        assertEquals(6, clusters.size(), "The number of data points should remain the same.");

        // 进一步检查返回的簇分配（这是一个简单检查，通常会根据数据结果来验证）
        assertNotNull(clusters);
        assertTrue(clusters.stream().anyMatch(cluster -> cluster == 0), "Cluster 0 should exist.");
        assertTrue(clusters.stream().anyMatch(cluster -> cluster == 1), "Cluster 1 should exist.");
    }
}
