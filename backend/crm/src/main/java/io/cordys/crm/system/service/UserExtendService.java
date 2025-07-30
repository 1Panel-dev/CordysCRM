package io.cordys.crm.system.service;

import io.cordys.common.constants.InternalRole;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.util.Translator;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

	public static final String ROOT_NODE_PARENT_ID = "NONE";

	/**
	 * 获取范围的所有负责人ID
	 * @param scopeIds 范围ID集合
	 * @param orgId 组织ID
	 * @return 负责人ID集合
	 */
	public List<String> getScopeOwnerIds(List<String> scopeIds, String  orgId) {
		List<ScopeNameDTO> scopes = getScope(scopeIds);
		List<String> ownerIds = scopes.stream().filter(scope -> StringUtils.equals(scope.getScope(), ScopeKey.USER.name())).map(ScopeNameDTO::getId).collect(Collectors.toList());
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
		return ownerIds.stream().distinct().collect(Collectors.toList());
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
		List<ScopeNameDTO> scopeNameDTOS = extUserExtendMapper.groupByScopeIds(scopeIds);
		return scopeNameDTOS.stream().peek(scope -> {
			if (StringUtils.equalsAny(scope.getName(), InternalRole.ORG_ADMIN.getValue(), InternalRole.SALES_MANAGER.getValue(), InternalRole.SALES_STAFF.getValue())) {
				scope.setName(Translator.get("role." + scope.getName()));
			}
		}).toList();
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
	 * 获取负责人范围对象集合
	 * @param ownerIds 负责人ID集合
	 * @param organizationId 组织ID
	 * @return 范围对象集合
	 */
	public Map<String, List<String>> getMultiScopeMap(List<String> ownerIds, String organizationId) {
		if (CollectionUtils.isEmpty(ownerIds)) {
			return Map.of();
		}
		Map<String, List<String>> scopeMap = new HashMap<>(ownerIds.size());
		// 查询所有负责人的角色
		LambdaQueryWrapper<UserRole> userRoleWrapper = new LambdaQueryWrapper<>();
		userRoleWrapper.in(UserRole::getUserId, ownerIds);
		List<UserRole> allOwnerRoles = userRoleMapper.selectListByLambda(userRoleWrapper);
		Map<String, List<String>> ownerRoleMap = allOwnerRoles.stream().collect(Collectors.groupingBy(
				UserRole::getUserId, Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
		// 查询所有负责人的部门
		Map<String, List<String>> ownerDepMap = getOwnerAncestorMap(ownerIds, organizationId);

		ownerIds.forEach(ownerId -> {
			List<String> roleIds = ownerRoleMap.getOrDefault(ownerId, List.of());
			List<String> deptIds = ownerDepMap.getOrDefault(ownerId, List.of());
			List<String> allDeptIds = new ArrayList<>(deptIds);
			// 负责人也作为范围对象
			allDeptIds.add(ownerId);
			scopeMap.put(ownerId, ListUtils.union(roleIds, allDeptIds));
		});
		return scopeMap;
	}

	/**
	 * 获取负责人集合的父部门映射
	 * @param ownerIds 负责人ID集合
	 * @param organizationId 组织ID
	 * @return 负责人ID -> 父部门ID集合映射
	 */
	public Map<String, List<String>> getOwnerAncestorMap(List<String> ownerIds, String organizationId) {
		Map<String, String> deptParentMap = loadDeptParentMap(organizationId);
		Map<String, List<String>> ancestorMap = new HashMap<>(ownerIds.size());
		// 查询所有负责人的部门ID
		LambdaQueryWrapper<OrganizationUser> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(OrganizationUser::getUserId, ownerIds).eq(OrganizationUser::getOrganizationId, organizationId);
		List<OrganizationUser> organizationUsers = organizationUserMapper.selectListByLambda(wrapper);
		if (organizationUsers.isEmpty()) {
			return ancestorMap;
		}
		Map<String, String> ownerDepIdMap = organizationUsers.stream().collect(Collectors.toMap(OrganizationUser::getUserId, OrganizationUser::getDepartmentId));
		ownerDepIdMap.forEach((k, v) -> ancestorMap.put(k, getDepAllAncestor(v, deptParentMap)));
		return ancestorMap;
	}

	/**
	 * 获取部门的父部门
	 * @param deptId 部门ID
	 * @return 父部门ID集合
	 */
	public List<String> getDepAllAncestor(String deptId, Map<String, String> deptParentMap) {
		List<String> ancestors = new ArrayList<>();
		ancestors.add(deptId);
		String current = deptParentMap.get(deptId);
		while (StringUtils.isNotEmpty(current) && !StringUtils.equals(current, ROOT_NODE_PARENT_ID)) {
			ancestors.add(current);
			current = deptParentMap.get(current);
		}
		return ancestors;
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
		boolean found = findDeptPathWithDfs(departmentTree.getFirst(), departmentId, deptPath);
		// not found, department has been removed
		return found ? deptPath : new ArrayList<>();
	}

	/**
	 * 递归获取叶子部门的路径
	 */
	private boolean findDeptPathWithDfs(BaseTreeNode node, String targetNode, List<String> path) {
		// 加入当前节点
		path.add(node.getId());
		// 命中目标节点返回
		if (StringUtils.equals(node.getId(), targetNode)) {
			return true;
		}
		// 递归子节点
		for (BaseTreeNode treeNode : node.getChildren()) {
			if (findDeptPathWithDfs(treeNode, targetNode, path)) {
				return true;
			}
        }
		// 未命中, 回退当前节点
		path.removeLast();
		return false;
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

	/**
	 * 是否含有重复对象元素
	 * @param scopeIds 原始范围ID集合
	 * @param targetScopeIds 目标范围ID集合
	 * @param currentOrgId 当前组织ID
	 * @return 是否含有重复对象元素
	 */
	public boolean hasDuplicateScopeObj(List<String> scopeIds, List<String> targetScopeIds, String currentOrgId) {
		List<String> ownerIds = getScopeOwnerIds(scopeIds, currentOrgId);
		List<String> targetOwnerIds = getScopeOwnerIds(targetScopeIds, currentOrgId);
		ownerIds.retainAll(targetOwnerIds);
		return !ownerIds.isEmpty();
	}

	/**
	 * 是否池子管理员
	 * @param scopeIds 范围ID集合
	 * @param ownerId 操作人ID
	 * @param orgId 组织ID
	 * @return 是否管理员
	 */
	public boolean isPoolAdmin(List<String> scopeIds, String ownerId, String orgId) {
		List<String> adminIds = getScopeOwnerIds(scopeIds, orgId);
		return adminIds.contains(ownerId);
	}

	/**
	 * 加载组织部门父节点映射
	 * @param orgId 组织ID
	 * @return 部门ID -> 父部门ID 映射
	 */
	private Map<String, String> loadDeptParentMap(String orgId) {
		List<BaseTreeNode> departmentTree = departmentService.getTree(orgId);
		Map<String, String> deptMap = new HashMap<>(2 << 7);
		collectDeptNodes(departmentTree, deptMap);
		return deptMap;
	}

	/**
	 * 递归收集部门节点的ID和父ID映射
	 * @param nodes 节点列表
	 * @param deptMap 部门ID -> 父部门ID 映射
	 */
	private void collectDeptNodes(List<BaseTreeNode> nodes, Map<String, String> deptMap) {
		if (CollectionUtils.isEmpty(nodes)) {
			return;
		}
		for (BaseTreeNode node : nodes) {
			deptMap.put(node.getId(), node.getParentId());
			// 递归处理子节点
			collectDeptNodes(node.getChildren(), deptMap);
		}
	}

}
