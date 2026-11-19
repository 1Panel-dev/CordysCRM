package cn.cordys.common.util;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SubListUtils {

    public static int DEFAULT_EXPORT_BATCH_SIZE = 500;
    public static int DEFAULT_QUERY_BATCH_SIZE = 200;

    /**
     * 按原有顺序分批处理列表，批次的修改不会影响传入的列表结构。
     *
     * @param totalList 原始列表
     * @param batchSize 每批最多处理的元素数
     * @param subFunc 批次处理函数
     * @throws IllegalArgumentException 列表非空且批次大小小于 1
     */
    public static <T> void dealForSubList(List<T> totalList, int batchSize, Consumer<List<T>> subFunc) {
        if (CollectionUtils.isEmpty(totalList)) {
            return;
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        List<T> dealList = new ArrayList<>(totalList);
        int start = 0;
        while (start < dealList.size()) {
            int end = start + Math.min(batchSize, dealList.size() - start);
            List<T> batch = dealList.subList(start, end);
            subFunc.accept(batch);
            start += batch.size();
        }
    }
}
