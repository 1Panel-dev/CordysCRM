package cn.cordys.common.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DataScopeService {
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private BaseService baseService;
    @Resource
    private PermissionCache permissionCache;

    public DeptDataPermissionDTO getDeptDataPermission(String userId, String orgId, String viewId, String permission) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
        deptDataPermission.setViewId(viewId);
        if (InternalUserView.isSelf(viewId)) {
            // 只查看自己的数据
            deptDataPermission.setSelf(true);
            return deptDataPermission;
        } else if (InternalUserView.isVisible(viewId)) {
            // 查看设置为可见的数据
            deptDataPermission.setVisible(true);
            return deptDataPermission;
        } else {
            deptDataPermission = getDeptDataPermission(userId, orgId, permission);
            deptDataPermission.setViewId(viewId);
            if (deptDataPermission.getAll() && InternalUserView.isDepartment(viewId)) {
                // 数据权限是全部,但是查询条件是部门,则按照部门查询
                return getDeptDataPermissionForAllPermission(userId, orgId);
            }
        }

        return deptDataPermission;
    }

    /**
     * 获取用户角色的数据权限
     * @param userId
     * @param orgId
     * @return
     */
    public DeptDataPermissionDTO getDeptDataPermission(String userId, String orgId, String permission) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();

        if (Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
            // 超级管理员查看所有数据
            deptDataPermission.setAll(true);
            return deptDataPermission;
        }

        Map<String, List<RolePermissionDTO>> dataScopeRoleMap = getDataScopeRoleMap(userId, orgId);

        boolean hasAllPermission = hasDataScopePermission(dataScopeRoleMap, RoleDataScope.ALL.name(), permission);

        if (hasAllPermission) {
            // 可以查看所有数据
            deptDataPermission.setAll(true);
            return deptDataPermission;
        }

        List<RolePermissionDTO> userDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_AND_CHILD.name());
        List<RolePermissionDTO> customDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name());

        if (CollectionUtils.isEmpty(userDeptRoles)
                && CollectionUtils.isEmpty(customDeptRoles)) {
            // 如果没有部门权限,则默认只能查看自己的数据
            deptDataPermission.setSelf(true);
            return deptDataPermission;
        }

        return getDeptDataPermissionForDept(userId, orgId, dataScopeRoleMap, permission);
    }

    private DeptDataPermissionDTO getDeptDataPermissionForAllPermission(String userId, String orgId) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
        // 查询部门树
        List<BaseTreeNode> tree = departmentService.getTree(orgId);
        OrganizationUser organizationUser = getOrganizationUser(userId, orgId);
        if (organizationUser == null) {
            return deptDataPermission;
        }
        List<String> deptIds = getDeptIdsWithChild(tree, Set.of(organizationUser.getDepartmentId()));
        deptDataPermission.getDeptIds().addAll(deptIds);
        return deptDataPermission;
    }

    private DeptDataPermissionDTO getDeptDataPermissionForDept(String userId, String orgId,
                                                               Map<String, List<RolePermissionDTO>> dataScopeRoleMap,
                                                               String permission) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
        // 查询部门树
        List<BaseTreeNode> tree = departmentService.getTree(orgId);

        boolean hasDeptAndChildPermission = hasDataScopePermission(dataScopeRoleMap, RoleDataScope.DEPT_AND_CHILD.name(), permission);

        if (hasDeptAndChildPermission) {
            // 查看用户部门及其子部门数据
            OrganizationUser organizationUser = getOrganizationUser(userId, orgId);
            List<String> deptIds = getDeptIdsWithChild(tree, Set.of(organizationUser.getDepartmentId()));
            deptDataPermission.getDeptIds().addAll(deptIds);
        }

        List<RolePermissionDTO> customDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name());
        boolean hasDeptCustomPermission = hasDataScopePermission(dataScopeRoleMap, RoleDataScope.DEPT_CUSTOM.name(), permission);

        if (hasDeptCustomPermission) {
            // 查看指定部门及其子部门数据
            List<String> customDeptRolesIds = customDeptRoles.stream()
                    .map(RoleDataScopeDTO::getId)
                    .toList();
            List<String> parentDeptIds = roleService.getDeptIdsByRoleIds(customDeptRolesIds);
            List<String> deptIds = getDeptIdsWithChild(tree, new HashSet<>(parentDeptIds));
            deptDataPermission.getDeptIds().addAll(deptIds);
        }
        return deptDataPermission;
    }

    public boolean hasDataScopePermission(Map<String, List<RolePermissionDTO>> dataScopeRoleMap, String dataScope, String permission) {
        List<RolePermissionDTO> roleDataScopes = dataScopeRoleMap.get(dataScope);
        if (roleDataScopes == null) {
            return false;
        }
        for (RolePermissionDTO roleDataScope : roleDataScopes) {
            if (roleDataScope.getPermissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param userId
     * @param orgId
     * @return
     */
    private DeptDataPermissionDTO getDeptDataPermissionForDeptSearchType(String userId, String orgId, String permission) {
        return getDeptDataPermissionForDept(userId, orgId, getDataScopeRoleMap(userId, orgId), permission);
    }

    public Map<String, List<RolePermissionDTO>> getDataScopeRoleMap(String userId, String orgId) {
        return permissionCache.getRolePermissions(userId, orgId)
                .stream()
                .collect(Collectors.groupingBy(RolePermissionDTO::getDataScope, Collectors.toList()));
    }

    public OrganizationUser getOrganizationUser(String userId, String orgId) {
        OrganizationUser example = new OrganizationUser();
        example.setUserId(userId);
        example.setOrganizationId(orgId);
        return organizationUserMapper.selectOne(example);
    }

    /**
     * 根据已选的部门ID
     * 获取部门及其子部门的ID
     *
     * @param tree
     * @param deptIds
     * @return
     */
    public List<String> getDeptIdsWithChild(List<BaseTreeNode> tree, Set<String> deptIds) {
        List<String> childDeptIds = new ArrayList<>();
        for (BaseTreeNode node : tree) {
            if (deptIds.contains(node.getId())) {
                childDeptIds.add(node.getId());
                childDeptIds.addAll(getNodeIdsWithChild(node.getChildren()));
            } else {
                childDeptIds.addAll(getDeptIdsWithChild(node.getChildren(), deptIds));
            }
        }
        return childDeptIds;
    }

    /**
     * 获取树节点及其子节点的ID
     * @param tree
     * @return
     */
    private List<String> getNodeIdsWithChild(List<BaseTreeNode> tree) {
        if (CollectionUtils.isEmpty(tree)) {
            return List.of();
        }
        List<String> childDeptIds = new ArrayList<>();
        for (BaseTreeNode node : tree) {
            childDeptIds.add(node.getId());
            childDeptIds.addAll(getNodeIdsWithChild(node.getChildren()));
        }
        return childDeptIds;
    }

    /**
     * 校验数据权限
     * @param userId
     * @param orgId
     */
    public void checkDataPermission(String userId, String orgId, String owner, String permission) {
        checkDataPermission(userId, orgId, List.of(owner), permission);
    }

    public void checkDataPermission(String userId, String orgId, List<String> owners, String permission) {
        boolean hasPermission = hasDataPermission(userId, orgId, owners, permission);
        if (!hasPermission) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    public boolean hasDataPermission(String userId, String orgId, String owner, String permission) {
        return hasDataPermission(userId, orgId, List.of(owner), permission);
    }

    public boolean hasDataPermission(String userId, String orgId, List<String> owners, String permission) {
        DeptDataPermissionDTO deptDataPermission = getDeptDataPermission(userId, orgId, permission);
        if (deptDataPermission.getAll() || Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
            return true;
        }

        if (deptDataPermission.getSelf()) {
            for (String owner : owners) {
                // 是否是自己的客户
                if (!Strings.CS.equals(owner, userId)) {
                    return false;
                }
            }
            return true;
        }

        if (CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())) {
            Map<String, UserDeptDTO> userDeptMapByUserIds = baseService.getUserDeptMapByUserIds(owners, orgId);
            for (String owner : owners) {
                UserDeptDTO customerOwnerDept = userDeptMapByUserIds.get(owner);
                // 部门权限是否有该客户的权限
                if (customerOwnerDept == null || !deptDataPermission.getDeptIds().contains(customerOwnerDept.getDeptId())) {
                    if (!Strings.CS.equals(owner, userId)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}