package cn.cordys.crm.customer.service;

import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.permission.Permission;
import cn.cordys.common.permission.PermissionDefinitionItem;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.RoleService;
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
        var oldValue = differ.getOldValue();
        var newValue = differ.getNewValue();
        var deptNameMap = getDeptNameMap(oldValue, newValue);

        if (oldValue instanceof List<?> originDeptIds) {
            var deptNames = originDeptIds.stream()
                    .map(id -> deptNameMap.get(String.valueOf(id)))
                    .filter(Objects::nonNull)
                    .toList();
            differ.setOldValueName(deptNames);
        }
        if (newValue instanceof List<?> newDeptIds) {
            var deptNames = newDeptIds.stream()
                    .map(id -> deptNameMap.get(String.valueOf(id)))
                    .filter(Objects::nonNull)
                    .toList();
            differ.setNewValueName(deptNames);
        }
        differ.setColumnName(Translator.get("role.log.dept.name"));
    }

    private void handlePermissionSettingLogDetail(JsonDifferenceDTO differ) {
        var oldValue = differ.getOldValue();
        var originPermissionSet = new HashSet<String>();
        if (oldValue instanceof List<?> originPermissionIds) {
            originPermissionIds.forEach(id -> originPermissionSet.add(String.valueOf(id)));
        }
        var newValue = differ.getNewValue();
        var newPermissionSet = new HashSet<String>();
        if (newValue instanceof List<?> newPermissionIds) {
            newPermissionIds.forEach(id -> newPermissionSet.add(String.valueOf(id)));
        }

        var originPermissionMap = new HashMap<String, List<String>>();
        var newPermissionMap = new HashMap<String, List<String>>();
        var permissionDefinitions = roleService.getPermissionDefinitions();
        for (PermissionDefinitionItem permissionDefinition : permissionDefinitions) {
            for (PermissionDefinitionItem resourceItem : permissionDefinition.getChildren()) {
                var resourceName = Translator.get(resourceItem.getName());
                var originPermissionNames = new ArrayList<String>();
                var newPermissionNames = new ArrayList<String>();
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
        var sb = new StringBuilder();
        permissionMap.forEach((resource, names) -> {
            sb.append(resource).append(": ").append(String.join(",", names)).append("\n");
        });
        return sb.toString();
    }

    private String translatePermissionName(Permission permissionItem) {
        if (StringUtils.isNotBlank(permissionItem.getName())) {
            return Translator.get(permissionItem.getName());
        } else {
            return roleService.translateDefaultPermissionName(permissionItem);
        }
    }

    private Map<String, String> getDeptNameMap(Object oldValue, Object newValue) {
        var deptIds = new ArrayList<String>();
        if (oldValue instanceof List<?> originDeptIds) {
            originDeptIds.forEach(id -> deptIds.add(String.valueOf(id)));
        }
        if (newValue instanceof List<?> newDeptIds) {
            newDeptIds.forEach(id -> deptIds.add(String.valueOf(id)));
        }
        return departmentService.getDepartmentOptionsByIds(deptIds)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }
}