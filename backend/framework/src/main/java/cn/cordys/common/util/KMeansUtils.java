package cn.cordys.common.util;

import java.util.*;

/**
 * 该类实现了 K-means 聚类算法的核心功能，包括数据点分配、质心更新等操作。
 * 它提供了基于输入数据集和指定簇数进行 K-means 聚类的工具方法。
 */
public class KMeansUtils {

    /**
     * 执行 K-means 聚类算法。
     *
     * @param data 输入的数据集，每个数据点是一个特征向量。
     * @param k    聚类的簇数。
     * @return 包含每个数据点所属簇的列表，元素值为簇的索引。
     */
    public static List<Integer> performKMeans(List<double[]> data, int k) {
        int maxIterations = 100;
        List<double[]> centroids = initializeCentroids(data, k);
        List<Integer> clusters = new ArrayList<>(Collections.nCopies(data.size(), -1));

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean converged = true;

            // 第 1 步：将数据点分配给最近的质心
            for (int i = 0; i < data.size(); i++) {
                int closestCentroid = findClosestCentroid(data.get(i), centroids);
                if (clusters.get(i) != closestCentroid) {
                    clusters.set(i, closestCentroid);
                    converged = false;
                }
            }

            // 第 2 步：重新计算质心
            for (int j = 0; j < k; j++) {
                double[] newCentroid = recalculateCentroid(data, clusters, j);
                if (!Arrays.equals(newCentroid, centroids.get(j))) {
                    centroids.set(j, newCentroid);
                    converged = false;
                }
            }

            if (converged) break;
        }

        return clusters;
    }

    /**
     * 随机初始化 K 个质心。
     *
     * @param data 输入的数据集，每个数据点是一个特征向量。
     * @param k    聚类的簇数。
     * @return 初始化的 K 个质心。
     */
    private static List<double[]> initializeCentroids(List<double[]> data, int k) {
        List<double[]> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids.add(data.get(random.nextInt(data.size())));
        }
        return centroids;
    }

    /**
     * 计算一个数据点到所有质心的欧几里得距离，并返回距离最近的质心的索引。
     *
     * @param point     输入的数据点。
     * @param centroids 所有的质心列表。
     * @return 最近质心的索引。
     */
    private static int findClosestCentroid(double[] point, List<double[]> centroids) {
        double minDistance = Double.MAX_VALUE;
        int closestCentroid = -1;
        for (int i = 0; i < centroids.size(); i++) {
            double distance = euclideanDistance(point, centroids.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                closestCentroid = i;
            }
        }
        return closestCentroid;
    }

    /**
     * 计算两个数据点之间的欧几里得距离。
     *
     * @param point1 第一个数据点。
     * @param point2 第二个数据点。
     * @return 两个数据点之间的欧几里得距离。
     */
    private static double euclideanDistance(double[] point1, double[] point2) {
        double sum = 0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * 重新计算给定簇的质心。
     *
     * @param data         输入的数据集，每个数据点是一个特征向量。
     * @param clusters     每个数据点所属的簇的索引。
     * @param clusterIndex 当前簇的索引。
     * @return 该簇的新质心。
     */
    private static double[] recalculateCentroid(List<double[]> data, List<Integer> clusters, int clusterIndex) {
        double[] centroid = new double[data.getFirst().length];
        int count = 0;
        for (int i = 0; i < data.size(); i++) {
            if (clusters.get(i) == clusterIndex) {
                for (int j = 0; j < centroid.length; j++) {
                    centroid[j] += data.get(i)[j];
                }
                count++;
            }
        }
        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= count;
        }
        return centroid;
    }
}
