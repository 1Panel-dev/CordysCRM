package io.cordys.crm.system.service;

import io.cordys.common.dto.BaseTree;
import io.cordys.common.dto.NodeSortCountResultDTO;
import io.cordys.common.dto.NodeSortDTO;
import io.cordys.common.dto.NodeSortQueryParam;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.NodeSortUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.dto.request.NodeMoveRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.function.Function;

public abstract class MoveNodeService {

    protected static final long LIMIT_NUM = NodeSortUtils.DEFAULT_NODE_INTERVAL_NUM;

    public abstract void updateNum(String id, long num);

    public abstract void refreshNum(String testPlanId);


    private static final String MOVE_NUM_OPERATOR_LESS = "lessThan";
    private static final String MOVE_NUM_OPERATOR_MORE = "moreThan";
    private static final String MOVE_NUM_OPERATOR_LATEST = "latest";
    private static final String DRAG_NODE_NOT_EXIST = "drag_node.not.exist";


    /**
     * 构建节点排序的参数
     *
     * @param request           拖拽的前端请求参数
     * @param selectIdNodeFunc  通过id查询节点的函数
     * @param selectNumNodeFunc 通过parentId和num运算符查询节点的函数
     * @return
     */
    public NodeSortDTO getNodeSortDTO(NodeMoveRequest request, Function<String, BaseTree> selectIdNodeFunc, Function<NodeSortQueryParam, BaseTree> selectNumNodeFunc) {
        if (StringUtils.equals(request.getDragNodeId(), request.getDropNodeId())) {
            //两种节点不能一样
            throw new GenericException(Translator.get("invalid_parameter") + ": drag node  and drop node");
        }

        BaseTree dragNode = selectIdNodeFunc.apply(request.getDragNodeId());
        if (dragNode == null) {
            throw new GenericException(Translator.get(DRAG_NODE_NOT_EXIST) + ":" + request.getDragNodeId());
        }

        BaseTree dropNode = selectIdNodeFunc.apply(request.getDropNodeId());
        if (dropNode == null) {
            throw new GenericException(Translator.get(DRAG_NODE_NOT_EXIST) + ":" + request.getDropNodeId());

        }
        BaseTree parentModule;
        BaseTree previousNode;
        BaseTree nextNode = null;
        if (request.getDropPosition() == 0) {
            parentModule = new BaseTree(dropNode.getId(), dropNode.getName(), dropNode.getNum(), dropNode.getOrganizationId(), dropNode.getParentId());

            NodeSortQueryParam sortParam = new NodeSortQueryParam();
            sortParam.setParentId(dropNode.getId());
            sortParam.setOperator(MOVE_NUM_OPERATOR_LATEST);
            previousNode = selectNumNodeFunc.apply(sortParam);
        } else {
            parentModule = selectIdNodeFunc.apply(dropNode.getParentId());
            if (request.getDropPosition() == 1) {
                //dropnumition=1: 放到dropNode节点后，原dropNode后面的节点之前
                previousNode = dropNode;

                NodeSortQueryParam sortParam = new NodeSortQueryParam();
                sortParam.setParentId(parentModule.getId());
                sortParam.setNum(previousNode.getNum());
                sortParam.setOperator(MOVE_NUM_OPERATOR_MORE);
                nextNode = selectNumNodeFunc.apply(sortParam);
            } else if (request.getDropPosition() == -1) {
                //dropnumition=-1: 放到dropNode节点前，原dropNode前面的节点之后
                nextNode = dropNode;

                NodeSortQueryParam sortParam = new NodeSortQueryParam();
                sortParam.setParentId(parentModule.getId());
                sortParam.setNum(nextNode.getNum());
                sortParam.setOperator(MOVE_NUM_OPERATOR_LESS);
                previousNode = selectNumNodeFunc.apply(sortParam);
            } else {
                throw new GenericException(Translator.get("invalid_parameter") + ": dropPosition");
            }
        }

        return new NodeSortDTO(dragNode, parentModule, previousNode, nextNode);
    }

    //排序
    public void sort(@Validated NodeSortDTO nodeMoveDTO) {
        // 获取相邻节点
        BaseTree previousNode = nodeMoveDTO.getPreviousNode();
        BaseTree nextNode = nodeMoveDTO.getNextNode();

        NodeSortCountResultDTO countResultDTO = NodeSortUtils.countModuleSort(
                previousNode == null ? -1 : previousNode.getNum(),
                nextNode == null ? -1 : nextNode.getNum());
        updateNum(nodeMoveDTO.getNode().getId(), countResultDTO.getNum());
        if (countResultDTO.isRefreshNum()) {
            refreshNum(nodeMoveDTO.getParent().getId());
        }
    }

}
