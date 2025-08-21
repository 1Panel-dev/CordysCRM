package io.cordys.crm.customer.service;

import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.permission.Permission;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.service.BaseModuleLogService;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.crm.system.service.RoleService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class RoleLogService extends BaseModuleLogService {

    @Resource
    private DepartmentService departmentService;
    @Resource
    private RoleService roleService;

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        differenceDTOS.forEach(differ -> {
            if (Strings.CS.equals(differ.getColumn(), "deptIds")) {
                handleDeptIdsLogDetail(differ);
            } else if (Strings.CS.equals(differ.getColumn(), "permissions")) {
                handlePermissionSettingLogDetail(differ);
            } else if (Strings.CS.equals(differ.getColumn(), "dataScope")) {
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(Translator.get("role.data_permission." + differ.getOldValue().toString().toLowerCase()));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(Translator.get("role.data_permission." + differ.getNewValue().toString().toLowerCase()));
                }
                differ.setColumnName(Translator.get("log.dataScope"));
            } else {
                BaseModuleLogService.translatorDifferInfo(differ);
            }
        });
    }

    private void handleDeptIdsLogDetail(JsonDifferenceDTO differ) {
        Object oldValue = differ.getOldValue();
        Object newValue = differ.getNewValue();
        Map<String, String> deptNameMap = getDeptNameMap(oldValue, newValue);
        if (oldValue != null && oldValue instanceof List originDeptIds) {
            List<String> deptNames = originDeptIds.stream()
                    .map(deptNameMap::get)
                    .filter(name -> name != null)
                    .toList();
            differ.setOldValueName(deptNames);
        }
        if (newValue != null && newValue instanceof List newDeptIds) {
            List<String> deptNames = newDeptIds.stream()
                    .map(deptNameMap::get)
                    .filter(name -> name != null)
                    .toList();
            differ.setNewValueName(deptNames);
        }
        differ.setColumnName(Translator.get("role.log.dept.name"));
    }

    private void handlePermissionSettingLogDetail(JsonDifferenceDTO differ) {
        Object oldValue = differ.getOldValue();
        Set<String> originPermissionSet = new HashSet<>();
        if (oldValue != null && oldValue instanceof List originPermissionIds) {
            originPermissionSet.addAll(originPermissionIds);
        }
        Object newValue = differ.getNewValue();
        Set<String> newPermissionSet = new HashSet<>();
        if (newValue != null && newValue instanceof List newPermissionIds) {
            newPermissionSet.addAll(newPermissionIds);
        }
        Map<String, List<String>> originPermissionMap = new HashMap<>();
        Map<String, List<String>> newPermissionMap = new HashMap<>();
        List<PermissionDefinitionItem> permissionDefinitions = roleService.getPermissionDefinitions();
        for (PermissionDefinitionItem permissionDefinition : permissionDefinitions) {
            for (PermissionDefinitionItem resourceItem : permissionDefinition.getChildren()) {
                String resourceName = Translator.get(resourceItem.getName());
                List<String> originPermissionNames = new ArrayList<>();
                List<String> newPermissionNames = new ArrayList<>();
                for (Permission permissionItem : resourceItem.getPermissions()) {
                    if (originPermissionSet.contains(permissionItem.getId())) {
                        originPermissionNames.add(translatePermissionName(permissionItem));
                    }
                    if (newPermissionSet.contains(permissionItem.getId())) {
                        newPermissionNames.add(translatePermissionName(permissionItem));
                    }
                }
                if (!originPermissionNames.isEmpty()) {
                    originPermissionMap.put(resourceName, originPermissionNames);
                }
                if (!newPermissionNames.isEmpty()) {
                    newPermissionMap.put(resourceName, newPermissionNames);
                }
            }
        }

        differ.setOldValueName(getPermissionColumnName(originPermissionMap));
        differ.setNewValueName(getPermissionColumnName(newPermissionMap));

        differ.setColumnName(Translator.get("role.log.permission.name"));
    }

    private String getPermissionColumnName(Map<String, List<String>> permissionMap) {
        StringBuilder sb = new StringBuilder();
        permissionMap.forEach((originPermissionName, originPermissionNames) -> {
            sb.append(originPermissionName).append(": ");
            for (String permissionName : originPermissionNames) {
                sb.append(permissionName).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("\n");
        });
        return sb.toString();
    }

    private String translatePermissionName(Permission permissionItem) {
        if (StringUtils.isNotBlank(permissionItem.getName())) {
            // 有 name 字段翻译 name 字段
            return Translator.get(permissionItem.getName());
        } else {
            return roleService.translateDefaultPermissionName(permissionItem);
        }
    }

    private Map<String, String> getDeptNameMap(Object oldValue, Object newValue) {
        List<String> deptIds = new ArrayList<>();
        if (oldValue != null && oldValue instanceof List originDeptIds) {
            deptIds.addAll(originDeptIds);
        }
        if (newValue != null && newValue instanceof List newDeptIds) {
            deptIds.addAll(newDeptIds);
        }
        return departmentService.getDepartmentOptionsByIds(deptIds)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }
}