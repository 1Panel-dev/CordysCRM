package io.cordys.crm.opportunity.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.common.utils.RecycleConditionUtils;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.OpportunityRuleDTO;
import io.cordys.crm.opportunity.dto.request.OpportunityRuleAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityRuleUpdateRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityRuleMapper;
import io.cordys.crm.system.constants.RecycleConditionColumnKey;
import io.cordys.crm.system.constants.RecycleConditionOperator;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.RuleConditionDTO;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityRuleService {

	@Resource
	private BaseMapper<User> userMapper;
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
		List<String> userIds = rules.stream().flatMap(rule -> Stream.of(rule.getCreateUser(), rule.getUpdateUser())).toList();
		List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
		Map<String, String> userMap = createOrUpdateUsers.stream().collect(Collectors.toMap(User::getId, User::getName));
		rules.forEach(rule -> {
			delOldTime(rule);
			rule.setMembers(userExtendService.getScope(JSON.parseArray(rule.getScopeId(), String.class)));
			rule.setOwners(userExtendService.getScope(JSON.parseArray(rule.getOwnerId(), String.class)));
			rule.setCreateUserName(userMap.get(rule.getCreateUser()));
			rule.setUpdateUserName(userMap.get(rule.getUpdateUser()));
		});
		return rules;
	}

	private void delOldTime(OpportunityRuleDTO rule) {
		List<RuleConditionDTO> ruleConditionDTOS = JSON.parseArray(rule.getCondition(), RuleConditionDTO.class);
		if (CollectionUtils.isNotEmpty(ruleConditionDTOS)) {
			for (RuleConditionDTO condition : ruleConditionDTOS) {
				if (StringUtils.equals(condition.getColumn(), RecycleConditionColumnKey.CREATE_TIME)
						&& StringUtils.equals(condition.getOperator(), RecycleConditionOperator.DYNAMICS.name())) {
					String[] split = condition.getValue().split(",");
					if (StringUtils.isNotBlank(condition.getValue()) && split.length == 2 ) {
						String dateValue = split[0];
						String dateUnit = split[1];
						condition.setValue("CUSTOM,"+ dateValue + "," + dateUnit);
					}
				}
			}
		}
	}

	/**
	 * 新增商机规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 * @param organizationId 当前组织ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
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

		// 添加日志上下文
		OperationLogContext.setContext(LogContextInfo.builder()
				.modifiedValue(rule)
				.resourceId(rule.getId())
				.resourceName(Translator.get("module.opportunity.rule.setting") + ": " + rule.getName())
				.build());
	}

	/**
	 * 修改商机规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 * @param organizationId 当前组织ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#request.id}")
	public void update(OpportunityRuleUpdateRequest request, String currentUserId, String organizationId) {
		OpportunityRule oldRule = checkRuleExit(request.getId());
		OpportunityRule rule = new OpportunityRule();
		BeanUtils.copyBean(rule, request);
		rule.setOrganizationId(organizationId);
		rule.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
		rule.setScopeId(JSON.toJSONString(request.getScopeIds()));
		rule.setCondition(JSON.toJSONString(request.getConditions()));
		rule.setUpdateTime(System.currentTimeMillis());
		rule.setUpdateUser(currentUserId);
		opportunityRuleMapper.updateById(rule);

		OperationLogContext.setContext(
				LogContextInfo.builder()
						.resourceName(Translator.get("module.opportunity.rule.setting") + ": " + oldRule.getName())
						.originalValue(oldRule)
						.modifiedValue(opportunityRuleMapper.selectByPrimaryKey(request.getId()))
						.build()
		);
	}

	/**
	 * 删除商机规则
	 * @param id 规则ID
	 * @param currentUserId 当前用户ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
	public void delete(String id, String currentUserId) {
		OpportunityRule rule = checkRuleExit(id);
		opportunityRuleMapper.deleteByPrimaryKey(id);

		// 设置操作对象
		OperationLogContext.setResourceName(Translator.get("module.opportunity.rule.setting") + ": " + rule.getName());
	}

	/**
	 * 启用/禁用商机规则
	 * @param id 规则ID
	 * @param currentUserId 当前用户ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#id}")
	public void switchStatus(String id, String currentUserId) {
		OpportunityRule rule = checkRuleExit(id);
		rule.setEnable(!rule.getEnable());
		rule.setUpdateUser(currentUserId);
		rule.setUpdateTime(System.currentTimeMillis());
		opportunityRuleMapper.updateById(rule);

		OperationLogContext.setContext(
				LogContextInfo.builder()
						.resourceName(Translator.get("module.opportunity.rule.setting") + ": " + rule.getName())
						.originalValue(rule)
						.modifiedValue(opportunityRuleMapper.selectByPrimaryKey(id))
						.build()
		);
	}

	/**
	 * 获取负责人默认规则
	 * @param ownerIds 负责人ID集合
	 * @param organizationId 组织ID
	 * @return 默认规则
	 */
	public Map<String, OpportunityRule> getOwnersDefaultRuleMap(List<String> ownerIds, String organizationId) {
		Map<String, OpportunityRule> ruleMap = new HashMap<>(4);
		List<OpportunityRule> allRules = extOpportunityRuleMapper.getAllRule(organizationId);
		Map<String, List<String>> ownerScopeMap = userExtendService.getMultiScopeMap(ownerIds, organizationId);
		ownerIds.forEach(ownerId -> {
			List<OpportunityRule> rules = matchMultiScope(ownerScopeMap.get(ownerId), allRules);
			if (CollectionUtils.isEmpty(rules)) {
				return;
			}
			ruleMap.put(ownerId, rules.getFirst());
		});

		return ruleMap;
	}

	/**
	 * 匹配多个范围的商机关闭规则
	 * @param scopeIds 范围ID集合
	 * @param rules 规则列表
	 * @return 命中范围的规则列表
	 */
	public List<OpportunityRule> matchMultiScope(List<String> scopeIds, List<OpportunityRule> rules) {
		/*
		 * 命中规则任意范围即返回(默认按照创建时间作为优先级)
		 */
		if (CollectionUtils.isEmpty(scopeIds) || CollectionUtils.isEmpty(rules)) {
			return new ArrayList<>();
		}
		return rules.stream()
				.filter(rule -> {
					List<String> poolScopes = JSON.parseArray(rule.getScopeId(), String.class);
					return CollectionUtils.isNotEmpty(poolScopes) && CollectionUtils.containsAny(scopeIds, poolScopes);
				})
				.sorted(Comparator.comparing(OpportunityRule::getCreateTime).reversed())
				.collect(Collectors.toList());
	}

	/**
	 * 计算剩余归属天数
	 *
	 * @param rule 商机规则
	 * @param opportunity 商机
	 * @return 剩余归属天数
	 */
	public Integer calcReservedDay(OpportunityRule rule, OpportunityListResponse opportunity) {
		if (rule == null || !rule.getAuto()) {
			return null;
		}

		// 判断商机是否存在创建时间
		List<RuleConditionDTO> conditions = JSON.parseArray(rule.getCondition(), RuleConditionDTO.class);
		return RecycleConditionUtils.calcRecycleDays(conditions, opportunity.getCreateTime());
	}

	/**
	 * 获取负责人最佳匹配规则
	 * @param rules 规则列表
	 * @return 匹配集合
	 */
	public Map<List<String>, OpportunityRule> getOwnersBestMatchRuleMap(List<OpportunityRule> rules) {
		Map<List<String>, OpportunityRule> ruleMap = new HashMap<>(4);
		rules.sort(Comparator.comparing(OpportunityRule::getCreateTime).reversed());
		for (OpportunityRule rule : rules) {
			List<String> exitOwnerIds = ruleMap.keySet().stream().flatMap(List::stream).toList();
			List<String> scopeIds = JSON.parseArray(rule.getScopeId(), String.class);
			List<String> ownerIds = userExtendService.getScopeOwnerIds(scopeIds, rule.getOrganizationId());
			List<String> defaultOwnerIds = ownerIds.stream().distinct().filter(ownerId -> !exitOwnerIds.contains(ownerId)).toList();
			if (CollectionUtils.isEmpty(defaultOwnerIds)) {
				continue;
			}
			ruleMap.put(defaultOwnerIds, rule);
		}
		return ruleMap;
	}

	/**
	 * 判断客户是否需要回收
	 * @return 是否回收
	 */
	public boolean checkClosed(Opportunity opportunity, OpportunityRule rule) {
		boolean allMatch = StringUtils.equals(CombineSearch.SearchMode.AND.name(), rule.getOperator());
		List<RuleConditionDTO> conditions = JSON.parseArray(rule.getCondition(), RuleConditionDTO.class);
		if (allMatch) {
			return conditions.stream().allMatch(condition -> isConditionMatched(condition, opportunity));
		} else {
			return conditions.stream().anyMatch(condition -> isConditionMatched(condition, opportunity));
		}
	}

	/**
	 * 是否条件匹配
	 * @param condition 条件
	 * @param opportunity 商机
	 * @return 是否匹配
	 */
	private boolean isConditionMatched(RuleConditionDTO condition, Opportunity opportunity) {
		if (StringUtils.isEmpty(condition.getValue())) {
			return false;
		}
		if (StringUtils.equals(condition.getColumn(), RecycleConditionColumnKey.OPPORTUNITY_STAGE)) {
			if (StringUtils.equals(condition.getOperator(), RecycleConditionOperator.IN.name())) {
				return condition.getValue().contains(opportunity.getStage());
			} else {
				return !condition.getValue().contains(opportunity.getStage());
			}
		} else {
			return RecycleConditionUtils.matchTime(condition, opportunity.getCreateTime());
		}
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
		List<String> ownerUserIds = userExtendService.getScopeOwnerIds(ownerIds, rule.getOrganizationId());
		if (!ownerUserIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("opportunity.access_fail"));
		}
	}
}
