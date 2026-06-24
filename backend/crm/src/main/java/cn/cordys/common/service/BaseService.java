package cn.cordys.common.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeLink;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalInstanceService;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.mapper.ExtAttachmentMapper;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseService {
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private ExtAttachmentMapper extAttachmentMapper;
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
     * 设置创建人和更新人名称
     *
     * @param object
     * @param <T>
     * @return
     */
    public <T> T setCreateAndUpdateUserName(T object) {
        return setCreateAndUpdateUserName(List.of(object)).getFirst();
    }

    /**
     * 设置创建人和更新人名称
     *
     * @param list
     * @param <T>
     * @return
     */
    public <T> List<T> setCreateAndUpdateUserName(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        try {

            Class<?> clazz = list.getFirst().getClass();
            Method setCreateUserName = clazz.getMethod("setCreateUserName", String.class);
            Method setUpdateUserName = clazz.getMethod("setUpdateUserName", String.class);
            Method getCreateUser = clazz.getMethod("getCreateUser");
            Method getUpdateUser = clazz.getMethod("getUpdateUser");

            Set<String> userIds = new HashSet<>();
            for (T role : list) {
                userIds.add((String) getCreateUser.invoke(role));
                userIds.add((String) getUpdateUser.invoke(role));
            }

            Map<String, String> userNameMap = getUserNameMap(userIds);
            for (T item : list) {
                String createUserId = (String) getCreateUser.invoke(item);
                String updateUserId = (String) getUpdateUser.invoke(item);

                String createUserName = getAndCheckOptionName(userNameMap.get(createUserId));
                String updateUserName = getAndCheckOptionName(userNameMap.get(updateUserId));

                setCreateUserName.invoke(item, createUserName);
                setUpdateUserName.invoke(item, updateUserName);
            }
        } catch (Exception e) {
            throw new GenericException(e);
        }
        return list;
    }

    /**
     * 设置创建人、更新人和责任人名称
     *
     * @param object
     * @param <T>
     * @return
     */
    public <T> T setCreateUpdateOwnerUserName(T object) {
        return setCreateUpdateOwnerUserName(List.of(object)).getFirst();
    }

    /**
     * 设置创建人、更新人和责任人名称
     *
     * @param object
     * @param <T>
     * @return
     */
    public <T> T setCreateAndUpdateOwnerUserName(T object) {
        return setCreateUpdateOwnerUserName(List.of(object)).getFirst();
    }

    /**
     * 设置创建人、更新人和责任人名称
     *
     * @param list
     * @param <T>
     * @return
     */
    public <T> List<T> setCreateUpdateOwnerUserName(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        try {

            Class<?> clazz = list.getFirst().getClass();
            Method setCreateUserName = clazz.getMethod("setCreateUserName", String.class);
            Method setUpdateUserName = clazz.getMethod("setUpdateUserName", String.class);
            Method setOwnerName = clazz.getMethod("setOwnerName", String.class);
            Method getCreateUser = clazz.getMethod("getCreateUser");
            Method getUpdateUser = clazz.getMethod("getUpdateUser");
            Method getOwner = clazz.getMethod("getOwner");

            Set<String> userIds = new HashSet<>();
            for (T role : list) {
                userIds.add((String) getCreateUser.invoke(role));
                userIds.add((String) getUpdateUser.invoke(role));
                userIds.add((String) getOwner.invoke(role));
            }

            Map<String, String> userNameMap = getUserNameMap(userIds);
            for (T item : list) {

                String createUserId = (String) getCreateUser.invoke(item);
                String updateUserId = (String) getUpdateUser.invoke(item);
                String ownerId = (String) getOwner.invoke(item);

                String createUserName = getAndCheckOptionName(userNameMap.get(createUserId));
                String updateUserName = getAndCheckOptionName(userNameMap.get(updateUserId));
                String ownerName = getAndCheckOptionName(userNameMap.get(ownerId));

                setCreateUserName.invoke(item, createUserName);
                setUpdateUserName.invoke(item, updateUserName);
                setOwnerName.invoke(item, ownerName);
            }
        } catch (Exception e) {
            throw new GenericException(e);
        }
        return list;
    }

    /**
     * 根据用户ID列表，获取用户ID和名称的映射
     *
     * @param userIds
     * @return
     */
    public Map<String, String> getUserNameMap(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return extUserMapper.selectUserOptionByIds(userIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    public String getUserName(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null) {
            return user.getName();
        }
        return null;
    }

    /**
     * 根据用户ID列表，获取用户ID和名称的映射
     *
     * @param userIds
     * @return
     */
    public Map<String, String> getUserNameMap(Set<String> userIds) {
        return getUserNameMap(new ArrayList<>(userIds));
    }

    public Map<String, UserDeptDTO> getUserDeptMapByUserIds(Set<String> ownerIds, String orgId) {
        return getUserDeptMapByUserIds(new ArrayList<>(ownerIds), orgId);
    }

    public UserDeptDTO getUserDeptMapByUserId(String ownerId, String orgId) {
        List<UserDeptDTO> userDeptList = extUserMapper.getUserDeptByUserIds(List.of(ownerId), orgId);
        return CollectionUtils.isEmpty(userDeptList) ? null : userDeptList.getFirst();
    }

    public Map<String, UserDeptDTO> getUserDeptMapByUserIds(List<String> ownerIds, String orgId) {
        if (CollectionUtils.isEmpty(ownerIds)) {
            return Collections.emptyMap();
        }
        return extUserMapper.getUserDeptByUserIds(ownerIds, orgId)
                .stream()
                .collect(Collectors.toMap(UserDeptDTO::getUserId, Function.identity()));
    }


    /**
     * 获取联系人ID和名称的映射
     *
     * @param contactIds
     * @return
     */
    public Map<String, String> getContactMap(List<String> contactIds) {
        if (CollectionUtils.isEmpty(contactIds)) {
            return Collections.emptyMap();
        }
        return extCustomerContactMapper.selectContactOptionByIds(contactIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }


    public Map<String, UserResponse> getUserDepAndPhoneByUserIds(List<String> ownerIds, String orgId) {
        if (CollectionUtils.isEmpty(ownerIds)) {
            return Collections.emptyMap();
        }
        List<UserResponse> userResponseList = extOrganizationUserMapper.getUserDepAndPhoneByUserIds(ownerIds, orgId);
        return userResponseList.stream().collect(Collectors.toMap(UserResponse::getUserId, Function.identity()));
    }

    public <T> void handleAddLog(T resource, List<BaseModuleFieldValue> moduleFields) {
        Map<String, Object> resourceLog = JSON.parseToMap(JSON.toJSONString(resource));

        if (moduleFields != null) {
            moduleFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field ->
                            resourceLog.put(field.getFieldId(), field.getFieldValue())
                    );
        }

        writeAddLogContext(resource, resourceLog);
    }

	/**
	 * 新增日志, 替换资源名称
	 * @param resource 资源信息
	 * @param moduleFields 自定义字段
	 * @param <T> 资源类型
	 */
	public <T> void handleAddLogWithResourceName(T resource, List<BaseModuleFieldValue> moduleFields) {
		Map<String, Object> resourceLog = JSON.parseToMap(JSON.toJSONString(resource));

		if (moduleFields != null) {
			moduleFields.stream()
					.filter(BaseModuleFieldValue::valid)
					.forEach(field ->
							resourceLog.put(field.getFieldId(), field.getFieldValue())
					);
		}

		writeAddLogContextWithResourceName(resource, resourceLog);
	}

    public <T> void handleAddLogWithSubTable(
            T resource,
            List<BaseModuleFieldValue> moduleFields,
            String subTableKeyName,
            ModuleFormConfigDTO moduleFormConfigDTO) {

        Map<String, Object> resourceLog = JSON.parseToMap(JSON.toJSONString(resource));
        Set<String> subRefKey = getSubTableRefIds(moduleFormConfigDTO);
        if (moduleFields != null) {
            Map<String, String> fieldNameMap = getFieldNameMap(moduleFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = moduleFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(resourceLog, validFields, fieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey, moduleFormConfigDTO);
        }

		writeAddLogContextWithResourceName(resource, resourceLog);
    }


    /**
     * 写入操作日志上下文
     */
    private <T> void writeAddLogContext(T resource, Map<String, Object> resourceLog) {
        try {
            Method idGetter = resource.getClass().getMethod("getId");
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId((String) idGetter.invoke(resource))
                            .modifiedValue(resourceLog)
                            .build()
            );
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

	/**
	 * 写入操作日志上下文，替换资源名称 (参数资源名存在占位符)
	 * @param resource 资源
	 * @param resourceLog 日志信息
	 * @param <T> 资源类型
	 */
	private <T> void writeAddLogContextWithResourceName(T resource, Map<String, Object> resourceLog) {
		try {
			Method idGetter = resource.getClass().getMethod("getId");
			Method nameGetter = resource.getClass().getMethod("getName");
			OperationLogContext.setContext(
					LogContextInfo.builder()
							.resourceId((String) idGetter.invoke(resource))
							.resourceName((String) nameGetter.invoke(resource))
							.modifiedValue(resourceLog)
							.build()
			);
		} catch (Exception e) {
			throw new GenericException(e);
		}
	}

    public <T> void handleUpdateLog(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name) {

        Map<String, Object> originResourceLog = JSON.parseToMap(JSON.toJSONString(originResource));
        Map<String, Object> modifiedResourceLog = JSON.parseToMap(JSON.toJSONString(modifiedResource));

        // 收集所有字段ID，检测附件类型字段
        Set<String> allFieldIds = new HashSet<>();
        if (originResourceFields != null) {
            originResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> allFieldIds.add(field.getFieldId()));
        }
        if (modifiedResourceFields != null) {
            modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> allFieldIds.add(field.getFieldId()));
        }
        Set<String> attachmentFieldIds = filterAttachmentFieldIds(allFieldIds);

        // 添加原始字段值（附件类型字段将ID替换为名称）
        if (originResourceFields != null) {
            originResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> {
                        Object value = field.getFieldValue();
                        if (attachmentFieldIds.contains(field.getFieldId())) {
                            value = replaceAttachmentIdsWithNames(value);
                        }
                        originResourceLog.put(field.getFieldId(), value);
                    });
        }

        // 添加修改后的字段值（过滤无效字段，附件类型字段将ID替换为名称）
        if (modifiedResourceFields != null) {
            modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> {
                        Object value = field.getFieldValue();
                        if (attachmentFieldIds.contains(field.getFieldId())) {
                            value = replaceAttachmentIdsWithNames(value);
                        }
                        modifiedResourceLog.put(field.getFieldId(), value);
                    });
        }

        try {
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId(id)
                            .resourceName(name)
                            .originalValue(originResourceLog)
                            .modifiedValue(modifiedResourceLog)
                            .build()
            );
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }


    public <T> void handleUpdateLogWithSubTable(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name,
            String subTableKeyName,
            ModuleFormConfigDTO moduleFormConfigDTO) {

        Map<String, Object> originResourceLog = JSON.parseToMap(JSON.toJSONString(originResource));
        Map<String, Object> modifiedResourceLog = JSON.parseToMap(JSON.toJSONString(modifiedResource));
        Set<String> subRefKey = getSubTableRefIds(moduleFormConfigDTO);

        // 从表单配置中获取附件类型字段ID集合
        Set<String> attachmentFieldIds = getAttachmentFieldIdsFromFormConfig(moduleFormConfigDTO);

        if (CollectionUtils.isNotEmpty(originResourceFields)) {
            Map<String, String> oldFieldNameMap = getFieldNameMap(originResourceFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = originResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(originResourceLog, validFields, oldFieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey, moduleFormConfigDTO);
        }

        if (CollectionUtils.isNotEmpty(modifiedResourceFields)) {
            Map<String, String> newFieldNameMap = getFieldNameMap(modifiedResourceFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(modifiedResourceLog, validFields, newFieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey, moduleFormConfigDTO);
        }

        // 将附件字段中的附件ID替换为附件名称
        replaceAttachmentValuesInLog(originResourceLog, attachmentFieldIds);
        replaceAttachmentValuesInLog(modifiedResourceLog, attachmentFieldIds);

        try {
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId(id)
                            .resourceName(name)
                            .originalValue(originResourceLog)
                            .modifiedValue(modifiedResourceLog)
                            .build()
            );
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

    /**
     * 处理普通字段 + 子表字段
     */
    private void fillResourceLog(
            Map<String, Object> resourceLog,
            List<BaseModuleFieldValue> fields,
            Map<String, String> fieldNameMap,
            Map<String, String> subTableKeyMap,
            String subTableKeyName,
            Set<String> subRefKey,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        fields.forEach(field -> {
            String fieldId = field.getFieldId();
            // 普通字段
            if (!subTableKeyMap.containsKey(fieldId) && !subTableKeyMap.containsValue(fieldId)) {
                resourceLog.put(fieldId, field.getFieldValue());
                return;
            }

            // 子表字段
            List<Map<String, Object>> subTableList =
                    JSON.parseArray(JSON.toJSONString(field.getFieldValue()), new TypeReference<>() {
                    });
            if (CollectionUtils.isEmpty(subTableList)) {
                return;
            }
            AtomicReference<String> subTableName = new AtomicReference<>(moduleFormConfigDTO.getFields().stream().filter(BaseField::isSubField).filter(subField -> subField.getId().equals(fieldId)).findFirst().map(BaseField::getName).orElse(subTableKeyName));
            if (Strings.CI.equals(fieldId, "products")) {
                subTableKeyMap.forEach((key, value) -> {
                    if (StringUtils.isNotBlank(value) && Strings.CI.equals(value, fieldId))
                        subTableName.set(moduleFormConfigDTO.getFields().stream().filter(BaseField::isSubField).filter(subField -> subField.getId().equals(key)).findFirst().map(BaseField::getName).orElse(subTableKeyName));
                });
            }


            int size = subTableList.size();
            for (int i = 0; i < size; i++) {
                Map<String, Object> row = subTableList.get(i);
                if (size > 1) {
                    int finalI = i;
                    String finalSubTableName1 = subTableName.get();
                    row.forEach((key, value) -> {
                        if (!fieldNameMap.containsKey(key) || subRefKey.contains(key)) {
                            return;
                        }
                        resourceLog.put(finalSubTableName1 + "-" + fieldNameMap.get(key) + "-" + Translator.get("row") + (finalI + 1) + "-" + key, value);
                    });
                } else {
                    String finalSubTableName = subTableName.get();
                    row.forEach((key, value) -> {
                        if (!fieldNameMap.containsKey(key) || subRefKey.contains(key)) {
                            return;
                        }
                        resourceLog.put(finalSubTableName + "-" + fieldNameMap.get(key) + "-" + key, value);
                    });
                }
            }
        });
    }


    /**
     * 字段 ID → 字段名称 映射表
     */
    private Map<String, String> getFieldNameMap(List<BaseModuleFieldValue> fields, ModuleFormConfigDTO moduleFormConfigDTO) {
        List<String> fieldIds = new ArrayList<>(fields.stream()
                .map(BaseModuleFieldValue::getFieldId)
                .distinct()
                .toList());
        List<OptionDTO> fieldOptions = extModuleFieldMapper.getSourceOptionsByIds("sys_module_field", fieldIds);
        Map<String, String> nameMap = fieldOptions.stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));

        if (CollectionUtils.isNotEmpty(moduleFormConfigDTO.getFields())) {
            for (BaseField field : moduleFormConfigDTO.getFields()) {
                if (field.isSubField()) {
                    if (field instanceof SubField objectSubField) {
                        objectSubField.getSubFields().forEach(subField -> {
                            nameMap.put(StringUtils.isNotBlank(subField.getBusinessKey()) ? subField.getBusinessKey() : subField.getId(), subField.getName());
                        });
                    }
                }

            }
        }
        return nameMap;
    }

    public Map<String, String> getSubTableIdKeyMap(ModuleFormConfigDTO moduleFormConfigDTO) {
        Map<String, String> subTableIdKeyMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(moduleFormConfigDTO.getFields())) {
            for (BaseField field : moduleFormConfigDTO.getFields()) {
                if (field.isSubField()) {
                    subTableIdKeyMap.put(field.getId(), field.getBusinessKey());
                }
            }
        }
        return subTableIdKeyMap;

    }

    /**
     * 获取子表引用字段ID集合
     *
     * @param formConfig 表单陪配置
     * @return 子表引用字段ID集合
     */
    private Set<String> getSubTableRefIds(ModuleFormConfigDTO formConfig) {
        if (CollectionUtils.isNotEmpty(formConfig.getFields())) {
            Optional<BaseField> subOptional = formConfig.getFields().stream().filter(BaseField::isSubField).findAny();
            if (subOptional.isPresent()) {
                SubField subField = (SubField) subOptional.get();
                return subField.getSubFields().stream()
                        .filter(f -> StringUtils.isNotBlank(f.getResourceFieldId()))
                        .map(BaseField::getId)
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }


    /**
     * 客户id与名称映射
     *
     * @param customerIds
     * @return
     */
    public Map<String, String> getCustomerMap(List<String> customerIds) {
        if (CollectionUtils.isEmpty(customerIds)) {
            return Collections.emptyMap();
        }
        return extCustomerMapper.getCustomerOptionsByIds(customerIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    /**
     * 商机id与名称映射
     *
     * @param opportunityIds
     * @return
     */
    public Map<String, String> getOpportunityMap(List<String> opportunityIds) {
        if (CollectionUtils.isEmpty(opportunityIds)) {
            return Collections.emptyMap();
        }
        return extOpportunityMapper.getOpportunityOptionsByIds(opportunityIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }


    /**
     * 线索id与名称映射
     *
     * @param clueIds
     * @return
     */
    public Map<String, String> getClueMap(List<String> clueIds) {
        if (CollectionUtils.isEmpty(clueIds)) {
            return Collections.emptyMap();
        }
        return extClueMapper.selectOptionByIds(clueIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));

    }

    /**
     * 联系人id和电话映射
     *
     * @param contactIds
     * @return
     */
    public Map<String, String> getContactPhone(List<String> contactIds) {
        if (CollectionUtils.isEmpty(contactIds)) {
            return Collections.emptyMap();
        }
        return extCustomerContactMapper.selectContactPhoneOptionByIds(contactIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    public String getAndCheckOptionName(String option) {
        return option == null ? Translator.get("common.option.not_exist") : option;
    }

	/**
	 * 获取审批中的业务资源是否第一个审批节点已通过
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
		List<String> flowVersionIds = latestInstances.stream().map(ApprovalInstance::getFlowVersionId).distinct().toList();
		List<ApprovalNodeLink> nodeLinks = nodeLinkMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalNodeLink>().in(ApprovalNodeLink::getFlowVersionId, flowVersionIds));
		List<ApprovalNode> allNodes = approvalNodeMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalNode>().in(ApprovalNode::getFlowVersionId, flowVersionIds));
		List<ApprovalTask> allTasks = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>().in(ApprovalTask::getInstanceId, latestInstances.stream().map(ApprovalInstance::getId).toList()));
		Map<String, String> nodeTypeMap = allNodes.stream().collect(Collectors.toMap(ApprovalNode::getId, ApprovalNode::getNodeType));
		Map<String, List<String>> nodeLinkMap = nodeLinks.stream().filter(nodeLink -> nodeTypeMap.containsKey(nodeLink.getToNodeId()) && ApprovalNodeTypeEnum.valueOf(nodeTypeMap.get(nodeLink.getToNodeId())) != ApprovalNodeTypeEnum.END)
				.collect(Collectors.groupingBy(ApprovalNodeLink::getToNodeId, Collectors.mapping(ApprovalNodeLink::getFromNodeId, Collectors.toList())));
		latestInstances.forEach(latestInstance -> {
			if (isFirstApproverNode(nodeLinkMap, nodeTypeMap, latestInstance.getCurrentNodeId())) {
				// 当前审批实例处于第一个审批节点, 所以第一个节点为审批中
				boolean multiApprover = approvalFlowService.isCurrentNodeMultiApprover(latestInstance.getCurrentNodeId(), latestInstance.getSubmitterId(), currentOrgId);
				if (!multiApprover) {
					// 单人审批, 判断是否存在加签通过
					Integer nodeRound = extApprovalInstanceMapper.getNodeRound(latestInstance.getId(), latestInstance.getCurrentNodeId());
					Optional<ApprovalTask> signApproved = allTasks.stream().filter(task -> Strings.CI.equals(task.getType(), ApprovalTaskType.SN.name()) && Strings.CI.equals(task.getNodeId(), latestInstance.getCurrentNodeId())
							&& Strings.CI.equals(task.getInstanceId(), latestInstance.getId()) && nodeRound.equals(task.getNodeRound()) && Strings.CI.equals(task.getStatus(), ApprovalStatus.APPROVED.name())).findAny();
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
	 * 当前节点是否第一个审批节点
	 * @param nodeLinkMap 节点链接信息(toNodeId -> fromNodeId列表)
	 * @param nodeTypeMap 节点类型集合
	 * @param currentNodeId 当前节点ID
	 * @return 是否第一个审批节点
	 */
	private boolean isFirstApproverNode(Map<String, List<String>> nodeLinkMap, Map<String, String> nodeTypeMap, String currentNodeId) {
		return isFirstApproverNodeRecursive(nodeLinkMap, nodeTypeMap, currentNodeId, new HashSet<>());
	}

	/**
	 * 递归判断当前节点是否第一个审批节点
	 * @param nodeLinkMap 节点链接信息
	 * @param nodeTypeMap 节点类型集合
	 * @param nodeId 当前节点ID
	 * @param visited 已访问的节点集合(防止循环)
	 * @return 是否第一个审批节点
	 */
	private boolean isFirstApproverNodeRecursive(Map<String, List<String>> nodeLinkMap, Map<String, String> nodeTypeMap, String nodeId, Set<String> visited) {
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

	/**
	 * 从字段ID集合中筛选出附件类型的字段ID
	 *
	 * @param fieldIds 字段ID集合
	 * @return 附件类型字段ID集合
	 */
	private Set<String> filterAttachmentFieldIds(Set<String> fieldIds) {
		if (fieldIds.isEmpty()) {
			return Collections.emptySet();
		}
		LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.in(ModuleField::getId, new ArrayList<>(fieldIds))
				.eq(ModuleField::getType, FieldType.ATTACHMENT.name());
		List<ModuleField> attachmentFields = moduleFieldMapper.selectListByLambda(queryWrapper);
		return attachmentFields.stream().map(ModuleField::getId).collect(Collectors.toSet());
	}

	/**
	 * 将附件字段值中的附件ID替换为附件名称
	 * 在记录操作日志时调用，确保即使附件被删除，日志中仍保留附件名称
	 *
	 * @param fieldValue 附件字段值（附件ID列表）
	 * @return 替换为附件名称后的值
	 */
	private Object replaceAttachmentIdsWithNames(Object fieldValue) {
		if (fieldValue == null) {
			return null;
		}
		List<String> ids;
		if (fieldValue instanceof List) {
			ids = ((List<?>) fieldValue).stream().map(String::valueOf).toList();
		} else {
			ids = JSON.parseArray(fieldValue.toString(), String.class);
		}
		if (CollectionUtils.isEmpty(ids)) {
			return fieldValue;
		}
		List<String> names = extAttachmentMapper.selectNameByIds(ids);
		if (CollectionUtils.isNotEmpty(names)) {
			return names;
		}
		return fieldValue;
	}

	/**
	 * 从表单配置中获取附件类型字段ID集合
	 *
	 * @param formConfig 表单配置
	 * @return 附件类型字段ID集合
	 */
	private Set<String> getAttachmentFieldIdsFromFormConfig(ModuleFormConfigDTO formConfig) {
		if (formConfig == null || CollectionUtils.isEmpty(formConfig.getFields())) {
			return Collections.emptySet();
		}
		return formConfig.getFields().stream()
				.filter(field -> Strings.CS.equals(field.getType(), FieldType.ATTACHMENT.name()))
				.map(BaseField::getId)
				.collect(Collectors.toSet());
	}

	/**
	 * 替换日志中附件字段的值（将附件ID替换为附件名称）
	 *
	 * @param resourceLog      日志数据
	 * @param attachmentFieldIds 附件类型字段ID集合
	 */
	private void replaceAttachmentValuesInLog(Map<String, Object> resourceLog, Set<String> attachmentFieldIds) {
		if (CollectionUtils.isEmpty(attachmentFieldIds) || resourceLog == null) {
			return;
		}
		for (String fieldId : attachmentFieldIds) {
			if (resourceLog.containsKey(fieldId)) {
				resourceLog.put(fieldId, replaceAttachmentIdsWithNames(resourceLog.get(fieldId)));
			}
		}
	}
}