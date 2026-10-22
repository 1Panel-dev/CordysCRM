package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeLink;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 查询业务资源当前审批实例的提交人和首节点状态。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalResourceQueryService {
    @Resource
    private ApprovalInstanceService approvalInstanceService;
    @Resource
    private BaseMapper<ApprovalNodeLink> nodeLinkMapper;
    @Resource
    private BaseMapper<ApprovalNode> approvalNodeMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Lazy
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;

    /**
     * 获取审批中的业务资源是否第一个审批节点已通过
     *
     * @param resourceIds 资源ID集合
     * @return 是否第一个审批节点已通过
     */
    public Map<String, Boolean> getApprovingResourceFirstNodeApproved(List<String> resourceIds, String currentOrgId) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Map.of();
        }
        Map<String, Boolean> firstNodeApproved = new HashMap<>(resourceIds.size());
        List<ApprovalInstance> latestInstances = approvalInstanceService.getLatestInstances(resourceIds);
        if (CollectionUtils.isEmpty(latestInstances)) {
            return Map.of();
        }
        List<String> flowVersionIds = latestInstances.stream().map(ApprovalInstance::getFlowVersionId).distinct()
                .toList();
        List<ApprovalNodeLink> nodeLinks = nodeLinkMapper.selectListByLambda(
                new LambdaQueryWrapper<ApprovalNodeLink>().in(ApprovalNodeLink::getFlowVersionId, flowVersionIds));
        List<ApprovalNode> allNodes = approvalNodeMapper.selectListByLambda(
                new LambdaQueryWrapper<ApprovalNode>().in(ApprovalNode::getFlowVersionId, flowVersionIds));
        List<ApprovalTask> allTasks = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>()
                .in(ApprovalTask::getInstanceId, latestInstances.stream().map(ApprovalInstance::getId).toList()));
        Map<String, String> nodeTypeMap = allNodes.stream()
                .collect(Collectors.toMap(ApprovalNode::getId, ApprovalNode::getNodeType));
        Map<String, List<String>> nodeLinkMap = nodeLinks.stream()
                .filter(nodeLink -> nodeTypeMap.containsKey(nodeLink.getToNodeId()) && ApprovalNodeTypeEnum
                        .valueOf(nodeTypeMap.get(nodeLink.getToNodeId())) != ApprovalNodeTypeEnum.END)
                .collect(Collectors.groupingBy(ApprovalNodeLink::getToNodeId,
                        Collectors.mapping(ApprovalNodeLink::getFromNodeId, Collectors.toList())));
        latestInstances.forEach(latestInstance -> {
            if (isFirstApproverNode(nodeLinkMap, nodeTypeMap, latestInstance.getCurrentNodeId())) {
                // 当前审批实例处于第一个审批节点, 所以第一个节点为审批中
                boolean multiApprover = approvalFlowService.isCurrentNodeMultiApprover(
                        latestInstance.getCurrentNodeId(), latestInstance.getSubmitterId(), currentOrgId);
                if (!multiApprover) {
                    // 单人审批, 判断是否存在加签通过
                    Integer nodeRound = extApprovalInstanceMapper.getNodeRound(latestInstance.getId(),
                            latestInstance.getCurrentNodeId());
                    Optional<ApprovalTask> signApproved = allTasks.stream()
                            .filter(task -> Strings.CI.equals(task.getType(), ApprovalTaskType.SN.name())
                                    && Strings.CI.equals(task.getNodeId(), latestInstance.getCurrentNodeId())
                                    && Strings.CI.equals(task.getInstanceId(), latestInstance.getId())
                                    && nodeRound.equals(task.getNodeRound())
                                    && Strings.CI.equals(task.getStatus(), ApprovalStatus.APPROVED.name()))
                            .findAny();
                    if (signApproved.isPresent()) {
                        firstNodeApproved.put(latestInstance.getResourceId(), true);
                        return;
                    }
                }
                firstNodeApproved.put(latestInstance.getResourceId(), false);
            } else {
                // 当前审批实例不是处于第一个审批节点, 所以第一个节点已完成
                firstNodeApproved.put(latestInstance.getResourceId(), true);
            }
        });
        return firstNodeApproved;
    }

    /**
     * 获取审批中的业务资源对应的提审人
     * <p>
     * 仅审批中(APPROVING)的资源存在进行中的审批实例, 没有实例的资源不会出现在结果中。
     *
     * @param resourceIds 资源ID集合
     * @return 资源ID -> 提审人ID
     */
    public Map<String, String> getApprovingResourceSubmitterIds(List<String> resourceIds) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Map.of();
        }
        return approvalInstanceService.getLatestInstances(resourceIds).stream()
                .filter(instance -> StringUtils.isNotBlank(instance.getSubmitterId()))
                .collect(Collectors.toMap(ApprovalInstance::getResourceId, ApprovalInstance::getSubmitterId));
    }

    /**
     * 获取审批中业务资源的提审人, 详情场景使用
     *
     * @param resourceId 资源ID
     * @return 提审人ID, 无进行中的审批实例时返回 null
     */
    public String getApprovingResourceSubmitterId(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            return null;
        }
        return getApprovingResourceSubmitterIds(List.of(resourceId)).get(resourceId);
    }

    /**
     * 当前节点是否第一个审批节点
     *
     * @param nodeLinkMap   节点链接信息(toNodeId -> fromNodeId列表)
     * @param nodeTypeMap   节点类型集合
     * @param currentNodeId 当前节点ID
     * @return 是否第一个审批节点
     */
    private boolean isFirstApproverNode(Map<String, List<String>> nodeLinkMap, Map<String, String> nodeTypeMap,
            String currentNodeId) {
        return isFirstApproverNodeRecursive(nodeLinkMap, nodeTypeMap, currentNodeId, new HashSet<>());
    }

    /**
     * 递归判断当前节点是否第一个审批节点
     *
     * @param nodeLinkMap 节点链接信息
     * @param nodeTypeMap 节点类型集合
     * @param nodeId      当前节点ID
     * @param visited     已访问的节点集合(防止循环)
     * @return 是否第一个审批节点
     */
    private boolean isFirstApproverNodeRecursive(Map<String, List<String>> nodeLinkMap, Map<String, String> nodeTypeMap,
            String nodeId, Set<String> visited) {
        // 防止循环引用
        if (visited.contains(nodeId)) {
            return false;
        }
        visited.add(nodeId);

        // 获取当前节点的前驱节点列表
        List<String> preNodeIds = nodeLinkMap.get(nodeId);
        if (CollectionUtils.isEmpty(preNodeIds)) {
            // 没有前驱节点，说明是第一个审批节点
            return true;
        }

        // 遍历所有前驱节点
        for (String preNodeId : preNodeIds) {
            if (StringUtils.isBlank(preNodeId)) {
                continue;
            }
            String nodeType = nodeTypeMap.get(preNodeId);
            if (StringUtils.isBlank(nodeType)) {
                continue;
            }
            ApprovalNodeTypeEnum nodeTypeEnum = ApprovalNodeTypeEnum.valueOf(nodeType);
            if (nodeTypeEnum == ApprovalNodeTypeEnum.START) {
                // 找到 START 节点，说明当前节点不是第一个审批节点
                return true;
            }
            if (nodeTypeEnum == ApprovalNodeTypeEnum.CONDITION || nodeTypeEnum == ApprovalNodeTypeEnum.DEFAULT) {
                // 继续往前找
                boolean isFirst = isFirstApproverNodeRecursive(nodeLinkMap, nodeTypeMap, preNodeId, visited);
                if (isFirst) {
                    return true;
                }
            }
        }
        return false;
    }
}
