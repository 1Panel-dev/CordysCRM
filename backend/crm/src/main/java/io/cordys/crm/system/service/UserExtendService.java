package io.cordys.crm.system.service;

import io.cordys.common.constants.InternalRole;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.ScopeKey;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.cordys.crm.system.dto.response.RoleUserOptionResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserExtendService {

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
}
