package io.cordys.crm.opportunity.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.condition.BasePageRequest;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.OpportunityRuleDTO;
import io.cordys.crm.opportunity.dto.request.OpportunityRuleSaveRequest;
import io.cordys.crm.opportunity.mapper.ExtOpportunityRuleMapper;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.User;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityRuleService {

	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private BaseMapper<OpportunityRule> opportunityRuleMapper;
	@Resource
	private ExtOpportunityRuleMapper extOpportunityRuleMapper;

	/**
	 * 分页获取商机规则
	 * @param request 分页参数
	 * @return 商机规则列表
	 */
	public List<OpportunityRuleDTO> page(BasePageRequest request, String organizationId) {
		List<OpportunityRuleDTO> rules = extOpportunityRuleMapper.list(request, organizationId);
		if (CollectionUtils.isEmpty(rules)) {
			return new ArrayList<>();
		}
		List<String> scopeIds = new ArrayList<>();
		List<String> ownerIds = new ArrayList<>();
		rules.forEach(rule -> {
			scopeIds.addAll(List.of(rule.getScopeId().split(",")));
			ownerIds.addAll(List.of(rule.getOwnerId().split(",")));
		});
		List<String> unionIds = ListUtils.union(scopeIds, ownerIds);
		List<User> users = userMapper.selectByIds(unionIds.toArray(new String[0]));
		List<Department> departments = departmentMapper.selectByIds(unionIds.toArray(new String[0]));
		rules.forEach(rule -> {
			rule.setScopeNames(transferIdToName(users, departments, List.of(rule.getScopeId().split(","))));
			rule.setOwnerNames(transferIdToName(users, departments, List.of(rule.getOwnerId().split(","))));
		});
		return rules;
	}

	public void save(OpportunityRuleSaveRequest request, String currentUserId, String organizationId) {
		OpportunityRule rule = new OpportunityRule();
		BeanUtils.copyBean(rule, request);
		rule.setOrganizationId(organizationId);
		rule.setUpdateTime(System.currentTimeMillis());
		rule.setUpdateUser(currentUserId);
		if (request.getId() == null) {
			rule.setId(IDGenerator.nextStr());
			rule.setCreateTime(System.currentTimeMillis());
			rule.setCreateUser(currentUserId);
			opportunityRuleMapper.insert(rule);
		} else {
			OpportunityRule oldRule = checkRuleExit(request.getId());
			checkRuleOwner(oldRule, currentUserId);
			opportunityRuleMapper.updateById(rule);
		}
	}

	public void delete(String id, String currentUserId) {
		OpportunityRule rule = checkRuleExit(id);
		checkRuleOwner(rule, currentUserId);
		opportunityRuleMapper.deleteByPrimaryKey(id);
	}

	public void switchStatus(String id, String currentUserId) {
		OpportunityRule rule = checkRuleExit(id);
		checkRuleOwner(rule, currentUserId);
		rule.setEnable(!rule.getEnable());
		rule.setUpdateUser(currentUserId);
		rule.setUpdateTime(System.currentTimeMillis());
		opportunityRuleMapper.updateById(rule);
	}

	private OpportunityRule checkRuleExit(String id) {
		OpportunityRule rule = opportunityRuleMapper.selectByPrimaryKey(id);
		if (rule == null) {
			throw new RuntimeException(Translator.get("opportunity.rule.not_exist"));
		}
		return rule;
	}

	private void checkRuleOwner(OpportunityRule rule, String accessUserId) {
		// split multiple owner by comma
		List<String> ownerIds = List.of(rule.getOwnerId().split(","));
		if (!ownerIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("opportunity.access_fail"));
		}
	}

	/**
	 * ID => Name
	 * @param users 用户集合
	 * @param departments 部门集合
	 * @param ids ID集合
	 * @return 名称集合
	 */
	private List<String> transferIdToName(List<User> users, List<Department> departments, List<String> ids) {
		List<String> names = new ArrayList<>();
		Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getName));
		Map<String, String> departmentMap = departments.stream().collect(Collectors.toMap(Department::getId, Department::getName));
		ids.forEach(id -> {
			if (userMap.containsKey(id)) {
				names.add(userMap.get(id));
			} else if (departmentMap.containsKey(id)) {
				names.add(departmentMap.get(id));
			}
		});
		return names;
	}
}
