package io.cordys.crm.system.service;

import io.cordys.common.constants.InternalRole;
import io.cordys.common.constants.RoleDataScope;
import io.cordys.common.context.OrganizationContext;
import io.cordys.common.exception.GenericException;
import io.cordys.common.permission.Permission;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.RolePermission;
import io.cordys.crm.system.domain.UserRole;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.*;
import io.cordys.crm.system.domain.Role;

/**
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RoleService {

    @Resource
    private BaseMapper<Role> roleMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private BaseMapper<RolePermission> rolePermissionMapper;

    public List<RoleListResponse> list() {
        List<Role> roles = roleMapper.select(new Role());
        return JSON.parseArray(JSON.toJSONString(roles), RoleListResponse.class);
    }


    public RoleGetResponse get(String id) {
        Role role = roleMapper.selectByPrimaryKey(id);
        RoleGetResponse roleGetResponse = BeanUtils.copyBean(new RoleGetResponse(), role);
        // do something...
        return roleGetResponse;
    }

    public Role add(RoleAddRequest request, String userId) {
        Role role = BeanUtils.copyBean(new Role(), request);
        role.setId(IDGenerator.nextStr());
        role.setCreateTime(System.currentTimeMillis());
        role.setUpdateTime(System.currentTimeMillis());
        role.setUpdateUser(userId);
        role.setCreateUser(userId);
        role.setInternal(false);
        role.setOrganizationId(OrganizationContext.getOrganizationId());
        // 创建默认仅可查看
        role.setDataScope(RoleDataScope.SELF.name());
        roleMapper.insert(role);
        return role;
    }

    public Role update(RoleUpdateRequest request, String userId) {
        Role role = BeanUtils.copyBean(new Role(), request);
        role.setUpdateTime(System.currentTimeMillis());
        role.setUpdateUser(userId);
        roleMapper.update(role);
        return roleMapper.selectByPrimaryKey(role.getId());
    }

    public void delete(String id) {
        roleMapper.deleteByPrimaryKey(id);
    }

    public List<PermissionDefinitionItem> getPermissionSetting(String id) {
        // 获取角色
        Role role = roleMapper.selectByPrimaryKey(id);
        // 获取该角色拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(role.getId());
        // 获取所有的权限
        List<PermissionDefinitionItem> permissionDefinitions = getPermissionDefinitions();
        // 设置勾选项
        for (PermissionDefinitionItem firstLevel : permissionDefinitions) {
            List<PermissionDefinitionItem> children = firstLevel.getChildren();
            boolean allCheck = true;
            firstLevel.setName(Translator.get(firstLevel.getName()));
            for (PermissionDefinitionItem secondLevel : children) {
                List<Permission> permissions = secondLevel.getPermissions();
                secondLevel.setName(Translator.get(secondLevel.getName()));
                if (CollectionUtils.isEmpty(permissions)) {
                    continue;
                }
                boolean secondAllCheck = true;
                for (Permission p : permissions) {
                    if (StringUtils.isNotBlank(p.getName())) {
                        // 有 name 字段翻译 name 字段
                        p.setName(Translator.get(p.getName()));
                    } else {
                        p.setName(translateDefaultPermissionName(p));
                    }
                    // 管理员默认勾选全部二级权限位
                    if (permissionIds.contains(p.getId()) || StringUtils.equals(role.getId(), InternalRole.ORG_ADMIN.getValue())) {
                        p.setEnable(true);
                    } else {
                        // 如果权限有未勾选，则二级菜单设置为未勾选
                        p.setEnable(false);
                        secondAllCheck = false;
                    }
                }
                secondLevel.setEnable(secondAllCheck);
                if (!secondAllCheck) {
                    // 如果二级菜单有未勾选，则一级菜单设置为未勾选
                    allCheck = false;
                }
            }
            firstLevel.setEnable(allCheck);
        }

        return permissionDefinitions;
    }

    public List<PermissionDefinitionItem> getPermissionDefinitions() {
        List<PermissionDefinitionItem> permissionDefinitions = null;
        try {
            Enumeration<URL> urls = this.getClass().getClassLoader().getResources("permission.json");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String content = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
                if (StringUtils.isBlank(content)) {
                    continue;
                }
                List<PermissionDefinitionItem> temp = JSON.parseArray(content, PermissionDefinitionItem.class);
                if (permissionDefinitions == null) {
                    permissionDefinitions = temp;
                } else {
                    permissionDefinitions.addAll(temp);
                }
            }
        } catch (IOException e) {
            throw new GenericException(e);
        }
        return permissionDefinitions;
    }

    private String translateDefaultPermissionName(Permission p) {
        if (StringUtils.isNotBlank(p.getName())) {
            p.getName();
        }
        String[] idSplit = p.getId().split(":");
        String permissionKey = idSplit[idSplit.length - 1];

        String permissionName = switch (permissionKey) {
            case "READ":
                yield Translator.get("permission.read");
            case "READ+ADD":
                yield Translator.get("permission.add");
            case "READ+UPDATE":
                yield Translator.get("permission.edit");
            case "READ+DELETE":
                yield Translator.get("permission.delete");
            case "READ+IMPORT":
                yield Translator.get("permission.import");
            case "READ+RECOVER":
                yield Translator.get("permission.recover");
            case "READ+EXPORT":
                yield Translator.get("permission.export");
            case "READ+EXECUTE":
                yield Translator.get("permission.execute");
            case "READ+DEBUG":
                yield Translator.get("permission.debug");
            default:
                throw new GenericException("Unknown permission key: " + permissionKey);
        };
        return Translator.get(permissionName);
    }

    /**
     * 查询用户组对应的权限ID
     *
     * @param roleId
     * @return
     */
    public Set<String> getPermissionIdSetByRoleId(String roleId) {
        return getRolePermissionByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }

    /**
     * 查询用户组对应的权限列表
     *
     * @param roleId
     * @return
     */
    public List<RolePermission> getRolePermissionByRoleId(String roleId) {
        RolePermission example = new RolePermission();
        example.setRoleId(roleId);
        return rolePermissionMapper.select(example);
    }

    public Set<String> getPermissionIdsByUserId(String userId) {
        return getPermissionsByUserId(userId)
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }

    public List<RolePermission> getPermissionsByUserId(String userId) {
        List<UserRole> userRoles = getUserRolesByUserId(userId);
        List<String> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        return rolePermissionMapper.selectByColumn("role_id", roleIds.toArray(new String[0]));
    }

    private List<UserRole> getUserRolesByUserId(String userId) {
        UserRole example = new UserRole();
        example.setUserId(userId);
        return userRoleMapper.select(example);
    }

}