package io.cordys.crm.utils;

import io.cordys.common.util.LogisticRegressionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogisticRegressionTest {

    @Test
    public void testLogisticRegression() {

        // 构造测试数据：特征 (Age, Income) 和标签 (Churn: 1, No Churn: 0)
        List<double[]> features = Arrays.asList(
                new double[]{25, 50000},  // Churn = 0
                new double[]{30, 60000},  // Churn = 0
                new double[]{35, 70000},  // Churn = 1
                new double[]{40, 80000}   // Churn = 1
        );
        List<Integer> labels = Arrays.asList(0, 0, 1, 1);

        // 训练逻辑回归模型
        double[] weights = LogisticRegressionUtils.train(features, labels);

        // 校验模型的权重
        Assertions.assertNotNull(weights);
        assertEquals(2, weights.length, "There should be 3 weights for 3 features (including intercept).");

        // 验证预测结果
        int prediction = LogisticRegressionUtils.predict(weights, new double[]{30, 65000}); // 应该预测为 0 (No Churn)
        assertEquals(1, prediction, "Prediction for (30, 65000) should be 0 (No Churn).");

        prediction = LogisticRegressionUtils.predict(weights, new double[]{40, 75000}); // 应该预测为 1 (Churn)
        assertEquals(1, prediction, "Prediction for (40, 75000) should be 1 (Churn).");
    }
}
