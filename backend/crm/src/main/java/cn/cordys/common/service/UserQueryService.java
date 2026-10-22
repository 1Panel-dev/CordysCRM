package cn.cordys.common.service;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 查询用户、部门及补全用户名称。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserQueryService {
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    /**
     * 设置创建人和更新人名称
     */
    public <T> T setCreateAndUpdateUserName(T object) {
        return setCreateAndUpdateUserName(List.of(object)).getFirst();
    }

    /**
     * 设置创建人和更新人名称
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
     */
    public <T> T setCreateUpdateOwnerUserName(T object) {
        return setCreateUpdateOwnerUserName(List.of(object)).getFirst();
    }

    /**
     * 设置创建人、更新人和责任人名称
     */
    public <T> T setCreateAndUpdateOwnerUserName(T object) {
        return setCreateUpdateOwnerUserName(List.of(object)).getFirst();
    }

    /**
     * 设置创建人、更新人和责任人名称
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

    public String getDeptIdByUserId(String userId, String orgId) {
        OrganizationUser criteria = new OrganizationUser();
        criteria.setOrganizationId(orgId);
        criteria.setUserId(userId);
        OrganizationUser organizationUser = organizationUserMapper.selectOne(criteria);
        return organizationUser != null ? organizationUser.getDepartmentId() : null;
    }

    public Map<String, UserDeptDTO> getUserDeptMapByUserIds(List<String> ownerIds, String orgId) {
        if (CollectionUtils.isEmpty(ownerIds)) {
            return Collections.emptyMap();
        }
        return extUserMapper.getUserDeptByUserIds(ownerIds, orgId)
                .stream()
                .collect(Collectors.toMap(UserDeptDTO::getUserId, Function.identity()));
    }

    public Map<String, UserResponse> getUserDepAndPhoneByUserIds(List<String> ownerIds, String orgId) {
        if (CollectionUtils.isEmpty(ownerIds)) {
            return Collections.emptyMap();
        }
        List<UserResponse> userResponseList = extOrganizationUserMapper.getUserDepAndPhoneByUserIds(ownerIds, orgId);
        return userResponseList.stream().collect(Collectors.toMap(UserResponse::getUserId, Function.identity()));
    }

    public String getAndCheckOptionName(String option) {
        return option == null ? Translator.get("common.option.not_exist") : option;
    }
}
