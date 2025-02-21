package io.cordys.crm.opportunity.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.OpportunityRuleDTO;
import io.cordys.crm.opportunity.dto.request.OpportunityRuleAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityRuleUpdateRequest;
import io.cordys.crm.opportunity.mapper.ExtOpportunityRuleMapper;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.service.UserExtendService;
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
	private ExtUserMapper extUserMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Role> roleMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private BaseMapper<OpportunityRule> opportunityRuleMapper;
	@Resource
	private ExtOpportunityRuleMapper extOpportunityRuleMapper;
	@Resource
	private UserExtendService userExtendService;

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
		List<String> userIds = new ArrayList<>();
		List<String> scopeIds = new ArrayList<>();
		List<String> ownerIds = new ArrayList<>();
		rules.forEach(rule -> {
			userIds.add(rule.getCreateUser());
			userIds.add(rule.getUpdateUser());
			scopeIds.addAll(JSON.parseArray(rule.getScopeId(), String.class));
			ownerIds.addAll(JSON.parseArray(rule.getOwnerId(), String.class));
		});
		List<String> unionIds = ListUtils.union(scopeIds, ownerIds).stream().distinct().toList();
		List<User> users = userMapper.selectByIds(unionIds.toArray(new String[0]));
		List<Role> roles = roleMapper.selectByIds(unionIds.toArray(new String[0]));
		List<Department> departments = departmentMapper.selectByIds(unionIds.toArray(new String[0]));
		List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
		Map<String, String> userMap = createOrUpdateUsers.stream().collect(Collectors.toMap(User::getId, User::getName));
		rules.forEach(rule -> {
			rule.setMembers(userExtendService.getScope(users, roles, departments, JSON.parseArray(rule.getScopeId(), String.class)));
			rule.setOwners(userExtendService.getScope(users, roles, departments, JSON.parseArray(rule.getOwnerId(), String.class)));
			rule.setCreateUserName(userMap.get(rule.getCreateUser()));
			rule.setUpdateUserName(userMap.get(rule.getUpdateUser()));
		});
		return rules;
	}

	/**
	 * 新增商机规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 * @param organizationId 当前组织ID
	 */
	public void add(OpportunityRuleAddRequest request, String currentUserId, String organizationId) {
		OpportunityRule rule = new OpportunityRule();
		BeanUtils.copyBean(rule, request);
		rule.setId(IDGenerator.nextStr());
		rule.setOrganizationId(organizationId);
		rule.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
		rule.setScopeId(JSON.toJSONString(request.getScopeIds()));
		rule.setCondition(JSON.toJSONString(request.getConditions()));
		rule.setCreateTime(System.currentTimeMillis());
		rule.setCreateUser(currentUserId);
		rule.setUpdateTime(System.currentTimeMillis());
		rule.setUpdateUser(currentUserId);
		opportunityRuleMapper.insert(rule);
	}

	/**
	 * 修改商机规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 * @param organizationId 当前组织ID
	 */
	public void update(OpportunityRuleUpdateRequest request, String currentUserId, String organizationId) {
		OpportunityRule oldRule = checkRuleExit(request.getId());
		checkRuleOwner(oldRule, currentUserId);
		OpportunityRule rule = new OpportunityRule();
		BeanUtils.copyBean(rule, request);
		rule.setOrganizationId(organizationId);
		rule.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
		rule.setScopeId(JSON.toJSONString(request.getScopeIds()));
		rule.setCondition(JSON.toJSONString(request.getConditions()));
		rule.setUpdateTime(System.currentTimeMillis());
		rule.setUpdateUser(currentUserId);
		opportunityRuleMapper.updateById(rule);
	}

	/**
	 * 删除商机规则
	 * @param id 规则ID
	 * @param currentUserId 当前用户ID
	 */
	public void delete(String id, String currentUserId) {
		OpportunityRule rule = checkRuleExit(id);
		checkRuleOwner(rule, currentUserId);
		opportunityRuleMapper.deleteByPrimaryKey(id);
	}

	/**
	 * 启用/禁用商机规则
	 * @param id 规则ID
	 * @param currentUserId 当前用户ID
	 */
	public void switchStatus(String id, String currentUserId) {
		OpportunityRule rule = checkRuleExit(id);
		checkRuleOwner(rule, currentUserId);
		rule.setEnable(!rule.getEnable());
		rule.setUpdateUser(currentUserId);
		rule.setUpdateTime(System.currentTimeMillis());
		opportunityRuleMapper.updateById(rule);
	}

	/**
	 * 校验商机规则是否存在
	 * @param id 规则ID
	 * @return 商机规则
	 */
	private OpportunityRule checkRuleExit(String id) {
		OpportunityRule rule = opportunityRuleMapper.selectByPrimaryKey(id);
		if (rule == null) {
			throw new RuntimeException(Translator.get("opportunity.rule.not_exist"));
		}
		return rule;
	}

	/**
	 * 校验是否商机规则管理员
	 * @param rule 商机规则
	 * @param accessUserId 当前用户ID
	 */
	private void checkRuleOwner(OpportunityRule rule, String accessUserId) {
		List<String> ownerIds = JSON.parseArray(rule.getOwnerId(), String.class);
		List<String> ownerUserIds = extUserMapper.getUserIdsByScope(ownerIds, rule.getOrganizationId());
		if (!ownerUserIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("opportunity.access_fail"));
		}
	}
}
