package io.cordys.common.util;


import io.cordys.common.dto.NodeSortCountResultDTO;

public class NodeSortUtils {
    //默认节点间隔
    public static final long DEFAULT_NODE_INTERVAL_NUM = 4096;

    /**
     * 计算排序
     *
     * @param previousNodeNum 前一个节点的num     如果没有节点则为-1
     * @param nextNodeNum     后一个节点的num     如果没有节点则为-1
     * @return 计算后的num值以及是否需要刷新整棵树的num(如果两个节点之间的num值小于2则需要刷新整棵树的num)
     */
    public static NodeSortCountResultDTO countModuleSort(long previousNodeNum, long nextNodeNum) {
        boolean refreshNum = false;
        long num;
        if (nextNodeNum < 0 && previousNodeNum < 0) {
            num = 0;
        } else if (nextNodeNum < 0) {
            num = previousNodeNum + DEFAULT_NODE_INTERVAL_NUM;
        } else if (previousNodeNum < 0) {
            num = nextNodeNum / 2;
            if (num < 2) {
                refreshNum = true;
            }
        } else {
            long quantityDifference = (nextNodeNum - previousNodeNum) / 2;
            if (quantityDifference <= 2) {
                refreshNum = true;
            }
            num = previousNodeNum + quantityDifference;
        }
        return new NodeSortCountResultDTO(refreshNum, num);
    }
}
