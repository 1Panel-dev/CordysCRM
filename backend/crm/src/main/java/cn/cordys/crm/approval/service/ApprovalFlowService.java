package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.Permission;
import cn.cordys.common.permission.PermissionDefinitionItem;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
import cn.cordys.crm.approval.dto.FieldPermissionDTO;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.*;
import cn.cordys.crm.approval.mapper.ExtApprovalFlowMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.mapper.ExtDepartmentCommanderMapper;
import cn.cordys.crm.system.mapper.ExtRoleMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
    private BaseMapper<ApprovalFlowVersion> approvalFlowVersionMapper;
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
    private BaseService baseService;
    @Resource
    private BaseMapper<Department> departmentBaseMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private ExtDepartmentCommanderMapper extDepartmentCommanderMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private ExtRoleMapper extRoleMapper;
    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;
	@Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
	@Resource
	private ModuleFormService formService;
    @Resource
	private UserViewService userViewService;
	@Resource
	private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
	private ApprovalInstanceService approvalInstanceService;

	/**
	 * 加签节点后缀分隔符
	 */
	public static final String SIGN_SPILT = "-SN";
	/**
	 * 范围大小
	 */
	public static final int RANGE_SIZE = 2;
	/**
	 * 审批流导出权限
	 */
	public static final String EXPORT_SUFFIX = "EXPORT";

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
        criteria.setDeleted(false);
        criteria.setOrganizationId(organizationId);
        List<ApprovalFlow> flows = approvalFlowMapper.select(criteria);

        if (CollectionUtils.isEmpty(flows)) {
            response.setStatusPermissions(List.of());
            return response;
        }

        // 优先使用启用的审批流
        ApprovalFlow targetFlow = flows.stream()
                .filter(ApprovalFlow::getEnable)
                .findFirst()
                .orElse(flows.getFirst());

        // 从主表获取状态权限配置
        if (StringUtils.isBlank(targetFlow.getStatusPermissions())) {
            response.setStatusPermissions(List.of());
            return response;
        }

        // 解析状态权限配置
        response.setStatusPermissions(parseStatusPermissions(permissions, targetFlow.getStatusPermissions()));
        return response;
    }

    /**
     * 批量检查资源的操作权限
     *
     * @param formType           表单类型
     * @param resources          资源列表
     * @param permission         权限标识
     * @param organizationId     组织ID
     * @param idGetter           获取资源ID的函数
     * @param statusGetter       获取审批状态的函数
     * @return 有权限的资源ID列表
     */
    public <T> List<String> filterResourcesWithPermission(
			String formType, List<T> resources, String permission, String organizationId,
			java.util.function.Function<T, String> idGetter,
			java.util.function.Function<T, String> statusGetter) {
        if (CollectionUtils.isEmpty(resources)) {
            return List.of();
        }

        // 检查是否所有资源都没有审批状态，如果是则直接返回所有资源ID，无需权限校验
        boolean allStatus = resources.stream()
                .allMatch(resource -> StringUtils.isNotBlank(statusGetter.apply(resource))
                        && Strings.CS.equals(statusGetter.apply(resource), ApprovalStatus.NONE.name()));

        if (allStatus) {
            // 所有资源都没有审批状态，则无需校验权限，直接返回所有资源ID
            return resources.stream().map(idGetter).collect(Collectors.toList());
        }

        // 获取状态权限配置
        StatusPermissionSettingResponse setting = getStatusPermissionsByFormType(formType, organizationId);
        List<StatusPermissionDTO> statusPermissions = setting.getStatusPermissions();

        if (CollectionUtils.isEmpty(statusPermissions)) {
            // 没有配置状态权限限制，全部允许操作
            return resources.stream().map(idGetter).collect(Collectors.toList());
        }

        // 构建需要权限的状态集合：(审批状态, 权限) -> 是否需要权限
        Map<String, Boolean> permissionRequiredMap = new HashMap<>(statusPermissions.size());
        for (StatusPermissionDTO sp : statusPermissions) {
            if (permission.equals(sp.getPermission())) {
                permissionRequiredMap.put(sp.getApprovalStatus(), sp.getEnabled());
            }
        }
        // 没有开启审核时创建的数据，状态为NONE，也应该能被修改
        permissionRequiredMap.put(ApprovalStatus.NONE.name(), true);
        // 检查用户是否有该权限
        boolean hasPermission = PermissionUtils.hasPermission(permission);

        // 过滤出有权限的资源
        return resources.stream()
                .filter(resource -> {
                    String status = statusGetter.apply(resource);
                    if (StringUtils.isBlank(status)) {
                        // 没有审批状态，不受限制
                        return true;
                    }
                    Boolean required = permissionRequiredMap.get(status);
                    // 需要权限，检查用户是否有权限
                    return BooleanUtils.isTrue(required) && hasPermission;
                })
                .map(idGetter)
                .collect(Collectors.toList());
    }

    /**
     * 批量检查资源的导出权限
     * 根据审批流配置的状态权限，过滤出当前用户可以导出的资源
     *
     * @param formKey            表单Key(如: quotation, contract, invoice, order)
     * @param resources          资源列表
     * @param organizationId     组织ID
     * @param idGetter           获取资源ID的函数
     * @param statusGetter       获取审批状态的函数
     * @return 可以导出的资源ID列表
     */
    public <T> List<String> filterResourcesWithExportPermission(
            String formKey, List<T> resources, String organizationId,
            java.util.function.Function<T, String> idGetter,
            java.util.function.Function<T, String> statusGetter) {
        if (CollectionUtils.isEmpty(resources)) {
            return List.of();
        }
		// 审批状态缺失走数据权限, 无需再过滤
        boolean allNoApprovalStatus = resources.stream()
                .allMatch(resource -> {
                    String status = statusGetter.apply(resource);
                    return StringUtils.isBlank(status) || Strings.CS.equals(status, ApprovalStatus.NONE.name());
                });
        if (allNoApprovalStatus) {
            return resources.stream().map(idGetter).collect(Collectors.toList());
        }

        // 获取审批流状态权限配置
        StatusPermissionSettingResponse setting = getStatusPermissionsByFormType(formKey, organizationId);
        if (setting == null || CollectionUtils.isEmpty(setting.getStatusPermissions())) {
            // 没有开启审批流或者没有配置状态权限限制，全部允许导出
            return resources.stream().map(idGetter).collect(Collectors.toList());
        }

        // 过滤出导出权限开启的状态集合
		List<StatusPermissionDTO> statusPermissions = setting.getStatusPermissions();
		List<String> exportApprovalStatus = statusPermissions.stream().filter(sp -> sp.getPermission().contains(EXPORT_SUFFIX) && sp.getEnabled()).map(StatusPermissionDTO::getApprovalStatus).toList();

        // 如果没有配置任何导出权限，无需过滤
        if (CollectionUtils.isEmpty(exportApprovalStatus)) {
            return resources.stream().filter(resource -> {
				String status = statusGetter.apply(resource);
						return StringUtils.isBlank(status) || Strings.CS.equals(status, ApprovalStatus.NONE.name());
			}).map(idGetter)
			.collect(Collectors.toList());
        }


        // 过滤出有导出权限的资源
        return resources.stream()
                .filter(resource -> {
                    String status = statusGetter.apply(resource);
                    if (StringUtils.isBlank(status) || Strings.CS.equals(status, ApprovalStatus.NONE.name())) {
                        // 没有审批状态，不受限制，允许导出
                        return true;
                    }
					// 审批状态开启了导出权限, 可导出
					return exportApprovalStatus.contains(status);
				})
                .map(idGetter)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询审批流列表
     */
    public Pager<List<ApprovalFlowListResponse>> list(ApprovalFlowPageRequest request, String organizationId) {
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ApprovalFlowListResponse> responses = extApprovalFlowMapper.list(request, organizationId);
        baseService.setCreateAndUpdateUserName(responses);
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

        ApprovalFlowDetailResponse response = BeanUtils.copyBean(new ApprovalFlowDetailResponse(), flow);

        // 设置创建人和更新人名称
        baseService.setCreateAndUpdateUserName(response);

        List<Permission> permissions = getPermissionsByFormType(flow.getFormType());

        // 设置对应资源的权限列表
        response.setPermissions(getResourcePermissions(permissions));

        // 解析状态权限配置
        response.setStatusPermissions(parseStatusPermissions(response.getPermissions(), flow.getStatusPermissions()));

        // 查询节点配置和连接关系
        buildNodesAndLinks(flow.getCurrentVersionId(), response, organizationId);

        return response;
    }

    /**
     * 根据表单类型获取审批流信息（不含节点配置）
     */
    public ApprovalFlowByFormTypeResponse getByFormType(String formType, String organizationId) {
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setFormType(formType);
        criteria.setEnable(true);
        criteria.setDeleted(false);
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

        // 解析状态权限配置（从主表获取）
        response.setStatusPermissions(parseStatusPermissions(response.getPermissions(), targetFlow.getStatusPermissions()));

        return response;
    }

    /**
     * 新建审批流
     */
    @OperationLog(module = LogModule.APPROVAL_FLOW, type = LogType.ADD, resourceName = "{#request.name}")
    public ApprovalFlowDetailResponse add(ApprovalFlowAddRequest request, String userId, String organizationId) {
        // 检查该表单类型是否已存在审批流（每个表单类型只能创建一个）
        ApprovalFlow existFlow = selectApprovalFlowByFormType(request.getFormType(), organizationId);
        if (existFlow != null) {
            throw new GenericException(Translator.get("approval_flow.type.already.exists"));
        }

        // 创建审批流主表
        ApprovalFlow flow = BeanUtils.copyBean(new ApprovalFlow(), request);
        flow.setId(IDGenerator.nextStr());
        flow.setNumber(generateFlowNumber(request.getFormType(), organizationId));
        flow.setCreateUser(userId);
        flow.setCreateTime(System.currentTimeMillis());
        flow.setUpdateUser(userId);
        flow.setUpdateTime(System.currentTimeMillis());
        flow.setOrganizationId(organizationId);
        flow.setDeleted(false);

        // 保存状态权限配置到主表
        if (request.getStatusPermissions() != null) {
            flow.setStatusPermissions(JSON.toJSONString(request.getStatusPermissions()));
        }

        // 创建版本（版本表只存储节点配置的关联）
        ApprovalFlowVersion version = createFlowVersion(flow.getId(), userId, organizationId);
        approvalFlowVersionMapper.insert(version);

        // 更新当前版本ID
        flow.setCurrentVersionId(version.getId());
        approvalFlowMapper.insert(flow);

        // 保存节点配置
        saveNodesAndLinks(request.getNodes(), request.getLinks(), version.getId(), flow.getId());

        ApprovalFlowDetailResponse detail = getDetail(flow.getId(), organizationId);

        // 设置日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(flow.getId())
                        .modifiedValue(detail)
                        .build()
        );

        return detail;
    }

    public ApprovalFlow selectApprovalFlowByFormType(String formType, String organizationId) {
        ApprovalFlow existCriteria = new ApprovalFlow();
        existCriteria.setFormType(formType);
        existCriteria.setDeleted(false);
        existCriteria.setOrganizationId(organizationId);
        List<ApprovalFlow> existFlows = approvalFlowMapper.select(existCriteria);
        if (CollectionUtils.isEmpty(existFlows)) {
            return null;
        }
        return existFlows.getFirst();
    }

    /**
     * 创建审批流版本
     */
    private ApprovalFlowVersion createFlowVersion(String flowId, String userId, String organizationId) {
        ApprovalFlowVersion version = new ApprovalFlowVersion();
        version.setId(IDGenerator.nextStr());
        version.setFlowId(flowId);
        version.setOrganizationId(organizationId);
        version.setCreateUser(userId);
        version.setCreateTime(System.currentTimeMillis());
        return version;
    }

    /**
     * 更新审批流
     */
    @OperationLog(module = LogModule.APPROVAL_FLOW, type = LogType.UPDATE, resourceId = "{#request.id}")
    public ApprovalFlowDetailResponse update(ApprovalFlowUpdateRequest request, String userId, String organizationId) {
        // 获取原始日志DTO
        ApprovalFlowDetailResponse originDetail = getDetail(request.getId(), organizationId);

        // 更新审批流主表（配置字段直接保存到主表）
        ApprovalFlow flow = BeanUtils.copyBean(new ApprovalFlow(), request);
        flow.setUpdateUser(userId);
        flow.setUpdateTime(System.currentTimeMillis());

        // 保存状态权限配置到主表
        if (request.getStatusPermissions() != null) {
            flow.setStatusPermissions(JSON.toJSONString(request.getStatusPermissions()));
        }

        // 如果有节点更新，创建新版本
        if (CollectionUtils.isNotEmpty(request.getNodes())) {
            ApprovalFlowVersion newVersion = createFlowVersion(originDetail.getId(), userId, organizationId);
            approvalFlowVersionMapper.insert(newVersion);

            // 更新当前版本ID
            flow.setCurrentVersionId(newVersion.getId());

            // 保存新节点配置
            saveNodesAndLinks(request.getNodes(), request.getLinks(), newVersion.getId(), originDetail.getId());
        }

        approvalFlowMapper.updateById(flow);

        ApprovalFlowDetailResponse detail = getDetail(flow.getId(), organizationId);

        // 获取更新后的日志DTO
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(detail.getName())
                        .originalValue(originDetail)
                        .modifiedValue(detail)
                        .build()
        );

        return detail;
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

        // 软删除：设置 deleted = true
        ApprovalFlow update = new ApprovalFlow();
        update.setId(id);
        update.setDeleted(true);
        update.setUpdateTime(System.currentTimeMillis());
        approvalFlowMapper.updateById(update);

        // 清除审批中的资源和待办
        approvalInstanceService.clearApprovingInstanceOfFlow(id);

        // 设置日志上下文
        OperationLogContext.setResourceName(flow.getName());
    }

    /**
     * 启用/禁用审批流
     */
    @OperationLog(module = LogModule.APPROVAL_FLOW, type = LogType.UPDATE, resourceId = "{#id}")
    public void updateEnable(String id, Boolean enable, String userId, String organizationId) {
        ApprovalFlow flow = approvalFlowMapper.selectByPrimaryKey(id);
        if (flow == null || !flow.getOrganizationId().equals(organizationId) || Boolean.TRUE.equals(flow.getDeleted())) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        Boolean oldEnable = flow.getEnable();

        ApprovalFlow update = new ApprovalFlow();
        update.setId(id);
        update.setEnable(enable);
        update.setUpdateUser(userId);
        update.setUpdateTime(System.currentTimeMillis());
        approvalFlowMapper.updateById(update);

        // 设置日志上下文
        Map<String, Object> originalVal = new HashMap<>(2);
        originalVal.put("enable", oldEnable);
        Map<String, Object> modifiedVal = new HashMap<>(2);
        modifiedVal.put("enable", enable);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(flow.getName())
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
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

    /**
     * 生成节点编码
     */
    private String generateNodeNumber(String flowId) {
        String key = "approval_node_num:" + ":" + flowId;
        long seq = stringRedisTemplate.opsForValue().increment(key);
        return String.format("P" + "N%03d", seq);    }

    private String getNumberPrefix(String formType) {
        try {
            return ApprovalFormTypeEnum.valueOf(formType.toUpperCase()).getPrefix();
        } catch (IllegalArgumentException e) {
            return "APV";
        }
    }

    private List<ApprovalNodeResponse> getNodesByFlowVersionId(String flowVersionId) {
        if (StringUtils.isBlank(flowVersionId)) {
            return List.of();
        }
        ApprovalNode criteria = new ApprovalNode();
        criteria.setFlowVersionId(flowVersionId);
        List<ApprovalNode> nodes = approvalNodeMapper.select(criteria);
        return nodes.stream().map(node -> {
            // 查询审批人节点配置
            if (ApprovalNodeTypeEnum.APPROVER.name().equals(node.getNodeType())) {
                ApprovalNodeApprover approverNode = approvalNodeApproverMapper.selectByPrimaryKey(node.getId());
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
                ApprovalNodeCondition conditionNode = approvalNodeConditionMapper.selectByPrimaryKey(node.getId());
                if (conditionNode != null) {
                    ApprovalNodeConditionResponse conditionResponse = BeanUtils.copyBean(
                            new ApprovalNodeConditionResponse(), node);
                    if (StringUtils.isNotBlank(conditionNode.getConditionConfig())) {
                        conditionResponse.setConditionConfig(JSON.parseObject(conditionNode.getConditionConfig(), CombineSearch.class));
                    }
                    return conditionResponse;
                }
            }
            // 其他类型节点
            return BeanUtils.copyBean(new ApprovalNodeResponse(), node);
        }).collect(Collectors.toList());
    }

    /**
     * 解析审批人节点 JSON 字段为对象
     */
    private void parseApproverNodeFields(ApprovalNodeApprover approverNode, ApprovalNodeApproverResponse response) {
        // 解析审批人列表
        List<String> approverIds = null;
        if (StringUtils.isNotBlank(approverNode.getApproverList())) {
            approverIds = JSON.parseArray(approverNode.getApproverList(), String.class);
            response.setApproverList(approverIds);
        }
        // 解析抄送人列表
        List<String> ccIds = null;
        if (StringUtils.isNotBlank(approverNode.getCcList())) {
            ccIds = JSON.parseArray(approverNode.getCcList(), String.class);
            response.setCcList(ccIds);
        }
        // 解析其他配置
        if (StringUtils.isNotBlank(approverNode.getPassPostConfig())) {
            response.setPassPostConfig(JSON.parseObject(approverNode.getPassPostConfig(), ApprovalPostConfigDTO.class));
        }
        if (StringUtils.isNotBlank(approverNode.getRejectPostConfig())) {
            response.setRejectPostConfig(JSON.parseObject(approverNode.getRejectPostConfig(), ApprovalPostConfigDTO.class));
        }
        if (StringUtils.isNotBlank(approverNode.getFieldPermissions())) {
            response.setFieldPermissions(JSON.parseArray(approverNode.getFieldPermissions(), FieldPermissionDTO.class));
        }

        // 查询审批人选择项名称（用于前端回显）
        if (CollectionUtils.isNotEmpty(approverIds)) {
            response.setApproverSelectOptions(resolveSelectOptions(approverNode.getApproverType(), approverIds));
        }
        // 查询抄送人选择项名称（用于前端回显）
        if (CollectionUtils.isNotEmpty(ccIds)) {
            response.setCcSelectOptions(resolveSelectOptions(approverNode.getCcType(), ccIds));
        }
    }

    /**
     * 根据类型解析选择项列表
     */
    private List<OptionDTO> resolveSelectOptions(String type, List<String> ids) {
        if (StringUtils.isBlank(type) || CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        try {
            ApproverTypeEnum approverType = ApproverTypeEnum.valueOf(type);
            return switch (approverType) {
                case MEMBER -> resolveMemberOptions(ids);
                case ROLE -> resolveRoleOptions(ids);
                default -> List.of();
            };
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    /**
     * 解析成员选择项
     */
    private List<OptionDTO> resolveMemberOptions(List<String> userIds) {
        List<OptionDTO> userOptions = extUserMapper.selectUserOptionByIds(userIds);
        if (CollectionUtils.isEmpty(userOptions)) {
            return List.of();
        }
        // 按照 userIds 顺序返回
        Map<String, OptionDTO> optionMap = userOptions.stream()
                .collect(Collectors.toMap(OptionDTO::getId, o -> o));
        return userIds.stream()
                .map(optionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 解析角色选择项
     */
    private List<OptionDTO> resolveRoleOptions(List<String> roleIds) {
        List<OptionDTO> roleOptions = extRoleMapper.getIdNameByIds(roleIds);
        if (CollectionUtils.isEmpty(roleOptions)) {
            return List.of();
        }
        // 按照 roleIds 顺序返回
        Map<String, OptionDTO> optionMap = roleOptions.stream()
                .collect(Collectors.toMap(OptionDTO::getId, o -> o));
        return roleIds.stream()
                .map(optionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建节点和连接关系
     */
    private void buildNodesAndLinks(String flowVersionId, ApprovalFlowDetailResponse response, String organizationId) {
        if (StringUtils.isBlank(flowVersionId)) {
            response.setNodes(List.of());
            response.setLinks(List.of());
            response.setOptionMap(Map.of());
            return;
        }
        // 获取所有节点并按 sort 排序
        List<ApprovalNodeResponse> allNodes = getNodesByFlowVersionId(flowVersionId);
        allNodes.sort(Comparator.comparing(ApprovalNodeResponse::getSort));

        // 收集所有审批人节点的 fallbackApprover ID
        Set<String> fallbackApproverIds = allNodes.stream()
                .filter(node -> node instanceof ApprovalNodeApproverResponse)
                .map(node -> (ApprovalNodeApproverResponse) node)
                .map(ApprovalNodeApproverResponse::getFallbackApprover)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 批量查询兜底审批人名称
        Map<String, String> fallbackApproverNameMap = baseService.getUserNameMap(fallbackApproverIds);

        // 设置兜底审批人名称
        allNodes.stream()
                .filter(node -> node instanceof ApprovalNodeApproverResponse)
                .map(node -> (ApprovalNodeApproverResponse) node)
                .filter(node -> StringUtils.isNotBlank(node.getFallbackApprover()))
                .forEach(node -> node.setFallbackApproverName(
                        baseService.getAndCheckOptionName(fallbackApproverNameMap.get(node.getFallbackApprover()))));

        response.setNodes(allNodes);

        // 获取节点连接关系并按 sort 排序
        ApprovalNodeLink linkCriteria = new ApprovalNodeLink();
        linkCriteria.setFlowVersionId(flowVersionId);
        List<ApprovalNodeLink> links = approvalNodeLinkMapper.select(linkCriteria);
        links.sort(Comparator.comparing(ApprovalNodeLink::getSort));

        List<ApprovalNodeLinkResponse> linkResponses = links.stream()
                .map(link -> {
                    ApprovalNodeLinkResponse linkResponse = new ApprovalNodeLinkResponse();
                    linkResponse.setId(link.getId());
                    linkResponse.setFromNodeId(link.getFromNodeId());
                    linkResponse.setToNodeId(link.getToNodeId());
                    return linkResponse;
                })
                .collect(Collectors.toList());

        response.setLinks(linkResponses);

        // 构建条件节点字段选项映射
        Map<String, List<OptionDTO>> optionMap = buildConditionOptionMap(allNodes, response.getFormType(), organizationId);
        response.setOptionMap(optionMap);
    }

    /**
     * 构建条件节点字段选项映射
     */
    private Map<String, List<OptionDTO>> buildConditionOptionMap(List<ApprovalNodeResponse> nodes, String formType, String organizationId) {
        // 收集所有条件节点的条件配置
        List<FilterCondition> allConditions = nodes.stream()
                .filter(node -> node instanceof ApprovalNodeConditionResponse)
                .map(node -> (ApprovalNodeConditionResponse) node)
                .filter(node -> node.getConditionConfig() != null && CollectionUtils.isNotEmpty(node.getConditionConfig().getConditions()))
                .map(node -> node.getConditionConfig().getConditions())
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<BaseModuleFieldValue> postConfigFieldValues = getNodePostConfigFieldValues(nodes);

        if (CollectionUtils.isEmpty(allConditions) && CollectionUtils.isEmpty(postConfigFieldValues)) {
            return Map.of();
        }

        List<FilterCondition> postConfigConditions = postConfigFieldValues.stream()
                .filter(BaseModuleFieldValue::valid)
                .map(fieldValue -> {
                    FilterCondition condition = new FilterCondition();
                    condition.setName(fieldValue.getFieldId());
                    condition.setValue(fieldValue.getFieldValue());
                    return condition;
                })
                .toList();

        // 合并所有条件
        List<FilterCondition> combinedConditions = new ArrayList<>(allConditions);
        combinedConditions.addAll(postConfigConditions);

        return userViewService.buildOptionMap(organizationId, formType, combinedConditions);
    }

    private List<BaseModuleFieldValue> getNodePostConfigFieldValues(List<ApprovalNodeResponse> nodes) {
        // 收集审批人节点中的 passPostConfig 和 rejectPostConfig 中的字段值
		return nodes.stream()
				.filter(node -> node instanceof ApprovalNodeApproverResponse)
				.map(node -> (ApprovalNodeApproverResponse) node)
				.flatMap(approverNode -> {
					List<BaseModuleFieldValue> fieldValues = new ArrayList<>();
					if (approverNode.getPassPostConfig() != null
							&& CollectionUtils.isNotEmpty(approverNode.getPassPostConfig().getFieldUpdateConfigs())) {
						fieldValues.addAll(approverNode.getPassPostConfig().getFieldUpdateConfigs());
					}
					if (approverNode.getRejectPostConfig() != null
							&& CollectionUtils.isNotEmpty(approverNode.getRejectPostConfig().getFieldUpdateConfigs())) {
						fieldValues.addAll(approverNode.getRejectPostConfig().getFieldUpdateConfigs());
					}
					return fieldValues.stream();
				})
				.collect(Collectors.toList());
    }

    /**
     * 批量保存节点配置和连接关系
     */
    private void saveNodesAndLinks(List<ApprovalNodeRequest> nodes, List<ApprovalNodeLinkRequest> links,
                                    String flowVersionId, String flowId) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        // 用于收集节点和配置信息
        List<ApprovalNode> allNodes = new ArrayList<>();
        List<ApprovalNodeApprover> allApproverNodes = new ArrayList<>();
        List<ApprovalNodeCondition> allConditionNodes = new ArrayList<>();

        // 构建前端ID到数据库ID的映射
        Map<String, String> nodeIdMap = new HashMap<>(nodes.size());

        // 收集所有节点信息，按数组顺序设置 sort
        // 所有节点都生成新的数据库ID，前端传的ID仅用于关联关系映射
        int nodeSort = 0;
        for (ApprovalNodeRequest nodeRequest : nodes) {
            String frontEndId = nodeRequest.getId();
            String newNodeId = IDGenerator.nextStr();
            // 前端ID映射到新的数据库ID
            if (StringUtils.isNotBlank(frontEndId)) {
                nodeIdMap.put(frontEndId, newNodeId);
            }

            // 收集节点基本信息
            ApprovalNode node = BeanUtils.copyBean(new ApprovalNode(), nodeRequest);
            node.setId(newNodeId);
            node.setFlowVersionId(flowVersionId);
            // 如果前端传了number则使用，否则生成新的number
            if (StringUtils.isBlank(nodeRequest.getNumber())) {
                node.setNumber(generateNodeNumber(flowId));
            } else {
                node.setNumber(nodeRequest.getNumber());
            }
            node.setSort(nodeSort++);
            allNodes.add(node);

            // 收集审批人节点配置
            if (nodeRequest instanceof ApprovalNodeApproverRequest approverRequest) {
                ApprovalNodeApprover approverNode = BeanUtils.copyBean(new ApprovalNodeApprover(), approverRequest,
                        "approverList", "ccList", "passPostConfig", "rejectPostConfig", "fieldPermissions");
                approverNode.setId(newNodeId);
                approverNode.setFlowVersionId(flowVersionId);
                approverNode.setApproverList(JSON.toJSONString(approverRequest.getApproverList()));
                approverNode.setCcList(JSON.toJSONString(approverRequest.getCcList()));
                approverNode.setPassPostConfig(JSON.toJSONString(approverRequest.getPassPostConfig()));
                approverNode.setRejectPostConfig(JSON.toJSONString(approverRequest.getRejectPostConfig()));
                approverNode.setFieldPermissions(JSON.toJSONString(approverRequest.getFieldPermissions()));
                allApproverNodes.add(approverNode);
            }
            // 收集条件节点配置
            else if (nodeRequest instanceof ApprovalNodeConditionRequest conditionRequest) {
                ApprovalNodeCondition conditionNode = BeanUtils.copyBean(new ApprovalNodeCondition(), conditionRequest,
                        "rules");
                conditionNode.setId(newNodeId);
                conditionNode.setFlowVersionId(flowVersionId);
                conditionNode.setConditionConfig(JSON.toJSONString(conditionRequest.getConditionConfig()));
                allConditionNodes.add(conditionNode);
            }
        }

        // 收集连接信息，按数组顺序设置 sort
        // 所有连接都使用映射后的新节点ID
        List<ApprovalNodeLink> allLinks = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(links)) {
            int linkSort = 0;
            for (ApprovalNodeLinkRequest linkRequest : links) {
                String fromNodeId = nodeIdMap.get(linkRequest.getFromNodeId());
                String toNodeId = nodeIdMap.get(linkRequest.getToNodeId());
                // 只有当 from 和 to 节点都存在时才创建连接
                if (fromNodeId == null || toNodeId == null) {
                    continue;
                }

                ApprovalNodeLink link = new ApprovalNodeLink();
                link.setId(IDGenerator.nextStr());
                link.setFlowVersionId(flowVersionId);
                link.setFromNodeId(fromNodeId);
                link.setToNodeId(toNodeId);
                link.setSort(linkSort++);
                allLinks.add(link);
            }
        }

        // 批量插入
        approvalNodeMapper.batchInsert(allNodes);
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
     * 获取对应资源的权限列表
     */
    private List<OptionDTO> getResourcePermissions(List<Permission> permissions) {
        return permissions.stream()
                .map(p -> new OptionDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    private List<Permission> getPermissionsByFormType(String formType) {
        List<PermissionDefinitionItem> permissionSetting = roleService.getPermissionSetting();
        String permissionId = Objects.requireNonNull(ApprovalFormTypeEnum.getByValue(formType)).getPermissionId();
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
            return List.of();
        }
        // 解析已保存的状态权限配置
        List<StatusPermissionDTO> savedPermissions = JSON.parseArray(statusPermissions, StatusPermissionDTO.class);

        if (CollectionUtils.isEmpty(savedPermissions)) {
            return List.of();
        }

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
            // AND 模式：所有都必须满足
            return conditions.stream().allMatch(condition -> matchSingleCondition(condition, fieldValueMap));
        } else {
            // OR 模式：任一满足即可
            return conditions.stream().anyMatch(condition -> matchSingleCondition(condition, fieldValueMap));
        }
    }

    /**
     * 匹配单个条件
     */
    private boolean matchSingleCondition(FilterCondition condition, Map<String, Object> fieldValueMap) {
        String fieldName = condition.getName();
        Object actualValue = fieldValueMap.get(fieldName);

        // 获取动态转换后的值和操作符
        Object expectedValue = condition.getCombineValue();
        String operatorStr = condition.getCombineOperator();

        // 获取操作符枚举
        FilterCondition.CombineConditionOperator operator;
        try {
            operator = FilterCondition.CombineConditionOperator.valueOf(operatorStr);
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
        if (expectedValue instanceof List<?> rangeList && rangeList.size() == RANGE_SIZE) {
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

        String flowVersionId = node.getFlowVersionId();

        // 查询节点连接关系
        ApprovalNodeLink linkCriteria = new ApprovalNodeLink();
        linkCriteria.setFlowVersionId(flowVersionId);
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
                            // 解析 JSON 字段为对象（不需要前端回显，传null）
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

    /**
     * 获取表单下启用的审批流配置
     *
     * @param formKey        表单类型
     * @param organizationId 组织ID
     * @return 审批流配置，如果不存在或未启用返回null
     */
    public ApprovalFlow getEnabledFlow(String formKey, String organizationId) {
        if (StringUtils.isBlank(formKey) || StringUtils.isBlank(organizationId)) {
            return null;
        }
        LambdaQueryWrapper<ApprovalFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlow::getFormType, formKey)
                .eq(ApprovalFlow::getOrganizationId, organizationId)
                .eq(ApprovalFlow::getEnable, true)
                .eq(ApprovalFlow::getDeleted, false);
        List<ApprovalFlow> approvalFlows = approvalFlowMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(approvalFlows)) {
            return null;
        }
        return approvalFlows.getFirst();
    }

    /**
     * 根据审批人类型解析具体审批人用户列表
     *
     * @param userId       当前用户ID
     * @param approverType 审批人类型枚举
     * @param approverList 审批人值列表
     * @return 具体的用户列表，按照 approverList 顺序返回
     */
    public List<User> resolveApprovers(String userId, String orgId, ApproverTypeEnum approverType, List<String> approverList, ApproverDirectionEnum direction) {
        if (StringUtils.isBlank(userId) || approverType == null || CollectionUtils.isEmpty(approverList)) {
            return List.of();
        }

        // 获取当前用户的组织用户信息
        OrganizationUser currentUser = getOrganizationUser(userId, orgId);
        return switch (approverType) {
            case MEMBER -> resolveMemberApprovers(orgId, approverList);
            case ROLE -> resolveRoleApprovers(orgId, approverList);
            case SUPERIOR -> resolveSuperiorApprovers(orgId, currentUser, approverList);
            case MULTIPLE_SUPERIOR -> resolveMultipleSuperiorApprovers(orgId, currentUser, approverList, direction);
            case DEPT_HEAD -> resolveDeptHeadApprovers(orgId, currentUser, approverList);
            case MULTIPLE_DEPT_HEAD -> resolveMultipleDeptHeadApprovers(orgId, currentUser, approverList, direction);
        };
    }

    private ApproverDirectionEnum getApproverDirection(String direction) {
        if (StringUtils.isBlank(direction)) {
            return ApproverDirectionEnum.BOTTOM_UP;
        }
        try {
            return ApproverDirectionEnum.valueOf(direction);
        } catch (IllegalArgumentException e) {
            return ApproverDirectionEnum.BOTTOM_UP;
        }
    }

    private OrganizationUser getOrganizationUser(String userId, String orgId) {
        OrganizationUser criteria = new OrganizationUser();
        criteria.setUserId(userId);
        criteria.setOrganizationId(orgId);
		return organizationUserMapper.selectOne(criteria);
    }

    /**
     * 解析指定成员审批人
     */
    private List<User> resolveMemberApprovers(String orgId, List<String> approverList) {
		List<OrganizationUser> organizationUsers = organizationUserMapper.selectListByLambda(new LambdaQueryWrapper<OrganizationUser>().in(OrganizationUser::getUserId, approverList));
		List<User> users = extUserMapper.getOrgUserByUserIds(orgId, approverList);
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
		// 按照 approverList 顺序返回
		List<User> resolveUsers = new ArrayList<>();
		approverList.forEach(approver -> {
			Optional<OrganizationUser> findUser = organizationUsers.stream().filter(u -> Strings.CI.equals(u.getUserId(), approver)).findAny();
			if (!userMap.containsKey(approver)) {
				// 审批人被删除, 替换成ADMIN
				User admin = new User();
				admin.setId(InternalUser.ADMIN.getValue());
				resolveUsers.add(admin);
				return;
			}
			if (findUser.isPresent() && !findUser.get().getEnable()) {
				// 审批人被禁用, 替换为直属上级
				if (StringUtils.isNotBlank(findUser.get().getSupervisorId())) {
					User superUser = new User();
					superUser.setId(findUser.get().getSupervisorId());
					resolveUsers.add(superUser);
				} else {
					// 直属上级为空, 替换成ADMIN
					User admin = new User();
					admin.setId(InternalUser.ADMIN.getValue());
					resolveUsers.add(admin);
				}
				return;
			}
			resolveUsers.add(userMap.get(approver));
		});
		return resolveUsers;
    }

    /**
     * 解析指定角色审批人
     */
    private List<User> resolveRoleApprovers(String orgId, List<String> roleIds) {
        List<String> userIds = extUserRoleMapper.getUserIdsByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of();
        }
        return resolveMemberApprovers(orgId, userIds);
    }

    /**
     * 解析指定上级审批人
     */
    private List<User> resolveSuperiorApprovers(String orgId, OrganizationUser currentUser, List<String> approverList) {
		if (currentUser == null) {
			return List.of();
		}
        // 值是单选
        Integer approvalLevel = getValidLevel(approverList);

        String approver = null;
        String currentSupervisorId = currentUser.getSupervisorId();

        for (int level = 1; level <= approvalLevel && StringUtils.isNotBlank(currentSupervisorId); level++) {
            if (approvalLevel.equals(level)) {
                approver = currentSupervisorId;
                break;
            }
            OrganizationUser supervisorOrgUser = getOrganizationUser(currentSupervisorId, orgId);
            if (supervisorOrgUser == null) {
                break;
            }
            currentSupervisorId = supervisorOrgUser.getSupervisorId();
        }

        if (approver == null) {
            return List.of();
        }

        return resolveMemberApprovers(orgId, List.of(approver));
    }

    /**
     * 解析多级上级审批人
     */
    private List<User> resolveMultipleSuperiorApprovers(String orgId, OrganizationUser currentUser, List<String> approverList, ApproverDirectionEnum direction) {
		if (currentUser == null) {
			return List.of();
		}
        int approvalLevel = getValidLevel(approverList);
        if (approvalLevel <= 0) {
            return List.of();
        }

        // 从下往上收集所有上级
        List<String> allSuperiorIds = new ArrayList<>();
        String currentSupervisorId = currentUser.getSupervisorId();

        while (StringUtils.isNotBlank(currentSupervisorId)) {
            allSuperiorIds.add(currentSupervisorId);
            OrganizationUser supervisorOrgUser = getOrganizationUser(currentSupervisorId, orgId);
            if (supervisorOrgUser == null) {
                break;
            }
            currentSupervisorId = supervisorOrgUser.getSupervisorId();
        }

        if (allSuperiorIds.isEmpty()) {
            return List.of();
        }

        // 根据方向截取：BOTTOM_UP 取前 approvalLevel 个；TOP_DOWN 取到从顶往下第 approvalLevel 层
        List<String> resultIds;
        if (direction == ApproverDirectionEnum.TOP_DOWN) {
            int total = allSuperiorIds.size();
            int endExclusive = Math.min(total, Math.max(1, total - approvalLevel + 1));
            resultIds = allSuperiorIds.subList(0, endExclusive);
        } else {
            resultIds = allSuperiorIds.subList(0, Math.min(allSuperiorIds.size(), approvalLevel));
        }

        return resolveMemberApprovers(orgId, resultIds);
    }

    private Integer getValidLevel(List<String> approverList) {
        // 使用 ApproverLevelEnum 验证并获取有效的层级值
        Set<String> validLevels = Arrays.stream(ApproverLevelEnum.values())
                .map(ApproverLevelEnum::getValue)
                .collect(Collectors.toSet());

        // 过滤出有效的层级并按 approverList 顺序保存
        List<String> validRequestedLevels = approverList.stream()
                .filter(validLevels::contains)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validRequestedLevels)) {
            return 0;
        }

        // 值是单选
        return Integer.parseInt(validRequestedLevels.getFirst());
    }

    /**
     * 解析部门的负责人审批人
     */
    private List<User> resolveDeptHeadApprovers(String orgId, OrganizationUser currentUser, List<String> approverList) {
		if (currentUser == null) {
			return List.of();
		}
		String departmentId = currentUser.getDepartmentId();
        if (StringUtils.isBlank(departmentId)) {
            return List.of();
        }

        // 值是单选
        Integer approvalLevel = getValidLevel(approverList);

        String commanderId = null;
        String currentDepartmentId = departmentId;

        for (int level = 1; level <= approvalLevel && StringUtils.isNotBlank(currentDepartmentId); level++) {
            String currentCommanderId = getDeptCommander(orgId, currentDepartmentId);
            if (StringUtils.isNotBlank(currentCommanderId) && approvalLevel.equals(level)) {
                commanderId = currentCommanderId;
                break;
            }
            // 获取父部门，继续向上查找
            currentDepartmentId = getParentDepartmentId(currentDepartmentId);
        }

        if (StringUtils.isBlank(commanderId)) {
            return List.of();
        }

        return resolveMemberApprovers(orgId, List.of(commanderId));
    }

    private String getDeptCommander(String orgId, String departmentId) {
        List<String> commanderIds = extDepartmentCommanderMapper.selectCommander(departmentId, orgId);
        if (CollectionUtils.isNotEmpty(commanderIds)) {
            return commanderIds.getFirst();
        }
        return null;
    }

    /**
     * 解析多级部门的负责人审批人
     */
    private List<User> resolveMultipleDeptHeadApprovers(String orgId, OrganizationUser currentUser, List<String> approverList, ApproverDirectionEnum direction) {
		if (currentUser == null) {
			return List.of();
		}
        String departmentId = currentUser.getDepartmentId();
        if (StringUtils.isBlank(departmentId)) {
            return List.of();
        }

        int approvalLevel = getValidLevel(approverList);
        if (approvalLevel <= 0) {
            return List.of();
        }

        // 从下往上收集所有部门负责人
        List<String> allCommanderIds = new ArrayList<>();
        String currentDepartmentId = departmentId;

        while (StringUtils.isNotBlank(currentDepartmentId)) {
            String commanderId = getDeptCommander(orgId, currentDepartmentId);
            if (StringUtils.isNotBlank(commanderId)) {
                allCommanderIds.add(commanderId);
            }
            currentDepartmentId = getParentDepartmentId(currentDepartmentId);
        }

        if (allCommanderIds.isEmpty()) {
            return List.of();
        }

        // 根据方向截取：BOTTOM_UP 取前 approvalLevel 个；TOP_DOWN 取到从顶往下第 approvalLevel 层
        List<String> resultIds;
        if (direction == ApproverDirectionEnum.TOP_DOWN) {
            int total = allCommanderIds.size();
            int endExclusive = Math.min(total, Math.max(1, total - approvalLevel + 1));
            resultIds = allCommanderIds.subList(0, endExclusive);
        } else {
            resultIds = allCommanderIds.subList(0, Math.min(allCommanderIds.size(), approvalLevel));
        }

        return resolveMemberApprovers(orgId, resultIds);
    }

    /**
     * 获取父部门ID
     */
    private String getParentDepartmentId(String departmentId) {
        Department department = departmentBaseMapper.selectByPrimaryKey(departmentId);
        if (department != null) {
            return department.getParentId();
        }
        return null;
    }

	/**
	 * 判断当前实例节点是否支持多人审批
	 * @param currentNodeId 当前节点ID
	 * @param submitterId 提审人
	 * @param currentOrgId 当前组织ID
	 * @return 是否支持多人审批
	 */
	public boolean isCurrentNodeMultiApprover(String currentNodeId, String submitterId, String currentOrgId) {
		ApprovalNodeApprover nodeApprover = approvalNodeApproverMapper.selectByPrimaryKey(currentNodeId);
		List<User> approvers = resolveApprovers(submitterId, currentOrgId, ApproverTypeEnum.valueOf(nodeApprover.getApproverType()), JSON.parseArray(nodeApprover.getApproverList(), String.class), getApproverDirection(nodeApprover.getApproverDirection()));
		return approvers.size() > 1;
	}

	/**
	 * 获取当前实例节点审批人
	 * @param instance 审批实例
	 * @param currentNodeId 当前节点ID
	 * @param currentOrgId 当前组织ID
	 * @return 审批人ID集合
	 */
	public List<String> getCurrentNodeApproverList(ApprovalInstance instance, String currentNodeId, String currentOrgId) {
		ApprovalNodeApprover nodeApprover = approvalNodeApproverMapper.selectByPrimaryKey(currentNodeId);
		if (nodeApprover == null) {
			return new ArrayList<>();
		}
		List<User> approvers = resolveApprovers(instance.getSubmitterId(), currentOrgId, ApproverTypeEnum.valueOf(nodeApprover.getApproverType()), JSON.parseArray(nodeApprover.getApproverList(), String.class), getApproverDirection(nodeApprover.getApproverDirection()));
		// 审批人为空时
		if (CollectionUtils.isEmpty(approvers)) {
			if (EmptyApproverActionEnum.valueOf(nodeApprover.getEmptyApproverAction()) != EmptyApproverActionEnum.AUTO_PASS) {
				// 指定人员, 审批管理员处理, 返回兜底审批人
				return List.of(nodeApprover.getFallbackApprover());
			}
		}

		// 审批人与提交人同一人时
		Optional<User> findSame = approvers.stream().filter(approver -> Strings.CS.equals(approver.getId(), instance.getSubmitterId())).findAny();
		if (SameSubmitterActionEnum.valueOf(nodeApprover.getSameSubmitterAction()) == SameSubmitterActionEnum.ASSIGN_SUPERIOR && findSame.isPresent()) {
			// 转交给直属上级审批
			OrganizationUser criteria = new OrganizationUser();
			criteria.setUserId(findSame.get().getId());
			criteria.setOrganizationId(currentOrgId);
			OrganizationUser currentUser = organizationUserMapper.selectOne(criteria);
			if (currentUser != null && StringUtils.isNotEmpty(currentUser.getSupervisorId())) {
				// 替换审批人列表中与提审人相同审批人 => 直属上级
				String supervisorId = currentUser.getSupervisorId();
				return approvers.stream()
						.map(approver -> Strings.CS.equals(approver.getId(), instance.getSubmitterId()) ? supervisorId : approver.getId())
						.collect(Collectors.toList());
			}
		}
		return approvers.stream().map(User::getId).toList();
	}

	/**
	 * 获取当前实例节点审批人
	 * @param approverType 审批类型
	 * @param approverList 审批人集合
	 * @param userId 用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 审批人ID集合
	 */
	public List<User> getCurrentNodeApproverList(String approverType, List<String> approverList, String userId, String currentOrgId, String approverDirection) {
		return resolveApprovers(userId, currentOrgId, ApproverTypeEnum.valueOf(approverType), approverList, getApproverDirection(approverDirection));
	}

	/**
	 * 获取当前实例节点抄送人
	 * @param currentNodeId 当前节点ID
	 * @param userId 用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 审批人ID集合
	 */
	public List<User> getCurrentNodeCcList(String currentNodeId, String userId, String currentOrgId) {
		ApprovalNodeApprover nodeApprover = approvalNodeApproverMapper.selectByPrimaryKey(currentNodeId);
		if (StringUtils.isBlank(nodeApprover.getCcType())) {
			return new ArrayList<>();
		}
		return resolveApprovers(userId, currentOrgId, ApproverTypeEnum.valueOf(nodeApprover.getCcType()), JSON.parseArray(nodeApprover.getCcList(), String.class), getApproverDirection(nodeApprover.getCcDirection()));
	}

	/**
	 * 获取当前实例节点抄送人
	 * @param ccType 抄送类型
	 * @param ccList 抄送人集合
	 * @param userId 用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 抄送人ID集合
	 */
	public List<User> getCurrentNodeCcList(String ccType, List<String> ccList, String userId, String currentOrgId, String approverDirection) {
		if (StringUtils.isBlank(ccType)) {
			return List.of();
		}
		return resolveApprovers(userId, currentOrgId, ApproverTypeEnum.valueOf(ccType), ccList, getApproverDirection(approverDirection));
	}

	/**
	 * 获取当前资源审批实例第一个节点
	 * @param instance 审批实例
	 * @param currentOrgId 组织ID
	 * @return 第一个节点
	 */
    public ApprovalNodeResponse getResourceApprovalInstanceFirstNode(ApprovalInstance instance, String currentOrgId) {
        ApprovalNode nodeCriteria = new ApprovalNode();
		nodeCriteria.setFlowVersionId(instance.getFlowVersionId());
		nodeCriteria.setNodeType(ApprovalNodeTypeEnum.START.name());
		ApprovalNode start = approvalNodeMapper.selectOne(nodeCriteria);
		List<BaseModuleFieldValue> resourceFvs = formService.compressResourceDetail(instance.getType(), instance.getResourceId());
		return getNextNodeWithExceptionHandler(instance, start.getId(), resourceFvs, currentOrgId, false);
	}

	/**
	 * 获取当前待办任务下一个节点
	 * @param currentTask 待办任务
	 * @return 下一个节点
	 */
	public ApprovalNodeResponse getTaskNextNode(ApprovalTask currentTask, ApprovalInstance instance, String currentOrgId) {
		List<BaseModuleFieldValue> resourceFvs = formService.compressResourceDetail(instance.getType(), instance.getResourceId());
		return getNextNodeWithExceptionHandler(instance, currentTask.getNodeId(), resourceFvs, currentOrgId, false);
	}

	/**
	 * 获取当前节点下一个节点
	 * @param currentNodeId 当前节点ID
	 * @return 下一个节点
	 */
	public ApprovalNodeResponse getCurrentNextNode(String currentNodeId, ApprovalInstance instance, String currentOrgId) {
		List<BaseModuleFieldValue> resourceFvs = formService.compressResourceDetail(instance.getType(), instance.getResourceId());
		return getNextNodeWithExceptionHandler(instance, currentNodeId, resourceFvs, currentOrgId, false);
	}

	/**
	 * 获取实例当前节点后续所有审批节点
	 * @param instance 当前节点ID
	 * @return 后续所有审批节点
	 */
	public List<ApprovalNodeApproverResponse> getInstanceCurrentFollowNode(ApprovalInstance instance, String currentOrgId) {
		List<ApprovalNodeApproverResponse> nodes = new ArrayList<>();
		List<BaseModuleFieldValue> resourceFvs = formService.compressResourceDetail(instance.getType(), instance.getResourceId());
		String currentNodeId = instance.getCurrentNodeId();
		if (currentNodeId.contains(SIGN_SPILT)) {
			currentNodeId = currentNodeId.split(SIGN_SPILT)[0];
		}
		ApprovalNodeResponse next = getNextNodeWithExceptionHandler(instance, currentNodeId, resourceFvs, currentOrgId, true);
		while (ApprovalNodeTypeEnum.valueOf(next.getNodeType()) == ApprovalNodeTypeEnum.APPROVER) {
			nodes.add((ApprovalNodeApproverResponse) next);
			next = getNextNodeWithExceptionHandler(instance, next.getId(), resourceFvs, currentOrgId, true);
		}
		return nodes;
	}

	/**
	 * 获取实例下一个节点，包含异常处理和自动审批, 跳过条件节点
	 * @param instance 审批实例
	 * @param nodeId 节点ID
	 * @param fieldValues 资源业务字段值
	 * @param currentOrgId 当前组织ID
	 * @return 下一个节点 (结束节点或者审批节点)
	 */
	private ApprovalNodeResponse getNextNodeWithExceptionHandler(ApprovalInstance instance, String nodeId, List<BaseModuleFieldValue> fieldValues, String currentOrgId, boolean preview) {
		ApprovalNodeResponse nextNode = getNextNode(nodeId, fieldValues);
		if (nextNode == null) {
			throw new GenericException(Translator.get("no.approval.next.node"));
		}
		if (ApprovalNodeTypeEnum.valueOf(nextNode.getNodeType()) == ApprovalNodeTypeEnum.END) {
			return nextNode;
		}

		if (ApprovalNodeTypeEnum.valueOf(nextNode.getNodeType()) == ApprovalNodeTypeEnum.APPROVER) {
			if (preview) {
				return previewNextApproverNodeWithExceptionHandler(instance, nextNode, currentOrgId);
			} else {
				Integer round = extApprovalInstanceMapper.getNodeRound(instance.getId(), nodeId);
				List<ApprovalTask> approvedTasks = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>().eq(ApprovalTask::getInstanceId, instance.getId())
						.eq(ApprovalTask::getNodeId, nodeId).eq(ApprovalTask::getNodeRound, round).eq(ApprovalTask::getStatus, ApprovalStatus.APPROVED));
				List<String> preApprovers = approvedTasks.stream().map(ApprovalTask::getApproverId).toList();
				return handleNextApproverNodeWithExceptionHandler(instance, nextNode, fieldValues, currentOrgId, preApprovers);
			}
		}
		// 条件类型节点, 继续往下获取
		return getNextNodeWithExceptionHandler(instance, nextNode.getId(), fieldValues, currentOrgId, preview);
	}

	/**
	 * 处理实例下一个审批节点，包含异常处理
	 * @param instance 审批实例
	 * @param nextNode 下一个审批节点
	 * @param fieldValues 资源业务字段值
	 * @param currentOrgId 当前组织ID
	 * @return 下一个节点 (审批节点)
	 */
	private ApprovalNodeResponse handleNextApproverNodeWithExceptionHandler(ApprovalInstance instance, ApprovalNodeResponse nextNode,
																			List<BaseModuleFieldValue> fieldValues, String currentOrgId, List<String> preApprovers) {
		ApprovalNodeApproverResponse nextApproverNode = (ApprovalNodeApproverResponse) nextNode;
		if (ApprovalTypeEnum.valueOf(nextApproverNode.getApprovalType()) == ApprovalTypeEnum.AUTO_PASS) {
			// 自动通过, 插入审批记录, 获取下一个节点
			saveAutoRecord(instance.getId(), nextApproverNode.getId(), ApprovalStatus.AUTO_APPROVED, null, null, null, false, null);
			return getNextNodeWithExceptionHandler(instance, nextApproverNode.getId(), fieldValues, currentOrgId, false);
		}
		if (ApprovalTypeEnum.valueOf(nextApproverNode.getApprovalType()) == ApprovalTypeEnum.AUTO_REJECT) {
			// 自动驳回, 插入审批记录
			saveAutoRecord(instance.getId(), nextApproverNode.getId(), ApprovalStatus.AUTO_UNAPPROVED, null, null, null, false, null);
			ApprovalNodeExceptionResponse exNode = BeanUtils.copyBean(new ApprovalNodeExceptionResponse(), nextApproverNode);
			exNode.setNodeType(ApprovalNodeTypeEnum.EXCEPTION.name());
			return exNode;
		}

		// 人工审批, 异常处理
		setNodeApproverAndCcList(nextApproverNode, instance.getSubmitterId(), currentOrgId);

		// 审批人为空时
		if (CollectionUtils.isEmpty(nextApproverNode.getApproverList())) {
			if (EmptyApproverActionEnum.valueOf(nextApproverNode.getEmptyApproverAction()) == EmptyApproverActionEnum.AUTO_PASS) {
				// 自动通过, 插入审批记录, 获取下一个节点
				saveAutoRecord(instance.getId(), nextApproverNode.getId(), ApprovalStatus.AUTO_APPROVED, "审批人为空，自动通过", null, nextApproverNode.getCcList(), true, null);
				updateApprovalPostField(instance, nextApproverNode.getId(), ApprovalAction.APPROVE);
				return getNextNodeWithExceptionHandler(instance, nextApproverNode.getId(), fieldValues, currentOrgId, false);
			} else {
				// 指定人员, 审批管理员处理, 返回兜底审批人
				nextApproverNode.setApproverType(ApproverTypeEnum.MEMBER.name());
				nextApproverNode.setApproverList(List.of(nextApproverNode.getFallbackApprover()));
			}
		}

		// 审批人与提交人同一人时
		Optional<String> findSame = nextApproverNode.getApproverList().stream().filter(approver -> Strings.CS.equals(approver, instance.getSubmitterId())).findAny();
		if (findSame.isPresent()) {
			SameSubmitterActionEnum sameAction = SameSubmitterActionEnum.valueOf(nextApproverNode.getSameSubmitterAction());
			switch (sameAction) {
				case ASSIGN_SUPERIOR -> {
					// 转交给直属上级审批
					OrganizationUser criteria = new OrganizationUser();
					criteria.setUserId(findSame.get());
					criteria.setOrganizationId(currentOrgId);
					OrganizationUser currentUser = organizationUserMapper.selectOne(criteria);
					if (currentUser != null && StringUtils.isNotEmpty(currentUser.getSupervisorId())) {
						// 替换审批人列表中与提审人相同审批人 => 直属上级
						String supervisorId = currentUser.getSupervisorId();
						List<String> newApproverList = nextApproverNode.getApproverList().stream()
								.map(approver -> Strings.CS.equals(approver, instance.getSubmitterId()) ? supervisorId : approver)
								.collect(Collectors.toList());
						nextApproverNode.setApproverList(newApproverList);
					} else {
						// 不存在直属上级, 自动跳过
						ApprovalTask autoTask = saveAutoSkipTask(instance.getId(), nextApproverNode.getId(), findSame.get());
						saveAutoRecord(instance.getId(), nextApproverNode.getId(), ApprovalStatus.AUTO_APPROVED, "审批人与提交人为同一人时, 直属上级为空, 自动通过", autoTask.getId(), nextApproverNode.getCcList(), true, autoTask.getNodeRound());
						updateApprovalPostField(instance, nextApproverNode.getId(), ApprovalAction.APPROVE);
						return getNextNodeWithExceptionHandler(instance, nextApproverNode.getId(), fieldValues, currentOrgId, false);
					}
				}
				case SKIP -> {
					// 自动跳过
					if (nextApproverNode.getApproverList().size() == 1 || MultiApproverModeEnum.valueOf(nextApproverNode.getMultiApproverMode()) == MultiApproverModeEnum.ANY) {
						// 如果刚好为单人审批, 多人或签, 直接插入待办任务和记录, 流转到下一个节点
						ApprovalTask autoTask = saveAutoSkipTask(instance.getId(), nextApproverNode.getId(), findSame.get());
						saveAutoRecord(instance.getId(), nextApproverNode.getId(), ApprovalStatus.AUTO_APPROVED, "审批人与提交人为同一人时, 自动通过", autoTask.getId(), nextApproverNode.getCcList(), true, autoTask.getNodeRound());
						updateApprovalPostField(instance, nextApproverNode.getId(), ApprovalAction.APPROVE);
						return getNextNodeWithExceptionHandler(instance, nextApproverNode.getId(), fieldValues, currentOrgId, false);
					}
				}
				default -> {

				}
			}
		}

		return handleNextNodeDuplicateApprover(instance, nextApproverNode, fieldValues, currentOrgId, preApprovers);
	}

	/**
	 * 处理下一个节点 (审批人重复跳过)
	 * @param instance 审批实例
	 * @param nextApproverNode 下一个审批节点
	 * @param fieldValues 资源字段值
	 * @param currentOrgId 当前组织ID
	 * @return 下一个节点
	 */
	private ApprovalNodeResponse handleNextNodeDuplicateApprover(ApprovalInstance instance, ApprovalNodeApproverResponse nextApproverNode, List<BaseModuleFieldValue> fieldValues, String currentOrgId, List<String> preApprovers) {
		// 重复审批人处理
		DuplicateApproverRuleEnum duplicateApproverRuleEnum = getFlowDuplicateApproverRule(instance.getFlowVersionId());
		List<String> autoSkipUser = getFlowAutoSkipUser(instance, nextApproverNode.getId(), preApprovers);
		if (duplicateApproverRuleEnum == DuplicateApproverRuleEnum.FIRST_ONLY || duplicateApproverRuleEnum == DuplicateApproverRuleEnum.SEQUENTIAL_ALL) {
			// 仅首个节点需要审批, 后续节点自动同意 (当前节点审批人在之前在有不同的节点审批过待办任务)
			Optional<String> anySkip = nextApproverNode.getApproverList().stream().filter(autoSkipUser::contains).findAny();
			if (anySkip.isPresent()) {
				if (nextApproverNode.getApproverList().size() == 1 || MultiApproverModeEnum.valueOf(nextApproverNode.getMultiApproverMode()) == MultiApproverModeEnum.ANY) {
					// 如果为单人审批, 或签, 直接同意, 且流转到下一个节点
					ApprovalTask autoTask = saveAutoSkipTask(instance.getId(), nextApproverNode.getId(), anySkip.get());
					saveAutoRecord(instance.getId(), nextApproverNode.getId(), ApprovalStatus.AUTO_APPROVED, "审批人重复出现, 后续节点自动通过", autoTask.getId(), nextApproverNode.getCcList(), true, autoTask.getNodeRound());
					updateApprovalPostField(instance, nextApproverNode.getId(), ApprovalAction.APPROVE);
					return getNextNodeWithExceptionHandler(instance, nextApproverNode.getId(), fieldValues, currentOrgId, false);
				}
			}
		}
		return nextApproverNode;
	}

	/**
	 * 获取当前审批流重复审批人配置
	 * @param flowVersionId 审批流版本
	 * @return 重复审批人配置
	 */
	public DuplicateApproverRuleEnum getFlowDuplicateApproverRule(String flowVersionId) {
		ApprovalFlowVersion flowVersion = approvalFlowVersionMapper.selectByPrimaryKey(flowVersionId);
		ApprovalFlow approvalFlow = approvalFlowMapper.selectByPrimaryKey(flowVersion.getFlowId());
		return DuplicateApproverRuleEnum.valueOf(approvalFlow.getDuplicateApproverRule());
	}

	/**
	 * 预览实例下一个审批节点，包含异常处理
	 * @param instance 审批实例
	 * @param nextNode 下一个审批节点
	 * @param currentOrgId 当前组织ID
	 * @return 下一个节点 (审批节点)
	 */
	private ApprovalNodeResponse previewNextApproverNodeWithExceptionHandler(ApprovalInstance instance, ApprovalNodeResponse nextNode, String currentOrgId) {
		ApprovalNodeApproverResponse nextApproverNode = (ApprovalNodeApproverResponse) nextNode;
		if (ApprovalTypeEnum.valueOf(nextApproverNode.getApprovalType()) == ApprovalTypeEnum.AUTO_PASS || ApprovalTypeEnum.valueOf(nextApproverNode.getApprovalType()) == ApprovalTypeEnum.AUTO_REJECT) {
			// 自动节点, 直接返回
			return nextApproverNode;
		}

		// 人工审批, 异常处理
		setNodeApproverAndCcList(nextApproverNode, instance.getSubmitterId(), currentOrgId);

		// 审批人为空时
		if (CollectionUtils.isEmpty(nextApproverNode.getApproverList()) && EmptyApproverActionEnum.valueOf(nextApproverNode.getEmptyApproverAction()) != EmptyApproverActionEnum.AUTO_PASS) {
			// 指定人员, 审批管理员处理, 返回兜底审批人
			nextApproverNode.setApproverType(ApproverTypeEnum.MEMBER.name());
			nextApproverNode.setApproverList(List.of(nextApproverNode.getFallbackApprover()));
		}

		// 审批人与提交人同一人时
		Optional<String> findSame = nextApproverNode.getApproverList().stream().filter(approver -> Strings.CS.equals(approver, instance.getSubmitterId())).findAny();
		if (findSame.isEmpty()) {
			return nextApproverNode;
		}
		if (SameSubmitterActionEnum.valueOf(nextApproverNode.getSameSubmitterAction()) == SameSubmitterActionEnum.ASSIGN_SUPERIOR) {
			// 转交给直属上级审批
			OrganizationUser criteria = new OrganizationUser();
			criteria.setUserId(findSame.get());
			criteria.setOrganizationId(currentOrgId);
			OrganizationUser currentUser = organizationUserMapper.selectOne(criteria);
			if (currentUser != null && StringUtils.isNotEmpty(currentUser.getSupervisorId())) {
				// 替换审批人列表中与提审人相同审批人 => 直属上级
				String supervisorId = currentUser.getSupervisorId();
				List<String> newApproverList = nextApproverNode.getApproverList().stream()
						.map(approver -> Strings.CS.equals(approver, instance.getSubmitterId()) ? supervisorId : approver)
						.collect(Collectors.toList());
				nextApproverNode.setApproverList(newApproverList);
			}
		}

		return nextApproverNode;
	}

	/**
	 * 自动跳过的待办任务
	 *
	 * @param instanceId 实例ID
	 * @param nodeId 节点ID
	 */
	private ApprovalTask saveAutoSkipTask(String instanceId, String nodeId, String approver) {
		Integer nextRound = extApprovalInstanceMapper.getNextNodeRound(instanceId, nodeId);
		ApprovalTask task = new ApprovalTask();
		task.setId(IDGenerator.nextStr());
		task.setInstanceId(instanceId);
		task.setNodeId(nodeId);
		task.setNodeRound(nextRound);
		task.setApproverId(approver);
		task.setType(ApprovalTaskType.NL.name());
		task.setStatus(ApprovalStatus.AUTO_APPROVED.name());
		task.setAction(ApprovalAction.APPROVE.name());
		task.setCreateTime(System.currentTimeMillis());
		task.setCreateUser(InternalUser.ADMIN.getValue());
		task.setUpdateTime(System.currentTimeMillis());
		task.setUpdateUser(InternalUser.ADMIN.getValue());
		approvalTaskMapper.insert(task);
		return task;
	}

	/**
	 * 自动审批的记录
	 *
	 * @param instanceId 实例ID
	 * @param nodeId 自动审批的节点ID
	 * @param approvalStatus 审批状态
	 */
	public void saveAutoRecord(String instanceId, String nodeId, ApprovalStatus approvalStatus, String comment, String taskId, List<String> ccList, boolean sendCc, Integer nodeRound) {
		if (nodeRound == null) {
			nodeRound = extApprovalInstanceMapper.getNextNodeRound(instanceId, nodeId);
		}

		ApprovalRecord record = new ApprovalRecord();
		record.setId(IDGenerator.nextStr());
		if (StringUtils.isNotBlank(taskId)) {
			record.setTaskId(taskId);
		}
		record.setInstanceId(instanceId);
		record.setNodeId(nodeId);
		record.setNodeRound(nodeRound);
		record.setResult(approvalStatus.name());
		if (StringUtils.isBlank(comment)) {
			record.setComment(approvalStatus == ApprovalStatus.AUTO_APPROVED ? Translator.get("auto.approval.passed") : Translator.get("auto.approval.rejected"));
		} else {
			record.setComment(comment);
		}
		record.setCreateTime(System.currentTimeMillis());
		record.setCreateUser(InternalUser.ADMIN.getValue());
		record.setUpdateTime(System.currentTimeMillis());
		record.setUpdateUser(InternalUser.ADMIN.getValue());
		approvalRecordMapper.insert(record);
		if (sendCc && CollectionUtils.isNotEmpty(ccList)) {
			// 节点执行完成, 发送抄送
			ApprovalActionService approvalActionService = CommonBeanFactory.getBean(ApprovalActionService.class);
			if (approvalActionService != null) {
				List<ApprovalTask> ccTasks = approvalActionService.getNodeCcTasks(nodeId, ccList, instanceId, InternalUser.ADMIN.getValue());
				approvalTaskMapper.batchInsert(ccTasks);
			}
		}
	}

	/**
	 * 获取审批流自动跳过的审批人ID集合
	 * @param instance 审批实例
	 * @param currentNodeId 当前节点ID
	 * @param preApprovers 上一个节点的审批人集合
	 * @return 审批人ID集合
	 */
	public List<String> getFlowAutoSkipUser(ApprovalInstance instance, String currentNodeId, List<String> preApprovers) {
		List<String> autoSkipUserIds = new ArrayList<>();
		DuplicateApproverRuleEnum duplicateApproverRuleEnum = getFlowDuplicateApproverRule(instance.getFlowVersionId());
		if (duplicateApproverRuleEnum == DuplicateApproverRuleEnum.FIRST_ONLY) {
			// 仅首个节点需审批(之前审批过的), 后续审批节点跳过
			List<ApprovalTask> alreadyApprovedTasks = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>()
					.eq(ApprovalTask::getInstanceId, instance.getId()).eq(ApprovalTask::getStatus, ApprovalStatus.APPROVED.name()));
			autoSkipUserIds = alreadyApprovedTasks.stream().filter(task -> !Strings.CI.equals(task.getNodeId(), currentNodeId) && task.getNodeRound() != -1).map(ApprovalTask::getApproverId).toList();
		} else if (duplicateApproverRuleEnum == DuplicateApproverRuleEnum.SEQUENTIAL_ALL){
			// 连续审批人跳过 (上一个连续审批人)
			autoSkipUserIds.addAll(preApprovers);
		}
		return autoSkipUserIds;
	}

	/**
	 * 设置节点的审批人集合和抄送人集合
	 * @param approverNode 审批节点
	 * @param submitter 提交人
	 * @param currentOrgId 当前组织ID
	 */
	private void setNodeApproverAndCcList(ApprovalNodeApproverResponse approverNode, String submitter, String currentOrgId) {
		if (approverNode == null) {
			return;
		}
		List<User> nextApprovers = getCurrentNodeApproverList(approverNode.getApproverType(), approverNode.getApproverList(), submitter, currentOrgId, approverNode.getApproverDirection());
		approverNode.setApproverList(nextApprovers.stream().map(User::getId).distinct().collect(Collectors.toList()));
		List<User> nextCcList = getCurrentNodeCcList(approverNode.getCcType(), approverNode.getCcList(), submitter, currentOrgId, approverNode.getCcDirection());
		approverNode.setCcList(nextCcList.stream().map(User::getId).distinct().collect(Collectors.toList()));
	}

	/**
	 * 审批后操作 (字段更新)
	 * @param instance 审批实例
	 * @param currentNodeId 当前字段ID
	 * @param action 审批动作
	 */
	public void updateApprovalPostField(ApprovalInstance instance, String currentNodeId, ApprovalAction action) {
		ApprovalResourceService resourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
		if (resourceService != null) {
			ApprovalNodeApprover approvalNodeApprover = approvalNodeApproverMapper.selectByPrimaryKey(currentNodeId);
			resourceService.updateApprovalPostField(FormKey.ofKey(instance.getType()), instance.getResourceId(),
					action == ApprovalAction.APPROVE ? approvalNodeApprover.getPassPostConfig() : approvalNodeApprover.getRejectPostConfig());
		}
	}
}
