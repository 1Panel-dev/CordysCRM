package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.InternalRole;
import io.cordys.common.constants.RoleDataScope;
import io.cordys.common.dto.RoleDataScopeDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.permission.Permission;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.RolePermission;
import io.cordys.crm.system.domain.RoleScopeDept;
import io.cordys.crm.system.domain.UserRole;
import io.cordys.crm.system.dto.log.RoleLogDTO;
import io.cordys.crm.system.dto.request.PermissionUpdateRequest;
import io.cordys.crm.system.dto.request.RoleAddRequest;
import io.cordys.crm.system.dto.request.RoleUpdateRequest;
import io.cordys.crm.system.dto.response.RoleGetResponse;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.crm.system.mapper.ExtRoleMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.cordys.crm.system.constants.SystemResultCode.INTERNAL_ROLE_PERMISSION;
import static io.cordys.crm.system.constants.SystemResultCode.ROLE_EXIST;

/**
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RoleService {

    @Resource
    private BaseMapper<Role> roleMapper;
    @Resource
    private ExtRoleMapper extRoleMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private BaseMapper<RoleScopeDept> roleScopeDeptMapper;
    @Resource
    private BaseMapper<RolePermission> rolePermissionMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private PermissionCache permissionCache;

    public List<RoleListResponse> list(String orgId) {
        Role role = new Role();
        role.setOrganizationId(orgId);
        List<Role> roles = roleMapper.select(role);
        List<RoleListResponse> roleListResponseList = JSON.parseArray(JSON.toJSONString(roles), RoleListResponse.class);
        // 翻译内置角色名称
        roleListResponseList.stream()
                .filter(RoleListResponse::getInternal)
                .forEach(this::translateInternalRole);
        // 按创建时间排序
        roleListResponseList.sort(Comparator.comparingLong(RoleListResponse::getCreateTime));
        return baseService.setCreateAndUpdateUserName(roleListResponseList);
    }

    /**
     * 翻译内置角色名
     *
     * @param role
     * @return
     */
    public Role translateInternalRole(Role role) {
        if (BooleanUtils.isTrue(role.getInternal())) {
            role.setName(translateInternalRole(role.getName()));
        }
        return role;
    }

    /**
     * 翻译内置角色名
     *
     * @param roleKey
     * @return
     */
    public String translateInternalRole(String roleKey) {
        return Translator.get("role." + roleKey, roleKey);
    }

    public RoleGetResponse get(String id) {
        Role role = roleMapper.selectByPrimaryKey(id);
        RoleGetResponse roleGetResponse = BeanUtils.copyBean(new RoleGetResponse(), role);
        translateInternalRole(roleGetResponse);
        roleGetResponse.setPermissions(getPermissionSetting(id));
        if (StringUtils.equals(role.getDataScope(), RoleDataScope.DEPT_CUSTOM.name())) {
            roleGetResponse.setDeptIds(getDeptIdsByRoleId(id));
        }
        return baseService.setCreateAndUpdateUserName(roleGetResponse);
    }

    private List<String> getDeptIdsByRoleId(String roleId) {
        RoleScopeDept example = new RoleScopeDept();
        example.setRoleId(roleId);
        return roleScopeDeptMapper.select(example)
                .stream()
                .map(RoleScopeDept::getDepartmentId)
                .collect(Collectors.toList());
    }

    public List<String> getDeptIdsByRoleIds(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        LambdaQueryWrapper<RoleScopeDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoleScopeDept::getRoleId, roleIds);
        return roleScopeDeptMapper.selectListByLambda(wrapper)
                .stream()
                .map(RoleScopeDept::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());
    }

    @OperationLog(module = LogModule.SYSTEM_ROLE, type = LogType.ADD, resourceName = "{#request.name}")
    public Role add(RoleAddRequest request, String userId, String orgId) {
        Role role = BeanUtils.copyBean(new Role(), request);
        role.setId(IDGenerator.nextStr());
        role.setCreateTime(System.currentTimeMillis());
        role.setUpdateTime(System.currentTimeMillis());
        role.setUpdateUser(userId);
        role.setCreateUser(userId);
        role.setInternal(false);
        role.setOrganizationId(orgId);
        // 创建默认仅可查看本人数据
        role.setDataScope(Optional.ofNullable(request.getDataScope()).orElse(RoleDataScope.SELF.name()));
        // 校验名称重复
        checkAddExist(role);
        roleMapper.insert(role);

        // 配置指定部门权限
        if (StringUtils.equals(request.getDataScope(), RoleDataScope.DEPT_CUSTOM.name()) && CollectionUtils.isNotEmpty(request.getDeptIds())) {
            roleScopeDeptMapper.batchInsert(getRoleScopeDept(role.getId(), request.getDeptIds()));
        }

        // 配置权限
        insertRolePermission(request.getPermissions(), role.getId(), userId);

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(role.getId())
                        .modifiedValue(role)
                        .build()
        );

        return role;
    }

    private List<RoleScopeDept> getRoleScopeDept(String roleId, List<String> deptIds) {
        return deptIds.stream().map(deptId -> {
            RoleScopeDept roleScopeDept = new RoleScopeDept();
            roleScopeDept.setId(IDGenerator.nextStr());
            roleScopeDept.setRoleId(roleId);
            roleScopeDept.setDepartmentId(deptId);
            return roleScopeDept;
        }).collect(Collectors.toList());
    }

    @OperationLog(module = LogModule.SYSTEM_ROLE, type = LogType.UPDATE, resourceId = "{#request.id}")
    public Role update(RoleUpdateRequest request, String userId, String orgId) {
        Role originRole = roleMapper.selectByPrimaryKey(request.getId());
        if (originRole == null) {
            throw new GenericException(Translator.get("role.not_exist"));
        }

        Role role = BeanUtils.copyBean(new Role(), request);
        // 校验名称重复
        checkUpdateExist(role);
        if (originRole.getInternal()) {
            // 内置角色名称不能修改
            role.setName(null);
        }
        role.setUpdateTime(System.currentTimeMillis());
        role.setUpdateUser(userId);
        roleMapper.update(role);

        RoleGetResponse roleGetResponse = get(role.getId());

        String dataScope = request.getDataScope();
        if (StringUtils.isNotBlank(dataScope)
                && StringUtils.equals(originRole.getDataScope(), RoleDataScope.DEPT_CUSTOM.name())
                && !StringUtils.equals(dataScope, RoleDataScope.DEPT_CUSTOM.name())) {
            // 如果从自定义部门改为其他数据权限，则删除部门关联关系
            deleteRoleScopeDeptByRoleId(role.getId());
        }

        List<String> originDeptIds = null;
        if (request.getDeptIds() != null) {
            originDeptIds = getDeptIdsByRoleId(request.getId());
        }

        List<String> originPermissionIds = null;
        if (request.getPermissions() != null) {
            originPermissionIds = getPermissionIds(List.of(role.getId())).stream().toList();
        }

        // 配置指定部门权限
        if (StringUtils.equals(dataScope, RoleDataScope.DEPT_CUSTOM.name()) && request.getDeptIds() != null) {
            // 先删除
            deleteRoleScopeDeptByRoleId(role.getId());

            // 再添加
            if (CollectionUtils.isNotEmpty(request.getDeptIds())) {
                roleScopeDeptMapper.batchInsert(getRoleScopeDept(role.getId(), request.getDeptIds()));
            }
        }

        // 更新权限设置
        updatePermissionSetting(request.getPermissions(), role.getId(), userId);

        clearPermissionCacheByRoleId(role.getId(), orgId);

        RoleLogDTO originRoleLogDTO = BeanUtils.copyBean(new RoleLogDTO(), originRole);
        RoleLogDTO modifiedRoleLogDTO = BeanUtils.copyBean(new RoleLogDTO(), roleMapper.selectByPrimaryKey(request.getId()));
        if (request.getDeptIds() != null) {
            modifiedRoleLogDTO.setDeptIds(request.getDeptIds());
            originRoleLogDTO.setDeptIds(originDeptIds);
        }

        if (request.getPermissions() != null) {
            List<String> newPermissionIds = request.getPermissions().stream().filter(PermissionUpdateRequest::getEnable)
                    .map(PermissionUpdateRequest::getId)
                    .toList();
            if (!(newPermissionIds.containsAll(originPermissionIds) && originPermissionIds.containsAll(newPermissionIds))) {
                // 如果权限有变更，则记录权限
                modifiedRoleLogDTO.setPermissions(newPermissionIds);
                originRoleLogDTO.setPermissions(originPermissionIds);
            }
        }

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(roleGetResponse.getName())
                        .originalValue(originRoleLogDTO)
                        .modifiedValue(modifiedRoleLogDTO)
                        .build()
        );

        return roleGetResponse;
    }

    private void clearPermissionCacheByRoleId(String roleId, String orgId) {
        List<String> userIds = extUserRoleMapper.getUserIdsByRoleIds(List.of(roleId));
        userIds.forEach(userId -> {
            // 清除用户的权限缓存
            permissionCache.clearCache(userId, orgId);
        });
    }

    private void deleteRoleScopeDeptByRoleId(String roleId) {
        RoleScopeDept example = new RoleScopeDept();
        example.setRoleId(roleId);
        roleScopeDeptMapper.delete(example);
    }

    /**
     * 校验是否是内置角色
     */
    public void checkInternalRole(String roleId) {
        Role role = roleMapper.selectByPrimaryKey(roleId);
        if (BooleanUtils.isTrue(role.getInternal())) {
            throw new GenericException(INTERNAL_ROLE_PERMISSION);
        }
    }

    private void checkAddExist(Role role) {
        if (extRoleMapper.checkAddExist(role)) {
            throw new GenericException(ROLE_EXIST);
        }
    }

    private void checkUpdateExist(Role role) {
        if (extRoleMapper.checkUpdateExist(role)) {
            throw new GenericException(ROLE_EXIST);
        }
    }

    @OperationLog(module = LogModule.SYSTEM_ROLE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String orgId) {
        Role role = roleMapper.selectByPrimaryKey(id);
        if (role == null) {
            throw new GenericException(Translator.get("role.not_exist"));
        }
        // 内置角色不能删除
        checkInternalRole(role.getId());
        // 删除角色
        roleMapper.deleteByPrimaryKey(id);
        // 删除与权限的关联关系
        deletePermissionByRoleId(id);
        // 删除与部门的关联关系
        deleteRoleScopeDeptByRoleId(id);

        clearPermissionCacheByRoleId(role.getId(), orgId);
        // 删除与用户的关联关系
        deleteUserRoleByRoleId(id);

        // 设置操作对象
        OperationLogContext.setResourceName(role.getName());
    }

    private void deletePermissionByRoleId(String id) {
        RolePermission example = new RolePermission();
        example.setRoleId(id);
        rolePermissionMapper.delete(example);
    }

    private void deleteUserRoleByRoleId(String id) {
        UserRole example = new UserRole();
        example.setRoleId(id);
        userRoleMapper.delete(example);
    }

    public List<PermissionDefinitionItem> getPermissionSetting(String id) {
        // 获取角色
        Role role = roleMapper.selectByPrimaryKey(id);
        // 获取该角色拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(role.getId());
        return getPermissionDefinitionItems(role, permissionIds);
    }

    /**
     * 获取空的权限配置
     *
     * @return
     */
    public List<PermissionDefinitionItem> getPermissionSetting() {
        return getPermissionDefinitionItems(null, Set.of());
    }

    private List<PermissionDefinitionItem> getPermissionDefinitionItems(Role role, Set<String> permissionIds) {
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
                    if (permissionIds.contains(p.getId()) ||
                            (role != null && StringUtils.equals(role.getId(), InternalRole.ORG_ADMIN.getValue()))) {
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

    /**
     * 翻译默认的权限名称
     *
     * @param p
     * @return
     */
    public String translateDefaultPermissionName(Permission p) {
        if (StringUtils.isNotBlank(p.getName())) {
            p.getName();
        }
        String[] idSplit = p.getId().split(":");
        String permissionKey = idSplit[idSplit.length - 1];
        String permissionName = "permission." + permissionKey.toLowerCase();
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

    public Set<String> getPermissionIds(List<String> roleIds) {
        return getPermissions(roleIds)
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }

    public List<RolePermission> getPermissions(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        var userLambdaQueryWrapper = new LambdaQueryWrapper<RolePermission>()
                .in(RolePermission::getRoleId, roleIds);
        return rolePermissionMapper.selectListByLambda(userLambdaQueryWrapper);
    }


    public List<String> getRoleIdsByUserId(String userId) {
        List<UserRole> userRoles = getUserRolesByUserId(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            return List.of();
        }
        return userRoles.stream()
                .map(UserRole::getRoleId)
                .toList();
    }

    public List<UserRole> getUserRolesByUserId(String userId) {
        UserRole example = new UserRole();
        example.setUserId(userId);
        return userRoleMapper.select(example);
    }

    /**
     * 更新单个用户组的配置项
     *
     * @param permissions
     */
    public void updatePermissionSetting(List<PermissionUpdateRequest> permissions, String roleId, String userId) {
        if (permissions == null) {
            return;
        }

        // 先删除
        deletePermissionByRoleId(roleId);

        // 再新增
        insertRolePermission(permissions, roleId, userId);
    }

    private void insertRolePermission(List<PermissionUpdateRequest> permissions, String roleId, String userId) {
        if (CollectionUtils.isEmpty(permissions)) {
            return;
        }
        permissions.forEach(permission -> {
            if (BooleanUtils.isTrue(permission.getEnable())) {
                String permissionId = permission.getId();
                RolePermission rolePermission = new RolePermission();
                rolePermission.setId(IDGenerator.nextStr());
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        });
    }

    public List<Role> getByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return roleMapper.selectByIds(ids.toArray(new String[0]));
    }

    public List<RoleDataScopeDTO> getRoleOptions(String userId, String orgId) {
        List<String> roleIds = getRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<Role> roles = getByIds(roleIds);
        return roles.stream()
                .filter(role -> StringUtils.equals(role.getOrganizationId(), orgId))
                .map(role -> {
                    role = translateInternalRole(role);
                    return BeanUtils.copyBean(new RoleDataScopeDTO(), role);
                }).toList();
    }
}