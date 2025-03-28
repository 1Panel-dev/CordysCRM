package io.cordys.common.service;

import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.JSON;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
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
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;


    /**
     * 设置创建人和更新人名称
     *
     * @param object
     * @param <T>
     * @return
     */
    public <T> T setCreateAndUpdateUserName(T object) {
        return setCreateAndUpdateUserName(List.of(object)).get(0);
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
                setCreateUserName.invoke(item, userNameMap.get(getCreateUser.invoke(item)));
                setUpdateUserName.invoke(item, userNameMap.get(getUpdateUser.invoke(item)));
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
        return setCreateUpdateOwnerUserName(List.of(object)).get(0);
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
                setCreateUserName.invoke(item, userNameMap.get(getCreateUser.invoke(item)));
                setUpdateUserName.invoke(item, userNameMap.get(getUpdateUser.invoke(item)));
                setOwnerName.invoke(item, userNameMap.get(getOwner.invoke(item)));
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
        Map originCustomer = JSON.parseMap(JSON.toJSONString(resource));
        if (moduleFields != null) {
            moduleFields.forEach(field ->
                    originCustomer.put(field.getFieldId(), field.getFieldValue()));
        }

        try {

            Class<?> clazz = resource.getClass();
            Method getId = clazz.getMethod("getId");
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId((String) getId.invoke(resource))
                            .modifiedValue(originCustomer)
                            .build()
            );
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

    public <T> void handleUpdateLog(T originResource,
                                    T modifiedResource,
                                    List<BaseModuleFieldValue> originResourceFields,
                                    List<BaseModuleFieldValue> modifiedResourceFields) {

        Map originResourceLog = JSON.parseMap(JSON.toJSONString(originResource));
        if (modifiedResourceFields != null && originResourceFields != null) {
            originResourceFields.forEach(field ->
                    originResourceLog.put(field.getFieldId(), field.getFieldValue()));
        }

        Map modifiedResourceLog = JSON.parseMap(JSON.toJSONString(modifiedResource));
        if (modifiedResourceFields != null) {
            modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field ->
                            modifiedResourceLog.put(field.getFieldId(), field.getFieldValue()));
        }

        try {

            Class<?> clazz = originResource.getClass();
            Method getId = clazz.getMethod("getId");
            Method getName = clazz.getMethod("getName");

            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId((String) getId.invoke(originResource))
                            .resourceName((String) getName.invoke(originResource))
                            .originalValue(originResourceLog)
                            .modifiedValue(modifiedResourceLog)
                            .build()
            );
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }
}