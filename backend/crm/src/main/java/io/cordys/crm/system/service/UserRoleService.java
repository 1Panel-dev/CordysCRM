package io.cordys.crm.system.service;

import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.UserRole;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.request.RoleUserPageRequest;
import io.cordys.crm.system.dto.response.RoleUserListResponse;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserRoleService {
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private BaseMapper<Department> departmentMapper;

    public void delete(String id) {
        userRoleMapper.deleteByPrimaryKey(id);
    }

    public List<RoleUserListResponse> listUserByRoleId(RoleUserPageRequest request, String orgId) {
        List<RoleUserListResponse> users = extUserRoleMapper.list(request, orgId);
        Map<String, List<UserRoleConvert>> userRoleMap = getUserRoleMap(orgId, users);

        Map<String, String> deptNameMap = getDeptNameMap(users);
        for (RoleUserListResponse user : users) {
            user.setRoles(userRoleMap.get(user.getUserId()));
            user.setDepartmentName(deptNameMap.get(user.getDepartmentId()));
        }
        return users;
    }

    private Map<String, String> getDeptNameMap(List<RoleUserListResponse> users) {
        Set<String> deptIds = users.stream().map(RoleUserListResponse::getUserId).collect(Collectors.toSet());
        Map<String, String> deptNameMap = departmentMapper.selectByIds(deptIds.toArray(new String[0]))
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        return deptNameMap;
    }

    private Map<String, List<UserRoleConvert>> getUserRoleMap(String orgId, List<RoleUserListResponse> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }
        List<String> userIds = users.stream()
                .map(RoleUserListResponse::getUserId)
                .collect(Collectors.toList());

        List<UserRoleConvert> userRoles = extUserMapper.getUserRole(userIds, orgId);
        userRoles.forEach(role -> role.setName(roleService.translateInternalRole(role.getName())));
        Map<String, List<UserRoleConvert>> userRoleMap = userRoles.stream().collect(Collectors.groupingBy(UserRoleConvert::getUserId));
        return userRoleMap;
    }

    /**
     * 获取带用户的信息的部门树
     *
     * @return List<DeptUserTreeNode>
     */
    public List<DeptUserTreeNode> getDeptUserTree(String orgId, String roleId) {
        List<DeptUserTreeNode> treeNodes = extDepartmentMapper.selectDeptUserTreeNode(orgId);
        List<DeptUserTreeNode> userNodes = extUserRoleMapper.selectUserForRelevance(orgId, roleId);
        treeNodes.addAll(userNodes);
        return BaseTreeNode.buildTree(treeNodes);
    }
}