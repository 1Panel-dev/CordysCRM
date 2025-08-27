package cn.cordys.common.util;

import java.util.List;

/**
 * 该类提供了用于训练和预测逻辑回归模型的工具方法。
 */
public class LogisticRegressionUtils {

    // 学习率
    private static final double LEARNING_RATE = 0.01;

    // 最大迭代次数
    private static final int ITERATIONS = 1000;

    /**
     * 使用梯度下降法训练逻辑回归模型。
     *
     * @param features 输入特征列表，每个元素为一个特征向量。
     * @param labels   对应的标签列表，标签值为 0 或 1。
     * @return 训练得到的权重数组。
     */
    public static double[] train(List<double[]> features, List<Integer> labels) {
        // 初始化权重
        double[] weights = new double[features.getFirst().length];

        // 进行迭代优化
        for (int i = 0; i < ITERATIONS; i++) {
            // 计算梯度
            double[] gradients = computeGradients(features, labels, weights);
            // 更新权重
            for (int j = 0; j < weights.length; j++) {
                weights[j] -= LEARNING_RATE * gradients[j];
            }
        }
        return weights;
    }

    /**
     * 计算给定特征和标签的梯度。
     *
     * @param features 输入特征列表，每个元素为一个特征向量。
     * @param labels   对应的标签列表，标签值为 0 或 1。
     * @param weights  当前的模型权重。
     * @return 计算得到的梯度数组。
     */
    private static double[] computeGradients(List<double[]> features, List<Integer> labels, double[] weights) {
        double[] gradients = new double[weights.length];
        for (int i = 0; i < features.size(); i++) {
            // 计算预测值
            double prediction = sigmoid(dotProduct(weights, features.get(i)));
            int label = labels.get(i);
            double error = prediction - label;

            // 计算梯度
            for (int j = 0; j < weights.length; j++) {
                gradients[j] += error * features.get(i)[j];
            }
        }
        return gradients;
    }

    /**
     * 计算两个向量的点积。
     *
     * @param weights 模型的权重向量。
     * @param feature 输入特征向量。
     * @return 点积的值。
     */
    private static double dotProduct(double[] weights, double[] feature) {
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i] * feature[i];
        }
        return sum;
    }

    /**
     * Sigmoid 激活函数，将线性输出转化为概率值。
     *
     * @param x 输入的线性值。
     * @return 转化后的概率值（范围在 0 和 1 之间）。
     */
    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * 基于训练得到的权重和输入特征进行预测。
     *
     * @param weights 训练得到的模型权重。
     * @param feature 输入的特征向量。
     * @return 预测结果，返回 1 表示正类，返回 0 表示负类。
     */
    public static int predict(double[] weights, double[] feature) {
        double prediction = sigmoid(dotProduct(weights, feature));
        return prediction >= 0.5 ? 1 : 0;
    }
}
