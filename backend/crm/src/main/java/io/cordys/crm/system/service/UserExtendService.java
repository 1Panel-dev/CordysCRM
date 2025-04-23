package io.cordys.crm.system.service;

import io.cordys.common.dto.BaseTreeNode;
import io.cordys.crm.system.constants.ScopeKey;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.domain.UserRole;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.cordys.crm.system.mapper.ExtUserExtendMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserExtendService {

	@Resource
	private BaseMapper<OrganizationUser> organizationUserMapper;
	@Resource
	private ExtUserExtendMapper extUserExtendMapper;
	@Resource
	private DepartmentService departmentService;
	@Resource
	private BaseMapper<UserRole> userRoleMapper;
	@Resource
	private BaseMapper<User> userMapper;

	/**
	 * 获取范围的所有负责人ID
	 * @param scopeIds 范围ID集合
	 * @param orgId 组织ID
	 * @return 负责人ID集合
	 */
	public List<String> getScopeOwnerIds(List<String> scopeIds, String  orgId) {
		List<ScopeNameDTO> scopes = getScope(scopeIds);
		List<String> ownerIds = new ArrayList<>(scopes.stream().filter(scope -> StringUtils.equals(scope.getScope(), ScopeKey.USER.name())).map(ScopeNameDTO::getId).toList());
		List<ScopeNameDTO> roleList = scopes.stream().filter(scope -> StringUtils.equals(scope.getScope(), ScopeKey.ROLE.name())).toList();
		List<ScopeNameDTO> dptList = scopes.stream().filter(scope -> StringUtils.equals(scope.getScope(), ScopeKey.DEPARTMENT.name())).toList();
		if (!CollectionUtils.isEmpty(roleList)) {
			List<String> roleIds = roleList.stream().map(ScopeNameDTO::getId).toList();
			LambdaQueryWrapper<UserRole> userRoleWrapper = new LambdaQueryWrapper<>();
			userRoleWrapper.in(UserRole::getRoleId, roleIds);
			List<UserRole> userRoles = userRoleMapper.selectListByLambda(userRoleWrapper);
			ownerIds.addAll(userRoles.stream().map(UserRole::getUserId).toList());
		}
		if (!CollectionUtils.isEmpty(dptList)) {
			List<BaseTreeNode> tree = departmentService.getTree(orgId);
			List<String> allDptIds = new ArrayList<>(dptList.stream().map(ScopeNameDTO::getId).toList());
			dptList.forEach(dpt -> {
				List<String> childDptIds = getChildDptById(tree.getFirst(), dpt.getId());
				allDptIds.addAll(childDptIds);
			});
			LambdaQueryWrapper<OrganizationUser> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.in(OrganizationUser::getDepartmentId, allDptIds.stream().distinct().toList());
			List<OrganizationUser> organizationUsers = organizationUserMapper.selectListByLambda(queryWrapper);
			ownerIds.addAll(organizationUsers.stream().map(OrganizationUser::getUserId).toList());
		}
		return ownerIds.stream().distinct().toList();
	}

	/**
	 * 获取成员范围集合
	 * @param scopeIds 范围ID集合
	 * @return 范围集合
	 */
	public List<ScopeNameDTO> getScope(List<String> scopeIds) {
		if (CollectionUtils.isEmpty(scopeIds)) {
			return new ArrayList<>();
		}
		return extUserExtendMapper.groupByScopeIds(scopeIds);
	}

	/**
	 * 获取负责人范围ID集合
	 * @param ownerId 负责人ID
	 * @param organizationId 组织ID
	 * @return 范围ID集合
	 */
	public List<String> getUserScopeIds(String ownerId, String organizationId) {
		List<String> departmentIds = getParentDepartmentIds(ownerId, organizationId);
		departmentIds.add(ownerId);
		LambdaQueryWrapper<UserRole> userRoleWrapper = new LambdaQueryWrapper<>();
		userRoleWrapper.eq(UserRole::getUserId, ownerId);
		List<UserRole> roles = userRoleMapper.selectListByLambda(userRoleWrapper);
		List<String> roleIds = roles.stream().map(UserRole::getRoleId).toList();
		return ListUtils.union(departmentIds, roleIds);
	}

	/**
	 * 获取用户所有的上级部门
	 * @param userId 用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 上级部门ID集合
	 */
	public List<String> getParentDepartmentIds(String userId, String currentOrgId) {
		LambdaQueryWrapper<OrganizationUser> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrganizationUser::getUserId, userId).eq(OrganizationUser::getOrganizationId, currentOrgId);
		List<OrganizationUser> organizationUsers = organizationUserMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(organizationUsers)) {
			return new ArrayList<>();
		}
		String departmentId = organizationUsers.getFirst().getDepartmentId();
		List<BaseTreeNode> departmentTree = departmentService.getTree(currentOrgId);
		List<String> deptPath = new ArrayList<>();
		findDeptPathWithDfs(departmentTree.getFirst(), departmentId, deptPath);
		return deptPath;
	}

	/**
	 * 递归获取叶子部门的路径
	 */
	private void findDeptPathWithDfs(BaseTreeNode node, String targetNode, List<String> path) {
		// 加入当前节点
		path.add(node.getId());
		// 命中目标节点返回
		if (StringUtils.equals(node.getId(), targetNode)) {
			return;
		}
		// 递归子节点
		for (BaseTreeNode treeNode : node.getChildren()) {
			findDeptPathWithDfs(treeNode, targetNode, path);
		}
		// 未命中, 回退当前节点
		path.removeLast();
	}

	/**
	 * 查找目标部门的子部门节点
	 * @param node 树节点
	 * @param targetId 目标部门ID
	 * @return 子部门节点
	 */
	public List<String> getChildDptById(BaseTreeNode node, String targetId) {
		BaseTreeNode targetNode = findTargetById(node, targetId);
		if (targetNode != null) {
			// find, return all children
			return getAllChildDptIds(targetNode);
		} else {
			// not found, return empty list
			return new ArrayList<>();
		}
	}

	/**
	 * 根据ID递归查找目标部门节点
	 * @param node 树节点
	 * @param targetId 目标部门ID
	 * @return 树节点
	 */
	public BaseTreeNode findTargetById(BaseTreeNode node, String targetId) {
		if (StringUtils.equals(node.getId(), targetId)) {
			return node;
		}
		for (BaseTreeNode child : node.getChildren()) {
			BaseTreeNode result = findTargetById(child, targetId);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/**
	 * 递归获取所有子节点部门ID
	 * @param currentNode 当前节点
	 * @return 子节点部门ID集合
	 */
	public List<String> getAllChildDptIds(BaseTreeNode currentNode) {
		List<String> children = new ArrayList<>();
		if (CollectionUtils.isEmpty(currentNode.getChildren())) {
			return children;
		}
		for (BaseTreeNode child : currentNode.getChildren()) {
			children.add(child.getId());
			children.addAll(getAllChildDptIds(child));
		}
		return children;
	}

	/**
	 * 用户ID集合获取用户选项
	 * @param ids 用户ID集合
	 * @return 用户选项
	 */
	public List<User> getUserOptionByIds(List<String> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return new ArrayList<>();
		}
		return userMapper.selectByIds(ids.toArray(new String[0]));
	}

	public List<User> getUserOptionById(String id) {
		if (StringUtils.isBlank(id)) {
			return List.of();
		}
		return getUserOptionByIds(List.of(id));
	}
}
