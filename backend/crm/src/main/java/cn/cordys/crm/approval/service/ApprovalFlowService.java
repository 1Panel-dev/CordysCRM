package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.Permission;
import cn.cordys.common.permission.PermissionDefinitionItem;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
import cn.cordys.crm.approval.dto.ApproverConfigDTO;
import cn.cordys.crm.approval.dto.FieldPermissionDTO;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.*;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.approval.log.ApprovalFlowLogDTO;
import cn.cordys.crm.approval.mapper.ExtApprovalFlowMapper;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    @Resource
    private RoleService roleService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据表单类型获取审批流状态权限配置
     */
    public StatusPermissionSettingResponse getStatusPermissionsByFormType(String formType, String organizationId) {
        StatusPermissionSettingResponse response = new StatusPermissionSettingResponse();
        
        // 获取权限列表
        List<OptionDTO> permissions = getResourcePermissions(getPermissionsByFormType(formType));
        response.setPermissions(permissions);
        
        // 查询该表单类型对应的审批流
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setFormType(formType);
        criteria.setEnable(true);
        criteria.setOrganizationId(organizationId);
        List<ApprovalFlow> flows = approvalFlowMapper.select(criteria);

        if (CollectionUtils.isEmpty(flows)) {
            response.setStatusPermissions(List.of());
            return response;
        }

        // 优先使用启用的审批流，如果没有则使用第一个
        ApprovalFlow targetFlow = flows.stream()
                .filter(ApprovalFlow::getEnable)
                .findFirst()
                .orElse(flows.get(0));

        // 查询大字段表获取状态权限配置
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(targetFlow.getId());
        if (blob == null || StringUtils.isBlank(blob.getStatusPermissions())) {
            response.setStatusPermissions(List.of());
            return response;
        }

        // 解析状态权限配置
        response.setStatusPermissions(parseStatusPermissions(permissions, blob.getStatusPermissions()));
        return response;
    }

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

        List<Permission> permissions = getPermissionsByFormType(flow.getFormType());

        // 设置对应资源的权限列表
        response.setPermissions(getResourcePermissions(permissions));

        // 查询大字段表
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(id);
        if (blob != null) {
            response.setDescription(blob.getDescription());
            response.setStatusPermissions(parseStatusPermissions(response.getPermissions(), blob.getStatusPermissions()));
        }

        // 查询节点配置并构建树状结构
        List<ApprovalNodeResponse> nodes = buildNodeTree(id);
        response.setNodes(nodes);

        return response;
    }

    /**
     * 根据表单类型获取审批流信息（不含节点配置）
     */
    public ApprovalFlowByFormTypeResponse getByFormType(String formType, String organizationId) {
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setFormType(formType);
        criteria.setEnable(true);
        criteria.setOrganizationId(organizationId);
        List<ApprovalFlow> flows = approvalFlowMapper.select(criteria);

        ApprovalFlow targetFlow = flows.stream()
                .filter(ApprovalFlow::getEnable)
                .findFirst()
                .orElse(null);

        if (targetFlow == null) {
            return null;
        }

        ApprovalFlowByFormTypeResponse response = BeanUtils.copyBean(new ApprovalFlowByFormTypeResponse(), targetFlow);

        List<Permission> permissions = getPermissionsByFormType(targetFlow.getFormType());
        response.setPermissions(getResourcePermissions(permissions));

        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(targetFlow.getId());
        if (blob != null) {
            response.setDescription(blob.getDescription());
            response.setStatusPermissions(parseStatusPermissions(response.getPermissions(), blob.getStatusPermissions()));
        }

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
        blob.setStatusPermissions(JSON.toJSONString(request.getStatusPermissions()));
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
        blob.setStatusPermissions(JSON.toJSONString(request.getStatusPermissions()));
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
        String key = "approval_flow_num:" + organizationId + ":" + formType;
        long seq = stringRedisTemplate.opsForValue().increment(key);
        return String.format("%s-%05d", prefix, seq);
    }

    private String getNumberPrefix(String formType) {
        try {
            return ApprovalFormTypeEnum.valueOf(formType.toUpperCase()).getPrefix();
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
                    // 解析 JSON 字段为对象
                    parseApproverNodeFields(approverNode, approverResponse);
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
     * 解析审批人节点 JSON 字段为对象
     */
    private void parseApproverNodeFields(ApprovalNodeApprover approverNode, ApprovalNodeApproverResponse response) {
        if (StringUtils.isNotBlank(approverNode.getCc())) {
            response.setCc(JSON.parseArray(approverNode.getCc(), ApproverConfigDTO.class));
        }
        if (StringUtils.isNotBlank(approverNode.getApprover())) {
            response.setApprover(JSON.parseArray(approverNode.getApprover(), ApproverConfigDTO.class));
        }
        if (StringUtils.isNotBlank(approverNode.getPassPostConfig())) {
            response.setPassPostConfig(JSON.parseObject(approverNode.getPassPostConfig(), ApprovalPostConfigDTO.class));
        }
        if (StringUtils.isNotBlank(approverNode.getRejectPostConfig())) {
            response.setRejectPostConfig(JSON.parseObject(approverNode.getRejectPostConfig(), ApprovalPostConfigDTO.class));
        }
        if (StringUtils.isNotBlank(approverNode.getFieldPermissions())) {
            response.setFieldPermissions(JSON.parseArray(approverNode.getFieldPermissions(), FieldPermissionDTO.class));
        }
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

    /**
     * 批量保存节点配置
     * 先递归收集所有节点信息，然后批量插入，提高性能
     */
    private void saveNodes(List<ApprovalNodeRequest> nodes, String flowId, String userId) {
        // 用于收集节点、连接和配置信息
        List<ApprovalNode> allNodes = new ArrayList<>();
        List<ApprovalNodeLink> allLinks = new ArrayList<>();
        List<ApprovalNodeApprover> allApproverNodes = new ArrayList<>();
        List<ApprovalNodeCondition> allConditionNodes = new ArrayList<>();

        // 递归收集所有节点信息
        for (ApprovalNodeRequest nodeRequest : nodes) {
            collectNodeInfo(nodeRequest, flowId, userId, null, allNodes, allLinks, allApproverNodes, allConditionNodes);
        }

        // 批量插入
        if (CollectionUtils.isNotEmpty(allNodes)) {
            approvalNodeMapper.batchInsert(allNodes);
        }
        if (CollectionUtils.isNotEmpty(allLinks)) {
            approvalNodeLinkMapper.batchInsert(allLinks);
        }
        if (CollectionUtils.isNotEmpty(allApproverNodes)) {
            approvalNodeApproverMapper.batchInsert(allApproverNodes);
        }
        if (CollectionUtils.isNotEmpty(allConditionNodes)) {
            approvalNodeConditionMapper.batchInsert(allConditionNodes);
        }
    }

    /**
     * 递归收集节点信息到列表中
     */
    private void collectNodeInfo(ApprovalNodeRequest nodeRequest, String flowId, String userId, String parentId,
                                  List<ApprovalNode> allNodes, List<ApprovalNodeLink> allLinks,
                                  List<ApprovalNodeApprover> allApproverNodes, List<ApprovalNodeCondition> allConditionNodes) {
        String nodeId = StringUtils.isNotBlank(nodeRequest.getId()) ? nodeRequest.getId() : IDGenerator.nextStr();

        // 收集节点基本信息
        ApprovalNode node = BeanUtils.copyBean(new ApprovalNode(), nodeRequest);
        node.setId(nodeId);
        node.setFlowId(flowId);
        allNodes.add(node);

        // 收集节点连接信息（如果有父节点）
        if (StringUtils.isNotBlank(parentId)) {
            ApprovalNodeLink link = new ApprovalNodeLink();
            link.setId(IDGenerator.nextStr());
            link.setFlowId(flowId);
            link.setFromNodeId(parentId);
            link.setToNodeId(nodeId);
            link.setSort(nodeRequest.getSort());
            allLinks.add(link);
        }

        // 收集审批人节点配置
        if (nodeRequest instanceof ApprovalNodeApproverRequest) {
            ApprovalNodeApproverRequest approverRequest = (ApprovalNodeApproverRequest) nodeRequest;
            ApprovalNodeApprover approverNode = BeanUtils.copyBean(new ApprovalNodeApprover(), approverRequest,
                    "cc", "approver", "passPostConfig", "rejectPostConfig", "fieldPermissions");
            approverNode.setId(nodeId);
            approverNode.setFlowId(flowId);
            approverNode.setCc(JSON.toJSONString(approverRequest.getCc()));
            approverNode.setApprover(JSON.toJSONString(approverRequest.getApprover()));
            approverNode.setPassPostConfig(JSON.toJSONString(approverRequest.getPassPostConfig()));
            approverNode.setRejectPostConfig(JSON.toJSONString(approverRequest.getRejectPostConfig()));
            approverNode.setFieldPermissions(JSON.toJSONString(approverRequest.getFieldPermissions()));
            allApproverNodes.add(approverNode);
        }
        // 收集条件节点配置
        else if (nodeRequest instanceof ApprovalNodeConditionRequest) {
            ApprovalNodeConditionRequest conditionRequest = (ApprovalNodeConditionRequest) nodeRequest;
            ApprovalNodeCondition conditionNode = BeanUtils.copyBean(new ApprovalNodeCondition(), conditionRequest,
                    "rules");
            conditionNode.setId(nodeId);
            conditionNode.setFlowId(flowId);
            conditionNode.setConditionConfig(JSON.toJSONString(conditionRequest.getConditionConfig()));
            allConditionNodes.add(conditionNode);
        }

        // 递归收集子节点
        if (CollectionUtils.isNotEmpty(nodeRequest.getChildren())) {
            for (ApprovalNodeRequest childRequest : nodeRequest.getChildren()) {
                collectNodeInfo(childRequest, flowId, userId, nodeId, allNodes, allLinks, allApproverNodes, allConditionNodes);
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

    /**
     * 获取对应资源的权限列表
     */
    private List<OptionDTO> getResourcePermissions(List<Permission> permissions) {
        return permissions.stream()
                .map(p -> new OptionDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    private List<Permission> getPermissionsByFormType(String formType) {
        List<PermissionDefinitionItem> permissionSetting = roleService.getPermissionSetting();
        String permissionId = ApprovalFormTypeEnum.getByValue(formType).getPermissionId();
        List<Permission> permissions = findPermissionsByPermissionId(permissionSetting, permissionId);
        if (permissions == null) {
            return List.of();
        }
        return permissions;
    }

    /**
     * 解析状态权限配置
     */
    private List<StatusPermissionDTO> parseStatusPermissions(List<OptionDTO> permissions, String statusPermissions) {
        if (StringUtils.isBlank(statusPermissions)) {
            return null;
        }
        // 解析已保存的状态权限配置
        List<StatusPermissionDTO> savedPermissions = JSON.parseArray(statusPermissions, StatusPermissionDTO.class);

        // 获取所有审批状态
        Set<String> approvalStatuses = savedPermissions.stream()
                .map(StatusPermissionDTO::getApprovalStatus)
                .collect(Collectors.toSet());

        // 构建已保存权限的映射：(审批状态, 权限ID) -> StatusPermissionDTO
        Map<String, StatusPermissionDTO> savedPermissionMap = savedPermissions.stream()
                .collect(Collectors.toMap(
                        p -> p.getApprovalStatus() + ":" + p.getPermission(),
                        p -> p));

        // 更新权限名称并补充缺失的权限
        List<StatusPermissionDTO> updatedPermissions = new ArrayList<>();
        for (String approvalStatus : approvalStatuses) {
            for (OptionDTO permission : permissions) {
                String key = approvalStatus + ":" + permission.getId();
                StatusPermissionDTO item = savedPermissionMap.get(key);
                if (item != null) {
                    // 更新权限名称
                    item.setEnabled(true);
                    updatedPermissions.add(item);
                } else {
                    // 添加缺失的权限，默认不启用
                    StatusPermissionDTO newItem = new StatusPermissionDTO();
                    newItem.setApprovalStatus(approvalStatus);
                    newItem.setPermission(permission.getId());
                    newItem.setEnabled(false);
                    updatedPermissions.add(newItem);
                }
            }
        }

        return updatedPermissions;
    }

    /**
     * 获取下一个节点
     * 条件节点根据字段值进行匹配，其他类型节点直接返回
     */
    public ApprovalNodeResponse getNextNode(String nodeId, List<BaseModuleFieldValue> fieldValues) {
        List<ApprovalNodeResponse> nextNodes = getNextNodes(nodeId);

        if (CollectionUtils.isEmpty(nextNodes)) {
            return null;
        }

        // 检查是否存在条件节点
        boolean hasConditionNode = nextNodes.stream()
                .anyMatch(n -> ApprovalNodeTypeEnum.CONDITION.name().equals(n.getNodeType()));

        if (!hasConditionNode) {
            // 非条件节点，通常只有一个，直接返回第一个
            return nextNodes.getFirst();
        }

        // 条件节点匹配逻辑：按 sort 顺序依次匹配所有条件节点
        // 条件节点 + 默认节点组成 if-else 组合
        ApprovalNodeResponse defaultNode = null;

        for (ApprovalNodeResponse nextNode : nextNodes) {
            if (nextNode instanceof ApprovalNodeConditionResponse conditionNode) {
                // 匹配条件节点，如果匹配成功则立即返回
                if (matchCondition(conditionNode.getConditionConfig(), fieldValues)) {
                    return conditionNode;
                }
            } else if (ApprovalNodeTypeEnum.DEFAULT.name().equals(nextNode.getNodeType())) {
                // 记录 DEFAULT 节点，等所有条件节点检查完毕后再返回
                defaultNode = nextNode;
            }
        }

        // 所有条件节点都不匹配，返回 DEFAULT 节点（如果存在）
        return defaultNode;
    }

    /**
     * 匹配条件
     */
    private boolean matchCondition(CombineSearch combineSearch, List<BaseModuleFieldValue> fieldValues) {
        if (combineSearch == null || CollectionUtils.isEmpty(combineSearch.getConditions())) {
            return false;
        }

        // 构建字段值映射
        Map<String, Object> fieldValueMap = fieldValues.stream()
                .filter(BaseModuleFieldValue::valid)
                .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue));

        List<FilterCondition> conditions = combineSearch.getConditions();
        String searchMode = combineSearch.getSearchMode();

        // 根据匹配模式进行条件判断
        if (CombineSearch.SearchMode.AND.name().equals(searchMode)) {
            // AND 模式：所有条件都必须满足
            return conditions.stream().allMatch(condition -> matchSingleCondition(condition, fieldValueMap));
        } else {
            // OR 模式：任一条件满足即可
            return conditions.stream().anyMatch(condition -> matchSingleCondition(condition, fieldValueMap));
        }
    }

    /**
     * 匹配单个条件
     */
    private boolean matchSingleCondition(FilterCondition condition, Map<String, Object> fieldValueMap) {
        String fieldName = condition.getName();
        Object actualValue = fieldValueMap.get(fieldName);

        // 获取操作符枚举
        FilterCondition.CombineConditionOperator operator;
        try {
            operator = FilterCondition.CombineConditionOperator.valueOf(condition.getOperator());
        } catch (IllegalArgumentException e) {
            return false;
        }

        // 处理空值判断操作符
        if (operator == FilterCondition.CombineConditionOperator.EMPTY) {
            return actualValue == null;
        }
        if (operator == FilterCondition.CombineConditionOperator.NOT_EMPTY) {
            return actualValue != null;
        }

        if (actualValue == null) {
            return false;
        }

        Object expectedValue = condition.getValue();

        try {
            return switch (operator) {
                case EQUALS -> matchEquals(actualValue, expectedValue);
                case NOT_EQUALS -> !matchEquals(actualValue, expectedValue);
                case CONTAINS -> matchContains(actualValue, expectedValue);
                case NOT_CONTAINS -> !matchContains(actualValue, expectedValue);
                case IN -> matchIn(actualValue, expectedValue);
                case NOT_IN -> !matchIn(actualValue, expectedValue);
                case GT -> matchCompare(actualValue, expectedValue) > 0;
                case LT -> matchCompare(actualValue, expectedValue) < 0;
                case GE -> matchCompare(actualValue, expectedValue) >= 0;
                case LE -> matchCompare(actualValue, expectedValue) <= 0;
                case BETWEEN -> matchBetween(actualValue, expectedValue);
                case DYNAMICS, COUNT_GT, COUNT_LT -> false;
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 等于匹配
     */
    private boolean matchEquals(Object actualValue, Object expectedValue) {
        if (actualValue instanceof List<?> actualList && expectedValue instanceof List<?> expectedList) {
            return actualList.equals(expectedList);
        }
        return Objects.equals(actualValue, expectedValue);
    }

    /**
     * 包含匹配
     */
    private boolean matchContains(Object actualValue, Object expectedValue) {
        String actualStr = String.valueOf(actualValue);
        String expectedStr = String.valueOf(expectedValue);
        return actualStr.contains(expectedStr);
    }

    /**
     * IN 匹配（实际值在期望值列表中）
     */
    private boolean matchIn(Object actualValue, Object expectedValue) {
        if (expectedValue instanceof List<?> expectedList) {
            if (actualValue instanceof List<?> actualList) {
                // 多值匹配：交集非空
                return actualList.stream().anyMatch(expectedList::contains);
            }
            return expectedList.contains(actualValue);
        }
        return false;
    }

    /**
     * 比较匹配（用于数字或日期比较）
     */
    private int matchCompare(Object actualValue, Object expectedValue) {
        if (actualValue instanceof Number actualNum && expectedValue instanceof Number expectedNum) {
            return Double.compare(actualNum.doubleValue(), expectedNum.doubleValue());
        }
        // 尝试转换为数字比较
        try {
            double actual = Double.parseDouble(String.valueOf(actualValue));
            double expected = Double.parseDouble(String.valueOf(expectedValue));
            return Double.compare(actual, expected);
        } catch (NumberFormatException e) {
            // 字符串比较
            return String.valueOf(actualValue).compareTo(String.valueOf(expectedValue));
        }
    }

    /**
     * BETWEEN 匹配（实际值在范围内）
     */
    private boolean matchBetween(Object actualValue, Object expectedValue) {
        if (expectedValue instanceof List<?> rangeList && rangeList.size() == 2) {
            Object min = rangeList.get(0);
            Object max = rangeList.get(1);
            return matchCompare(actualValue, min) >= 0 && matchCompare(actualValue, max) <= 0;
        }
        return false;
    }

    /**
     * 获取下一层节点列表
     */
    public List<ApprovalNodeResponse> getNextNodes(String nodeId) {
        ApprovalNode node = approvalNodeMapper.selectByPrimaryKey(nodeId);
        if (node == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        String flowId = node.getFlowId();

        // 查询节点连接关系
        ApprovalNodeLink linkCriteria = new ApprovalNodeLink();
        linkCriteria.setFlowId(flowId);
        linkCriteria.setFromNodeId(nodeId);
        List<ApprovalNodeLink> links = approvalNodeLinkMapper.select(linkCriteria);

        if (CollectionUtils.isEmpty(links)) {
            return List.of();
        }

        // 获取下一层节点ID列表（按sort排序）
        List<String> nextNodeIds = links.stream()
                .sorted(Comparator.comparing(ApprovalNodeLink::getSort))
                .map(ApprovalNodeLink::getToNodeId)
                .collect(Collectors.toList());

        // 批量查询节点
        List<ApprovalNode> nodes = approvalNodeMapper.selectByIds(nextNodeIds);
        Map<String, ApprovalNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(ApprovalNode::getId, n -> n));

        // 批量查询审批人节点配置
        List<ApprovalNodeApprover> approverNodes = approvalNodeApproverMapper.selectByIds(nextNodeIds);
        Map<String, ApprovalNodeApprover> approverNodeMap = approverNodes.stream()
                .collect(Collectors.toMap(ApprovalNodeApprover::getId, n -> n));

        // 批量查询条件节点配置
        List<ApprovalNodeCondition> conditionNodes = approvalNodeConditionMapper.selectByIds(nextNodeIds);
        Map<String, ApprovalNodeCondition> conditionNodeMap = conditionNodes.stream()
                .collect(Collectors.toMap(ApprovalNodeCondition::getId, n -> n));

        // 组装节点响应
        return nextNodeIds.stream()
                .map(nextNodeId -> {
                    ApprovalNode nextNode = nodeMap.get(nextNodeId);
                    if (nextNode == null) {
                        return null;
                    }

                    // 审批人节点
                    if (ApprovalNodeTypeEnum.APPROVER.name().equals(nextNode.getNodeType())) {
                        ApprovalNodeApprover approverNode = approverNodeMap.get(nextNodeId);
                        if (approverNode != null) {
                            ApprovalNodeApproverResponse approverResponse = BeanUtils.copyBean(
                                    new ApprovalNodeApproverResponse(), nextNode);
                            BeanUtils.copyBean(approverResponse, approverNode);
                            // 解析 JSON 字段为对象
                            parseApproverNodeFields(approverNode, approverResponse);
                            return approverResponse;
                        }
                    }

                    // 条件节点
                    if (ApprovalNodeTypeEnum.CONDITION.name().equals(nextNode.getNodeType())) {
                        ApprovalNodeCondition conditionNode = conditionNodeMap.get(nextNodeId);
                        if (conditionNode != null) {
                            ApprovalNodeConditionResponse conditionResponse = BeanUtils.copyBean(
                                    new ApprovalNodeConditionResponse(), nextNode);
                            // 手动解析条件配置（因为类型不同，BeanUtils 无法自动复制）
                            if (StringUtils.isNotBlank(conditionNode.getConditionConfig())) {
                                conditionResponse.setConditionConfig(
                                        JSON.parseObject(conditionNode.getConditionConfig(), CombineSearch.class));
                            }
                            return conditionResponse;
                        }
                    }

                    // 其他类型节点
                    return BeanUtils.copyBean(new ApprovalNodeResponse(), nextNode);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据权限ID查找对应的权限列表
     */
    private List<Permission> findPermissionsByPermissionId(List<PermissionDefinitionItem> permissionSetting, String permissionId) {
        if (StringUtils.isBlank(permissionId) || CollectionUtils.isEmpty(permissionSetting)) {
            return List.of();
        }
        for (PermissionDefinitionItem module : permissionSetting) {
            if (module.getChildren() != null) {
                for (PermissionDefinitionItem resource : module.getChildren()) {
                    if (resource.getId().equals(permissionId)) {
                        return resource.getPermissions();
                    }
                }
            }
        }
        return null;
    }
}