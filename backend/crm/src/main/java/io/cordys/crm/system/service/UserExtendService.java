package io.cordys.crm.system.service;

import io.cordys.common.constants.InternalRole;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.ScopeKey;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserExtendService {

	@Resource
	private BaseMapper<OrganizationUser> organizationUserMapper;
	@Resource
	private DepartmentService departmentService;
	/**
	 * 获取成员范围集合
	 * @param users 用户
	 * @param roles 角色
	 * @param departments 部门
	 * @param scopeIds 范围ID集合
	 * @return 范围集合
	 */
	public List<ScopeNameDTO> getScope(List<User> users, List<Role> roles, List<Department> departments, List<String> scopeIds) {
		Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getName));
		Map<String, String> roleMap = roles.stream().collect(Collectors.toMap(Role::getId, Role::getName));
		Map<String, String> departmentMap = departments.stream().collect(Collectors.toMap(Department::getId, Department::getName));
		List<ScopeNameDTO> scopes = new ArrayList<>();
		scopeIds.forEach(scopeId -> {
			ScopeNameDTO scope = ScopeNameDTO.builder().id(scopeId).build();
			if (userMap.containsKey(scopeId)) {
				scope.setName(userMap.get(scopeId));
				scope.setScope(ScopeKey.USER.name());
			} else if (roleMap.containsKey(scopeId)) {
				if (StringUtils.equalsAny(scopeId,
						InternalRole.ORG_ADMIN.getValue(),
						InternalRole.SALES_MANAGER.getValue(),
						InternalRole.SALES_STAFF.getValue())) {
					scope.setName(Translator.get("role." + scopeId));
				} else {
					scope.setName(roleMap.get(scopeId));
				}
				scope.setScope(ScopeKey.ROLE.name());
			} else if (departmentMap.containsKey(scopeId)) {
				scope.setName(departmentMap.get(scopeId));
				scope.setScope(ScopeKey.DEPARTMENT.name());
			}
			scopes.add(scope);
		});
		return scopes;
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
}
