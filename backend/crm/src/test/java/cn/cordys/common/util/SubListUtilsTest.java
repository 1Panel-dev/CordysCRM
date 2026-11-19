package cn.cordys.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class SubListUtilsTest {

    @Test
    void processesBatchesInOrderWithPartialLastBatch() {
        List<List<Integer>> batches = new ArrayList<>();

        SubListUtils.dealForSubList(List.of(1, 2, 3, 4, 5), 2, batches::add);

        assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), batches);
    }

    @Test
    void changesToACurrentBatchDoNotSkipItemsOrModifyTheInputList() {
        List<Integer> input = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> visited = new ArrayList<>();

        SubListUtils.dealForSubList(input, 2, batch -> {
            visited.addAll(batch);
            batch.clear();
        });

        assertEquals(List.of(1, 2, 3, 4, 5), visited);
        assertEquals(visited, input);
    }

    @Test
    void rejectsNonPositiveBatchSizeBeforeInvokingTheConsumer() {
        assertThrows(IllegalArgumentException.class,
                () -> SubListUtils.dealForSubList(List.of(1), 0, batch -> fail("不应调用批次处理函数")));
    }
}
