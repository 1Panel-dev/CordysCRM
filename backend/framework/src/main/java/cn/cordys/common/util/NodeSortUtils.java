package cn.cordys.common.util;


import cn.cordys.common.dto.NodeSortCountResultDTO;

public class NodeSortUtils {
    //默认节点间隔
    public static final long DEFAULT_NODE_INTERVAL_POS = 4096;

    /**
     * 计算排序
     *
     * @param previousNodePos 前一个节点的pos     如果没有节点则为-1
     * @param nextNodePos     后一个节点的pos     如果没有节点则为-1
     *
     * @return 计算后的num值以及是否需要刷新整棵树的num(如果两个节点之间的num值小于2则需要刷新整棵树的num)
     */
    public static NodeSortCountResultDTO countModuleSort(long previousNodePos, long nextNodePos) {
        boolean refreshNum = false;
        long num;
        if (nextNodePos < 0 && previousNodePos < 0) {
            num = 0;
        } else if (nextNodePos < 0) {
            num = previousNodePos + DEFAULT_NODE_INTERVAL_POS;
        } else if (previousNodePos < 0) {
            num = nextNodePos / 2;
            if (num < 2) {
                refreshNum = true;
            }
        } else {
            long quantityDifference = (nextNodePos - previousNodePos) / 2;
            if (quantityDifference <= 2) {
                refreshNum = true;
            }
            num = previousNodePos + quantityDifference;
        }
        return new NodeSortCountResultDTO(refreshNum, num);
    }
}
