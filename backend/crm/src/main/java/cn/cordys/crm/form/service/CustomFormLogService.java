package cn.cordys.crm.form.service;

import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private ExtUserMapper extUserMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences.forEach(differ -> {
            if (Strings.CS.equals(differ.getColumn(), "enable")) {
                differ.setColumnName(Translator.get("log.enable"));
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(Translator.get(Boolean.parseBoolean(differ.getOldValue().toString()) ? "log.enable.true" : "log.enable.false"));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(Translator.get(Boolean.parseBoolean(differ.getNewValue().toString()) ? "log.enable.true" : "log.enable.false"));
                }
            } else if (Strings.CS.equals(differ.getColumn(), "adminUserIds")) {
                handleAdminUserIdsLogDetail(differ);
            } else if (Strings.CS.equals(differ.getColumn(), "roleUsers")) {
                handleRoleUsersLogDetail(differ);
            } else if (Strings.CS.equals(differ.getColumn(), "name")) {
				differ.setColumnName(Translator.get("log.form." + differ.getColumn()));
				differ.setOldValueName(differ.getOldValue());
				differ.setNewValueName(differ.getNewValue());
			} else if (Strings.CS.equals(differ.getColumn(), "fields")) {
				differ.setColumnName(Translator.get("log." + differ.getColumn()));
				handleFieldsLogDetail(differ);
			} else if (Strings.CS.equals("formProp", differ.getColumn())) {
				differ.setColumnName(Translator.get("log.form.prop"));
				handleFormPropLogDetail(differ);
			} else {
                translatorDifferInfo(differ);
            }
        });
        differences.removeIf(differ -> differ.getOldValueName() == null && differ.getNewValueName() == null);
        return differences;
    }

    @SuppressWarnings("unchecked")
    private void handleAdminUserIdsLogDetail(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.adminUserIds"));
        Map<String, String> userNameMap = getUserNameMapFromValues(differ.getOldValue(), differ.getNewValue());

        if (differ.getOldValue() instanceof List<?> oldUserIds) {
            differ.setOldValueName(formatUserNames(oldUserIds, userNameMap));
        }
        if (differ.getNewValue() instanceof List<?> newUserIds) {
            differ.setNewValueName(formatUserNames(newUserIds, userNameMap));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRoleUsersLogDetail(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.roleUsers"));

        Map<String, List<String>> oldRoleUsers = new HashMap<>();
        Map<String, List<String>> newRoleUsers = new HashMap<>();
        Set<String> allUserIds = new HashSet<>();
        Set<String> allRoleIds = new HashSet<>();

        if (differ.getOldValue() instanceof Map<?, ?> oldMap) {
            oldMap.forEach((roleId, userIds) -> {
                String roleIdStr = String.valueOf(roleId);
                List<String> uids = (List<String>) userIds;
                oldRoleUsers.put(roleIdStr, uids);
                allUserIds.addAll(uids);
                allRoleIds.add(roleIdStr);
            });
        }
        if (differ.getNewValue() instanceof Map<?, ?> newMap) {
            newMap.forEach((roleId, userIds) -> {
                String roleIdStr = String.valueOf(roleId);
                List<String> uids = (List<String>) userIds;
                newRoleUsers.put(roleIdStr, uids);
                allUserIds.addAll(uids);
                allRoleIds.add(roleIdStr);
            });
        }

        Map<String, String> userNameMap = getUserNameMapByIds(new ArrayList<>(allUserIds));
        Map<String, String> roleNameMap = getRoleNameMap(allRoleIds);

        if (!oldRoleUsers.isEmpty()) {
            differ.setOldValueName(formatRoleUsersText(oldRoleUsers, roleNameMap, userNameMap));
        }
        if (!newRoleUsers.isEmpty()) {
            differ.setNewValueName(formatRoleUsersText(newRoleUsers, roleNameMap, userNameMap));
        }
    }

    private String formatUserNames(List<?> userIds, Map<String, String> userNameMap) {
        return userIds.stream()
                .map(id -> userNameMap.getOrDefault(String.valueOf(id), String.valueOf(id)))
                .collect(Collectors.joining(", "));
    }

    private String formatRoleUsersText(Map<String, List<String>> roleUsers,
                                        Map<String, String> roleNameMap,
                                        Map<String, String> userNameMap) {
        return roleUsers.entrySet().stream()
                .map(entry -> {
                    String roleName = roleNameMap.getOrDefault(entry.getKey(), entry.getKey());
                    String userNames = entry.getValue().stream()
                            .map(uid -> userNameMap.getOrDefault(uid, uid))
                            .collect(Collectors.joining(", "));
                    return roleName + ": " + userNames;
                })
                .collect(Collectors.joining("\n"));
    }

    private Map<String, String> getUserNameMapFromValues(Object oldValue, Object newValue) {
        List<String> userIds = new ArrayList<>();
        if (oldValue instanceof List<?> oldList) {
            oldList.forEach(id -> userIds.add(String.valueOf(id)));
        }
        if (newValue instanceof List<?> newList) {
            newList.forEach(id -> userIds.add(String.valueOf(id)));
        }
        return getUserNameMapByIds(userIds);
    }

    private Map<String, String> getUserNameMapByIds(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return extUserMapper.selectUserOptionByIds(userIds).stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName, (a, b) -> a));
    }

    private Map<String, String> getRoleNameMap(Set<String> roleIds) {
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CustomFormRole> roles = customFormRoleMapper.selectByIds(new ArrayList<>(roleIds));
        return roles.stream().collect(Collectors.toMap(
                CustomFormRole::getId,
                role -> Translator.get(role.getName(), role.getName()),
                (a, b) -> a
        ));
    }
}
