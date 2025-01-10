package io.cordys.crm.system.service;

import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.crm.system.mapper.ExtUserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseService {
    @Resource
    private ExtUserMapper extUserMapper;


    /**
     * 设置创建人和更新人名称
     * @param object
     * @return
     * @param <T>
     */
    public <T> T setCreateAndUpdateUserName(T object) {
        return setCreateAndUpdateUserName(List.of(object)).get(0);
    }

    /**
     * 设置创建人和更新人名称
     * @param list
     * @return
     * @param <T>
     */
    public <T> List<T> setCreateAndUpdateUserName(List<T> list) {
        try {
            Set<String> userIds = new HashSet<>();
            for (T role : list) {
                Method getCreateUser = role.getClass().getMethod("getCreateUser");
                Method getUpdateUser = role.getClass().getMethod("getUpdateUser");
                userIds.add((String) getCreateUser.invoke(role));
                userIds.add((String) getUpdateUser.invoke(role));
            }

            Map<String, String> userNameMap = getUserNameMap(userIds);

            for (T role : list) {
                Method setCreateUserName = role.getClass().getMethod("setCreateUserName", String.class);
                Method setUpdateUserName = role.getClass().getMethod("setUpdateUserName", String.class);
                Method getCreateUser = role.getClass().getMethod("getCreateUser");
                Method getUpdateUser = role.getClass().getMethod("getUpdateUser");
                setCreateUserName.invoke(role, userNameMap.get(getCreateUser.invoke(role)));
                setUpdateUserName.invoke(role, userNameMap.get(getUpdateUser.invoke(role)));
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

}