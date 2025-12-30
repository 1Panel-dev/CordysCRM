package cn.cordys.common.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;
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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
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
            moduleFields.forEach(field ->
                    resourceLog.put(field.getFieldId(), field.getFieldValue())
            );
        }

        writeAddLogContext(resource, resourceLog);
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
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(resourceLog, moduleFields, fieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey);
        }

        writeAddLogContext(resource, resourceLog);
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

    public <T> void handleUpdateLog(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name) {

        Map<String, Object> originResourceLog = JSON.parseToMap(JSON.toJSONString(originResource));
        Map<String, Object> modifiedResourceLog = JSON.parseToMap(JSON.toJSONString(modifiedResource));

        // 添加原始字段值
        if (originResourceFields != null) {
            originResourceFields.forEach(field ->
                    originResourceLog.put(field.getFieldId(), field.getFieldValue())
            );
        }

        // 添加修改后的字段值（过滤无效字段）
        if (modifiedResourceFields != null) {
            modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field ->
                            modifiedResourceLog.put(field.getFieldId(), field.getFieldValue())
                    );
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
        if (originResourceFields != null) {
            Map<String, String> oldFieldNameMap = getFieldNameMap(originResourceFields, moduleFormConfigDTO);
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(originResourceLog, originResourceFields, oldFieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey);
        }

        if (modifiedResourceFields != null) {
            Map<String, String> newFieldNameMap = getFieldNameMap(modifiedResourceFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(modifiedResourceLog, validFields, newFieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey);
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

    /**
     * 处理普通字段 + 子表字段
     */
    private void fillResourceLog(
            Map<String, Object> resourceLog,
            List<BaseModuleFieldValue> fields,
            Map<String, String> fieldNameMap,
            Map<String, String> subTableKeyMap,
            String subTableKeyName,
            Set<String> subRefKey) {
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
            int size = subTableList.size();
            for (int i = 0; i < size; i++) {
                Map<String, Object> row = subTableList.get(i);
                if (size > 1) {
                    int finalI = i;
                    row.forEach((key, value) -> {
                        if (!fieldNameMap.containsKey(key) || subRefKey.contains(key)) {
                            return;
                        }
                        resourceLog.put(subTableKeyName + "-" + fieldNameMap.get(key) + "-" + Translator.get("row") + (finalI + 1) + "-" + key, value);
                    });
                } else {
                    row.forEach((key, value) -> {
                        if (!fieldNameMap.containsKey(key) || subRefKey.contains(key)) {
                            return;
                        }
                        resourceLog.put(subTableKeyName + "-" + fieldNameMap.get(key) + "-" + key, value);
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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

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
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }

    public String getAndCheckOptionName(String option) {
        return option == null ? Translator.get("common.option.not_exist") : option;
    }

}