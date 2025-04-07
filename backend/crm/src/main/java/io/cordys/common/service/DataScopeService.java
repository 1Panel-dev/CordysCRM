package io.cordys.common.service;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.RoleDataScope;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.RoleDataScopeDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.response.result.CrmHttpResultCode;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.system.domain.OrganizationUser;

import io.cordys.crm.system.domain.UserRole;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.crm.system.service.RoleService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    public DeptDataPermissionDTO getDeptDataPermission(String userId, String orgId, String searchType) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();

        if (BusinessSearchType.isSelf(searchType)) {
            // 只查看自己的数据
            deptDataPermission.setSelf(true);
            return deptDataPermission;
        } else if (BusinessSearchType.isVisible(searchType)) {
            // 查看设置为可见的数据
            deptDataPermission.setVisible(true);
            return deptDataPermission;
        } else {
            deptDataPermission = getDeptDataPermission(userId, orgId);
            if (deptDataPermission.getAll() && BusinessSearchType.isDepartment(searchType)) {
                // 数据权限是全部,但是查询条件是部门,则按照部门查询
                return getDeptDataPermissionForDeptSearchType(userId, orgId);
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
    public DeptDataPermissionDTO getDeptDataPermission(String userId, String orgId) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();

        if (StringUtils.equals(userId, InternalUser.ADMIN.getValue())) {
            // 超级管理员查看所有数据
            deptDataPermission.setAll(true);
            return deptDataPermission;
        }

        // 从 sessionUser 中获取角色数据权限
        Map<String, List<RoleDataScopeDTO>> dataScopeRoleMap;
        SessionUser user = SessionUtils.getUser();
        if (user != null) {
            dataScopeRoleMap = user.getRoles()
                    .stream()
                    .collect(Collectors.groupingBy(RoleDataScopeDTO::getDataScope, Collectors.toList()));
        } else {
            dataScopeRoleMap = getDataScopeRoleMap(userId);
        }

        if (CollectionUtils.isNotEmpty(dataScopeRoleMap.get(RoleDataScope.ALL.name()))) {
            // 可以查看所有数据
            deptDataPermission.setAll(true);
            return deptDataPermission;
        }

        List<RoleDataScopeDTO> userDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_AND_CHILD.name());
        List<RoleDataScopeDTO> customDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name());

        if (CollectionUtils.isEmpty(userDeptRoles)
                && CollectionUtils.isEmpty(customDeptRoles)) {
            // 如果没有部门权限,则默认只能查看自己的数据
            deptDataPermission.setSelf(true);
            return deptDataPermission;
        }

        return getDeptDataPermissionForDept(userId, orgId, dataScopeRoleMap);
    }

    private DeptDataPermissionDTO getDeptDataPermissionForDept(String userId, String orgId, Map<String, List<RoleDataScopeDTO>> dataScopeRoleMap) {
        DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
        List<RoleDataScopeDTO> userDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_AND_CHILD.name());
        List<RoleDataScopeDTO> customDeptRoles = dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name());

        // 查询部门树
        List<BaseTreeNode> tree = departmentService.getTree(orgId);

        if (CollectionUtils.isNotEmpty(userDeptRoles)) {
            // 查看用户部门及其子部门数据
            OrganizationUser organizationUser = getOrganizationUser(userId, orgId);
            List<String> deptIds = getDeptIdsWithChild(tree, Set.of(organizationUser.getDepartmentId()));
            deptDataPermission.getDeptIds().addAll(deptIds);
        }

        if (CollectionUtils.isNotEmpty(customDeptRoles)) {
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

    /**
     * @param userId
     * @param orgId
     * @return
     */
    private DeptDataPermissionDTO getDeptDataPermissionForDeptSearchType(String userId, String orgId) {
        return getDeptDataPermissionForDept(userId, orgId, getDataScopeRoleMap(userId));
    }

    private Map<String, List<RoleDataScopeDTO>> getDataScopeRoleMap(String userId) {
        List<String> roleIds = roleService.getUserRolesByUserId(userId)
                .stream()
                .map(UserRole::getRoleId)
                .toList();

        return roleService.getByIds(roleIds)
                .stream()
                .map(role -> BeanUtils.copyBean(new RoleDataScopeDTO(), role))
                .collect(Collectors.groupingBy(RoleDataScopeDTO::getDataScope, Collectors.toList()));
    }

    private OrganizationUser getOrganizationUser(String userId, String orgId) {
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
    private List<String> getDeptIdsWithChild(List<BaseTreeNode> tree, Set<String> deptIds) {
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
    public void checkDataPermission(String userId, String orgId, String owner) {
        checkDataPermission(userId, orgId, List.of(owner));
    }

    public void checkDataPermission(String userId, String orgId, List<String> owners) {
        boolean hasPermission = hasDataPermission(userId, orgId, owners);
        if (!hasPermission) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    public boolean hasDataPermission(String userId, String orgId, String owner) {
        return hasDataPermission(userId, orgId, List.of(owner));
    }

    public boolean hasDataPermission(String userId, String orgId, List<String> owners) {
        DeptDataPermissionDTO deptDataPermission = getDeptDataPermission(userId, orgId);
        if (deptDataPermission.getAll() || StringUtils.equals(userId, InternalUser.ADMIN.getValue())) {
            return true;
        }

        if (deptDataPermission.getSelf()) {
            for (String owner : owners) {
                // 是否是自己的客户
                if (!StringUtils.equals(owner, userId)) {
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
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}