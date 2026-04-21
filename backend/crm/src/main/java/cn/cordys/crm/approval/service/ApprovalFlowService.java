package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.response.*;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.approval.log.ApprovalFlowLogDTO;
import cn.cordys.crm.approval.mapper.ExtApprovalFlowMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalFlowService {

    @Resource
    private BaseMapper<ApprovalFlow> approvalFlowMapper;
    @Resource
    private ExtApprovalFlowMapper extApprovalFlowMapper;
    @Resource
    private BaseMapper<ApprovalFlowBlob> approvalFlowBlobMapper;
    @Resource
    private BaseMapper<ApprovalNode> approvalNodeMapper;
    @Resource
    private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;
    @Resource
    private BaseMapper<ApprovalNodeCondition> approvalNodeConditionMapper;
    @Resource
    private BaseMapper<ApprovalNodeLink> approvalNodeLinkMapper;

    /**
     * 分页查询审批流列表
     */
    public Pager<List<ApprovalFlowListResponse>> list(ApprovalFlowPageRequest request, String organizationId) {
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ApprovalFlowListResponse> responses = extApprovalFlowMapper.list(request, organizationId);
        Page<ApprovalFlowListResponse> page = (Page<ApprovalFlowListResponse>) responses;
        return PageUtils.setPageInfo(page, responses);
    }

    /**
     * 获取审批流详情
     */
    public ApprovalFlowDetailResponse getDetail(String id, String organizationId) {
        ApprovalFlow flow = approvalFlowMapper.selectByPrimaryKey(id);
        if (flow == null || !flow.getOrganizationId().equals(organizationId)) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        ApprovalFlowDetailResponse response = convertToDetailResponse(flow);

        // 查询大字段表
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(id);
        if (blob != null) {
            response.setDescription(blob.getDescription());
            response.setStatusPermissions(blob.getStatusPermissions());
        }

        // 查询节点配置并构建树状结构
        List<ApprovalNodeResponse> nodes = buildNodeTree(id);
        response.setNodes(nodes);

        return response;
    }

    /**
     * 新建审批流
     */
    @OperationLog(module = LogModule.APPROVAL_FLOW, type = LogType.ADD, resourceName = "{#request.name}")
    public ApprovalFlow add(ApprovalFlowAddRequest request, String userId, String organizationId) {
        // 校验同一表单只能有一个启用的审批流
        if (Boolean.TRUE.equals(request.getEnable())) {
            checkOnlyOneEnableFlow(request.getFormType(), organizationId);
        }

        ApprovalFlow flow = BeanUtils.copyBean(new ApprovalFlow(), request);
        flow.setId(IDGenerator.nextStr());
        flow.setNumber(generateFlowNumber(request.getFormType(), organizationId));
        flow.setExecuteTiming(JSON.toJSONString(request.getExecuteTiming()));
        flow.setCreateUser(userId);
        flow.setCreateTime(System.currentTimeMillis());
        flow.setUpdateUser(userId);
        flow.setUpdateTime(System.currentTimeMillis());
        flow.setOrganizationId(organizationId);

        approvalFlowMapper.insert(flow);

        // 保存大字段表
        ApprovalFlowBlob blob = new ApprovalFlowBlob();
        blob.setId(flow.getId());
        blob.setDescription(request.getDescription());
        blob.setStatusPermissions(request.getStatusPermissions());
        approvalFlowBlobMapper.insert(blob);

        // 保存节点配置
        if (CollectionUtils.isNotEmpty(request.getNodes())) {
            saveNodes(request.getNodes(), flow.getId(), userId);
        }

        // 设置日志上下文
        ApprovalFlowLogDTO approvalFlowLogDTO = buildAddLogDTO(request, flow);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(flow.getId())
                        .modifiedValue(approvalFlowLogDTO)
                        .build()
        );

        return flow;
    }

    /**
     * 更新审批流
     */
    @OperationLog(module = LogModule.APPROVAL_FLOW, type = LogType.UPDATE, resourceId = "{#request.id}")
    public ApprovalFlow update(ApprovalFlowUpdateRequest request, String userId, String organizationId) {
        ApprovalFlow existing = approvalFlowMapper.selectByPrimaryKey(request.getId());
        if (existing == null || !existing.getOrganizationId().equals(organizationId)) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        // 获取原始日志DTO
        ApprovalFlowLogDTO originLogDTO = buildOriginLogDTO(existing);

        // 校验同一表单只能有一个启用的审批流
        if (Boolean.TRUE.equals(request.getEnable())) {
            checkOnlyOneEnableFlowExcludeId(existing.getFormType(), organizationId, request.getId());
        }

        ApprovalFlow flow = BeanUtils.copyBean(new ApprovalFlow(), request);
        flow.setExecuteTiming(JSON.toJSONString(request.getExecuteTiming()));
        flow.setUpdateUser(userId);
        flow.setUpdateTime(System.currentTimeMillis());

        approvalFlowMapper.updateById(flow);

        // 更新大字段表
        ApprovalFlowBlob blob = new ApprovalFlowBlob();
        blob.setId(flow.getId());
        blob.setDescription(request.getDescription());
        blob.setStatusPermissions(request.getStatusPermissions());
        approvalFlowBlobMapper.updateById(blob);

        // 删除原有节点配置并重新保存
        deleteNodesByFlowId(flow.getId());
        if (CollectionUtils.isNotEmpty(request.getNodes())) {
            saveNodes(request.getNodes(), flow.getId(), userId);
        }

        // 获取更新后的日志DTO
        ApprovalFlow updatedFlow = approvalFlowMapper.selectByPrimaryKey(request.getId());
        ApprovalFlowLogDTO modifiedLogDTO = buildModifiedLogDTO(request, updatedFlow);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originLogDTO)
                        .modifiedValue(modifiedLogDTO)
                        .build()
        );

        return flow;
    }

    /**
     * 删除审批流
     */
    @OperationLog(module = LogModule.APPROVAL_FLOW, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String organizationId) {
        ApprovalFlow flow = approvalFlowMapper.selectByPrimaryKey(id);
        if (flow == null || !flow.getOrganizationId().equals(organizationId)) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        // 删除主表
        approvalFlowMapper.deleteByPrimaryKey(id);
        // 删除大字段表
        approvalFlowBlobMapper.deleteByPrimaryKey(id);
        // 删除节点配置
        deleteNodesByFlowId(id);

        // 设置日志上下文
        OperationLogContext.setResourceName(flow.getName());
    }

    /**
     * 启用/禁用审批流
     */
    public void updateEnable(String id, Boolean enable, String userId, String organizationId) {
        ApprovalFlow flow = approvalFlowMapper.selectByPrimaryKey(id);
        if (flow == null || !flow.getOrganizationId().equals(organizationId)) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        if (Boolean.TRUE.equals(enable)) {
            checkOnlyOneEnableFlowExcludeId(flow.getFormType(), organizationId, id);
        }

        ApprovalFlow update = new ApprovalFlow();
        update.setId(id);
        update.setEnable(enable);
        update.setUpdateUser(userId);
        update.setUpdateTime(System.currentTimeMillis());
        approvalFlowMapper.updateById(update);
    }

    /**
     * 生成流程编码
     */
    private String generateFlowNumber(String formType, String organizationId) {
        String prefix = getNumberPrefix(formType);
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setOrganizationId(organizationId);
        criteria.setFormType(formType);
        List<ApprovalFlow> flows = approvalFlowMapper.select(criteria);
        int maxNum = 0;
        for (ApprovalFlow flow : flows) {
            if (StringUtils.isNotBlank(flow.getNumber()) && flow.getNumber().startsWith(prefix)) {
                try {
                    String numStr = flow.getNumber().substring(flow.getNumber().lastIndexOf("-") + 1);
                    int num = Integer.parseInt(numStr);
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("%s-%03d", prefix, maxNum + 1);
    }

    private String getNumberPrefix(String formType) {
        try {
            return ApprovalFormTypeEnum.valueOf(formType).getPrefix();
        } catch (IllegalArgumentException e) {
            return "APV";
        }
    }

    /**
     * 校验同一表单只能有一个启用的审批流
     */
    private void checkOnlyOneEnableFlow(String formType, String organizationId) {
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setFormType(formType);
        criteria.setOrganizationId(organizationId);
        criteria.setEnable(true);
        if (approvalFlowMapper.exist(criteria)) {
            throw new GenericException(SystemResultCode.APPROVAL_FLOW_DUPLICATE);
        }
    }

    private void checkOnlyOneEnableFlowExcludeId(String formType, String organizationId, String excludeId) {
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setFormType(formType);
        criteria.setOrganizationId(organizationId);
        criteria.setEnable(true);
        List<ApprovalFlow> flows = approvalFlowMapper.select(criteria);
        for (ApprovalFlow flow : flows) {
            if (!flow.getId().equals(excludeId)) {
                throw new GenericException(SystemResultCode.APPROVAL_FLOW_DUPLICATE);
            }
        }
    }

    private ApprovalFlowDetailResponse convertToDetailResponse(ApprovalFlow flow) {
        ApprovalFlowDetailResponse response = BeanUtils.copyBean(new ApprovalFlowDetailResponse(), flow);
        return response;
    }

    private List<ApprovalNodeResponse> getNodesByFlowId(String flowId) {
        ApprovalNode criteria = new ApprovalNode();
        criteria.setFlowId(flowId);
        List<ApprovalNode> nodes = approvalNodeMapper.select(criteria);
        return nodes.stream().map(node -> {
            // 查询审批人节点配置
            if (ApprovalNodeTypeEnum.APPROVER.name().equals(node.getNodeType())) {
                ApprovalNodeApprover approverCriteria = new ApprovalNodeApprover();
                approverCriteria.setId(node.getId());
                approverCriteria.setFlowId(flowId);
                ApprovalNodeApprover approverNode = approvalNodeApproverMapper.selectOne(approverCriteria);
                if (approverNode != null) {
                    ApprovalNodeApproverResponse approverResponse = BeanUtils.copyBean(
                            new ApprovalNodeApproverResponse(), node);
                    BeanUtils.copyBean(approverResponse, approverNode);
                    return approverResponse;
                }
            }
            // 查询条件节点配置
            if (ApprovalNodeTypeEnum.CONDITION.name().equals(node.getNodeType())) {
                ApprovalNodeCondition conditionCriteria = new ApprovalNodeCondition();
                conditionCriteria.setId(node.getId());
                conditionCriteria.setFlowId(flowId);
                ApprovalNodeCondition conditionNode = approvalNodeConditionMapper.selectOne(conditionCriteria);
                if (conditionNode != null) {
                    ApprovalNodeConditionResponse conditionResponse = BeanUtils.copyBean(
                            new ApprovalNodeConditionResponse(), node);
                    BeanUtils.copyBean(conditionResponse, conditionNode);
                    return conditionResponse;
                }
            }
            // 其他类型节点
            ApprovalNodeResponse response = BeanUtils.copyBean(new ApprovalNodeResponse(), node);
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 构建节点树状结构
     */
    private List<ApprovalNodeResponse> buildNodeTree(String flowId) {
        // 获取所有节点
        List<ApprovalNodeResponse> allNodes = getNodesByFlowId(flowId);

        // 获取节点连接关系
        ApprovalNodeLink linkCriteria = new ApprovalNodeLink();
        linkCriteria.setFlowId(flowId);
        List<ApprovalNodeLink> links = approvalNodeLinkMapper.select(linkCriteria);

        // 构建父子关系映射
        Map<String, List<String>> parentChildMap = links.stream()
                .collect(Collectors.groupingBy(
                        ApprovalNodeLink::getFromNodeId,
                        Collectors.mapping(ApprovalNodeLink::getToNodeId, Collectors.toList())
                ));

        // 构建节点ID到节点的映射
        Map<String, ApprovalNodeResponse> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(ApprovalNodeResponse::getId, n -> n));

        // 找出根节点（START节点或没有父节点的节点）
        Set<String> childIds = links.stream()
                .map(ApprovalNodeLink::getToNodeId)
                .collect(Collectors.toSet());

        List<ApprovalNodeResponse> rootNodes = allNodes.stream()
                .filter(node -> !childIds.contains(node.getId()))
                .sorted(Comparator.comparing(ApprovalNodeResponse::getSort))
                .collect(Collectors.toList());

        // 递归构建树状结构
        for (ApprovalNodeResponse rootNode : rootNodes) {
            buildChildren(rootNode, parentChildMap, nodeMap);
        }

        return rootNodes;
    }

    /**
     * 递归构建子节点
     */
    private void buildChildren(ApprovalNodeResponse node, Map<String, List<String>> parentChildMap,
                                Map<String, ApprovalNodeResponse> nodeMap) {
        List<String> childIds = parentChildMap.get(node.getId());
        if (CollectionUtils.isNotEmpty(childIds)) {
            List<ApprovalNodeResponse> children = childIds.stream()
                    .map(nodeMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ApprovalNodeResponse::getSort))
                    .collect(Collectors.toList());

            node.setChildren(children);

            // 递归构建子节点的子节点
            for (ApprovalNodeResponse child : children) {
                buildChildren(child, parentChildMap, nodeMap);
            }
        }
    }

    private void saveNodes(List<ApprovalNodeRequest> nodes, String flowId, String userId) {
        for (ApprovalNodeRequest nodeRequest : nodes) {
            saveNode(nodeRequest, flowId, userId, null);
        }
    }

    /**
     * 递归保存节点及其子节点
     */
    private void saveNode(ApprovalNodeRequest nodeRequest, String flowId, String userId, String parentId) {
        String nodeId = StringUtils.isNotBlank(nodeRequest.getId()) ? nodeRequest.getId() : IDGenerator.nextStr();

        // 保存节点基本信息
        ApprovalNode node = BeanUtils.copyBean(new ApprovalNode(), nodeRequest);
        node.setId(nodeId);
        node.setFlowId(flowId);
        approvalNodeMapper.insert(node);

        // 保存节点连接（如果有父节点）
        if (StringUtils.isNotBlank(parentId)) {
            ApprovalNodeLink link = new ApprovalNodeLink();
            link.setId(IDGenerator.nextStr());
            link.setFlowId(flowId);
            link.setFromNodeId(parentId);
            link.setToNodeId(nodeId);
            link.setSort(nodeRequest.getSort());
            approvalNodeLinkMapper.insert(link);
        }

        // 保存审批人节点配置
        if (nodeRequest instanceof ApprovalNodeApproverRequest) {
            ApprovalNodeApproverRequest approverRequest = (ApprovalNodeApproverRequest) nodeRequest;
            ApprovalNodeApprover approverNode = BeanUtils.copyBean(new ApprovalNodeApprover(), approverRequest,
                    "cc", "approver");
            approverNode.setId(nodeId);
            approverNode.setFlowId(flowId);
            approverNode.setCc(JSON.toJSONString(approverRequest.getCc()));
            approverNode.setApprover(JSON.toJSONString(approverRequest.getApprover()));
            approvalNodeApproverMapper.insert(approverNode);
        }
        // 保存条件节点配置
        else if (nodeRequest instanceof ApprovalNodeConditionRequest) {
            ApprovalNodeConditionRequest conditionRequest = (ApprovalNodeConditionRequest) nodeRequest;
            ApprovalNodeCondition conditionNode = BeanUtils.copyBean(new ApprovalNodeCondition(), conditionRequest,
                    "rules");
            conditionNode.setId(nodeId);
            conditionNode.setFlowId(flowId);
            conditionNode.setRuleExpression(JSON.toJSONString(conditionRequest.getRules()));
            approvalNodeConditionMapper.insert(conditionNode);
        }

        // 递归保存子节点
        if (CollectionUtils.isNotEmpty(nodeRequest.getChildren())) {
            for (ApprovalNodeRequest childRequest : nodeRequest.getChildren()) {
                saveNode(childRequest, flowId, userId, nodeId);
            }
        }
    }

    private void deleteNodesByFlowId(String flowId) {
        // 删除节点连接
        approvalNodeLinkMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalNodeLink>()
                .eq(ApprovalNodeLink::getFlowId, flowId));

        // 删除审批人节点配置
        approvalNodeApproverMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalNodeApprover>()
                .eq(ApprovalNodeApprover::getFlowId, flowId));

        // 删除条件节点配置
        approvalNodeConditionMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalNodeCondition>()
                .eq(ApprovalNodeCondition::getFlowId, flowId));

        // 删除节点
        approvalNodeMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalNode>()
                .eq(ApprovalNode::getFlowId, flowId));
    }

    private ApprovalFlowLogDTO buildAddLogDTO(ApprovalFlowAddRequest request, ApprovalFlow flow) {
        ApprovalFlowLogDTO logDTO = BeanUtils.copyBean(new ApprovalFlowLogDTO(), flow);
        logDTO.setDescription(request.getDescription());
        return logDTO;
    }

    private ApprovalFlowLogDTO buildOriginLogDTO(ApprovalFlow existing) {
        ApprovalFlowLogDTO logDTO = BeanUtils.copyBean(new ApprovalFlowLogDTO(), existing);
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(existing.getId());
        if (blob != null) {
            logDTO.setDescription(blob.getDescription());
        }
        return logDTO;
    }

    private ApprovalFlowLogDTO buildModifiedLogDTO(ApprovalFlowUpdateRequest request, ApprovalFlow flow) {
        ApprovalFlowLogDTO logDTO = BeanUtils.copyBean(new ApprovalFlowLogDTO(), flow);
        logDTO.setDescription(request.getDescription());
        return logDTO;
    }
}