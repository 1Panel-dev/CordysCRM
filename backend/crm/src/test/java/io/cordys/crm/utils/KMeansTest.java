package io.cordys.crm.utils;

import io.cordys.common.util.KMeansUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
        System.out.println(clusters);
    }
}
