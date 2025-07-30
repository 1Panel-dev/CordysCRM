package io.cordys.crm.customer.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.common.utils.RecycleConditionUtils;
import io.cordys.crm.customer.domain.*;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.CustomerPoolFieldConfigDTO;
import io.cordys.crm.customer.dto.CustomerPoolPickRuleDTO;
import io.cordys.crm.customer.dto.CustomerPoolRecycleRuleDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolAddRequest;
import io.cordys.crm.customer.dto.request.CustomerPoolUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerPoolMapper;
import io.cordys.crm.system.constants.RecycleConditionColumnKey;
import io.cordys.crm.system.constants.RecycleConditionOperator;
import io.cordys.crm.system.constants.RecycleConditionScopeKey;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.RuleConditionDTO;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
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
public class CustomerPoolService {

	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Customer> customerMapper;
	@Resource
	private BaseMapper<CustomerPool> customerPoolMapper;
	@Resource
	private BaseMapper<CustomerPoolHiddenField> customerPoolHiddenFieldMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> customerPoolPickRuleMapper;
	@Resource
	private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
	@Resource
	private ExtCustomerPoolMapper extCustomerPoolMapper;
	@Resource
	private UserExtendService userExtendService;
	@Resource
	private ModuleFormCacheService moduleFormCacheService;

	/**
	 * 分页获取公海池
	 * @param request 分页参数
	 * @return 公海池列表
	 */
	public List<CustomerPoolDTO> page(BasePageRequest request, String organizationId) {
		List<CustomerPoolDTO> pools = extCustomerPoolMapper.list(request, organizationId);
		if (CollectionUtils.isEmpty(pools)) {
			return new ArrayList<>();
		}

		List<String> userIds = pools.stream().flatMap(pool -> Stream.of(pool.getCreateUser(), pool.getUpdateUser())).toList();
		List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
		Map<String, String> userMap = createOrUpdateUsers.stream()
				.collect(Collectors.toMap(User::getId, User::getName));

		List<String> poolIds = pools.stream()
				.map(CustomerPoolDTO::getId)
				.toList();

		LambdaQueryWrapper<CustomerPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.in(CustomerPoolPickRule::getPoolId, poolIds);

		List<CustomerPoolPickRule> pickRules = customerPoolPickRuleMapper.selectListByLambda(pickRuleWrapper);
		Map<String, CustomerPoolPickRule> pickRuleMap = pickRules.stream()
				.collect(Collectors.toMap(CustomerPoolPickRule::getPoolId, pickRule -> pickRule));

		LambdaQueryWrapper<CustomerPoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
		recycleRuleWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);

		List<CustomerPoolRecycleRule> recycleRules = customerPoolRecycleRuleMapper.selectListByLambda(recycleRuleWrapper);
		Map<String, CustomerPoolRecycleRule> recycleRuleMap = recycleRules.stream()
				.collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, recycleRule -> recycleRule));

		Map<String, List<CustomerPoolHiddenField>> hiddenFieldMap = getCustomerPoolHiddenFieldByPoolIds(poolIds)
				.stream()
				.collect(Collectors.groupingBy(CustomerPoolHiddenField::getPoolId));

		List<BaseField> fields = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), organizationId).getFields();

		pools.forEach(pool -> {
			pool.setMembers(userExtendService.getScope(JSON.parseArray(pool.getScopeId(), String.class)));
			pool.setOwners(userExtendService.getScope(JSON.parseArray(pool.getOwnerId(), String.class)));
			pool.setCreateUserName(userMap.get(pool.getCreateUser()));
			pool.setUpdateUserName(userMap.get(pool.getUpdateUser()));

			CustomerPoolPickRuleDTO pickRule = new CustomerPoolPickRuleDTO();
			BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
			CustomerPoolRecycleRuleDTO recycleRule = new CustomerPoolRecycleRuleDTO();
			CustomerPoolRecycleRule customerPoolRecycleRule = recycleRuleMap.get(pool.getId());
			BeanUtils.copyBean(recycleRule, customerPoolRecycleRule);
			recycleRule.setConditions(JSON.parseArray(customerPoolRecycleRule.getCondition(), RuleConditionDTO.class));
			delOldTime(recycleRule);
			pool.setPickRule(pickRule);
			pool.setRecycleRule(recycleRule);

			Set<String> hiddenFieldIds;
			if (hiddenFieldMap.get(pool.getId()) != null) {
				hiddenFieldIds = hiddenFieldMap.get(pool.getId()).stream()
						.map(CustomerPoolHiddenField::getFieldId)
						.collect(Collectors.toSet());
			} else {
				hiddenFieldIds = Set.of();
			}

			pool.setFieldConfigs(getFieldConfigs(fields, hiddenFieldIds));
		});

		return pools;
	}

	private void delOldTime(CustomerPoolRecycleRuleDTO recycleRule) {
		recycleRule.getConditions().forEach(condition -> {
			if (StringUtils.equals(condition.getOperator(), RecycleConditionOperator.DYNAMICS.name())) {
				String[] split = condition.getValue().split(",");
				if (StringUtils.isNotBlank(condition.getValue()) && split.length == 2 ) {
					String dateValue = split[0];
					String dateUnit = split[1];
					condition.setValue("CUSTOM,"+ dateValue + "," + dateUnit);
				}
			}
		});
	}


	public List<CustomerPoolFieldConfigDTO> getFieldConfigs(List<BaseField> fields, Set<String> hiddenFieldIds) {
		List<CustomerPoolFieldConfigDTO> hiddenFields = fields.stream().map(field -> {
			CustomerPoolFieldConfigDTO hiddenFieldDTO = new CustomerPoolFieldConfigDTO();
			hiddenFieldDTO.setFieldId(field.getId());
			hiddenFieldDTO.setFieldName(field.getName());
			hiddenFieldDTO.setEnable(!hiddenFieldIds.contains(field.getId()));
			hiddenFieldDTO.setEditable(!StringUtils.equals(field.getInternalKey(), BusinessModuleField.CUSTOMER_NAME.getKey()));
			return hiddenFieldDTO;
		}).toList();
		return hiddenFields;
	}

	public List<CustomerPoolHiddenField> getCustomerPoolHiddenFieldByPoolIds(List<String> poolIds) {
		LambdaQueryWrapper<CustomerPoolHiddenField> hiddenFieldWrapper = new LambdaQueryWrapper<>();
		hiddenFieldWrapper.in(CustomerPoolHiddenField::getPoolId, poolIds);
		return customerPoolHiddenFieldMapper.selectListByLambda(hiddenFieldWrapper);
	}

	/**
	 * 新增公海池
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
	public void add(CustomerPoolAddRequest request, String currentUserId, String organizationId) {
		CustomerPool pool = new CustomerPool();
		BeanUtils.copyBean(pool, request);
		pool.setId(IDGenerator.nextStr());
		pool.setOrganizationId(organizationId);
		pool.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
		pool.setScopeId(JSON.toJSONString(request.getScopeIds()));
		pool.setCreateTime(System.currentTimeMillis());
		pool.setCreateUser(currentUserId);
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		customerPoolMapper.insert(pool);
		CustomerPoolPickRule pickRule = new CustomerPoolPickRule();
		BeanUtils.copyBean(pickRule, request.getPickRule());
		pickRule.setId(IDGenerator.nextStr());
		pickRule.setPoolId(pool.getId());
		pickRule.setCreateUser(currentUserId);
		pickRule.setCreateTime(System.currentTimeMillis());
		pickRule.setUpdateUser(currentUserId);
		pickRule.setUpdateTime(System.currentTimeMillis());
		customerPoolPickRuleMapper.insert(pickRule);
		CustomerPoolRecycleRule recycleRule = new CustomerPoolRecycleRule();
		BeanUtils.copyBean(recycleRule, request.getRecycleRule());
		recycleRule.setId(IDGenerator.nextStr());
		try {
			recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
		} catch (Exception e) {
			throw new GenericException(Translator.get("customer_rule_condition_error"));
		}
		recycleRule.setPoolId(pool.getId());
		recycleRule.setCreateUser(currentUserId);
		recycleRule.setCreateTime(System.currentTimeMillis());
		recycleRule.setUpdateUser(currentUserId);
		recycleRule.setUpdateTime(System.currentTimeMillis());
		customerPoolRecycleRuleMapper.insert(recycleRule);

		batchInsertCustomerPoolHiddenFields(pool.getId(), request.getHiddenFieldIds());

		// 添加日志上下文
		OperationLogContext.setContext(LogContextInfo.builder()
				.modifiedValue(pool)
				.resourceId(pool.getId())
				.resourceName(Translator.get("module.customer.pool.setting"))
				.build());
	}

	/**
	 * 修改公海池
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#request.id}")
	public void update(CustomerPoolUpdateRequest request, String currentUserId, String organizationId) {
		CustomerPool originCustomerPool = checkPoolExist(request.getId());
		CustomerPool pool = new CustomerPool();
		BeanUtils.copyBean(pool, request);
		pool.setOrganizationId(organizationId);
		pool.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
		pool.setScopeId(JSON.toJSONString(request.getScopeIds()));
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		customerPoolMapper.update(pool);
		CustomerPoolPickRule pickRule = new CustomerPoolPickRule();
		BeanUtils.copyBean(pickRule, request.getPickRule());
		pickRule.setPoolId(pool.getId());
		pickRule.setUpdateUser(currentUserId);
		pickRule.setUpdateTime(System.currentTimeMillis());
		extCustomerPoolMapper.updatePickRule(pickRule);
		CustomerPoolRecycleRule recycleRule = new CustomerPoolRecycleRule();
		BeanUtils.copyBean(recycleRule, request.getRecycleRule());
		recycleRule.setPoolId(pool.getId());
		try {
			recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
		} catch (Exception e) {
			throw new GenericException(Translator.get("customer_rule_condition_error"));
		}
		recycleRule.setUpdateUser(currentUserId);
		recycleRule.setUpdateTime(System.currentTimeMillis());
		extCustomerPoolMapper.updateRecycleRule(recycleRule);

		if (request.getHiddenFieldIds() != null) {
			deleteCustomerPoolHiddenFieldByPoolId(pool.getId());
			batchInsertCustomerPoolHiddenFields(pool.getId(), request.getHiddenFieldIds());
		}

		OperationLogContext.setContext(
				LogContextInfo.builder()
						.resourceName(Translator.get("module.customer.pool.setting"))
						.originalValue(originCustomerPool)
						.modifiedValue(customerPoolMapper.selectByPrimaryKey(request.getId()))
						.build()
		);
	}

	private void batchInsertCustomerPoolHiddenFields(String poolId, Set<String> fieldIds) {
		if (CollectionUtils.isEmpty(fieldIds)) {
			return;
		}
		List<CustomerPoolHiddenField> customerPoolHiddenFields = fieldIds.stream()
				.map(fieldId -> {
					CustomerPoolHiddenField customerPoolHiddenField = new CustomerPoolHiddenField();
					customerPoolHiddenField.setFieldId(fieldId);
					customerPoolHiddenField.setPoolId(poolId);
					return customerPoolHiddenField;
				}).toList();
		customerPoolHiddenFieldMapper.batchInsert(customerPoolHiddenFields);
	}

	private void deleteCustomerPoolHiddenFieldByPoolId(String poolId) {
		CustomerPoolHiddenField customerPoolHiddenField = new CustomerPoolHiddenField();
		customerPoolHiddenField.setPoolId(poolId);
		customerPoolHiddenFieldMapper.delete(customerPoolHiddenField);
	}

	/**
	 * 公海池是否存在未领取线索
	 *
	 * @param id 线索池ID
	 */
	public boolean checkNoPick(String id) {
		LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Customer::getPoolId, id)
				.eq(Customer::getInSharedPool, true);
		List<Customer> relations = customerMapper.selectListByLambda(wrapper);
		return CollectionUtils.isNotEmpty(relations);
	}

	/**
	 * 删除公海池
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
	public void delete(String id) {
		checkPoolExist(id);
		customerPoolMapper.deleteByPrimaryKey(id);
		CustomerPoolPickRule pickRule = new CustomerPoolPickRule();
		pickRule.setPoolId(id);
		customerPoolPickRuleMapper.delete(pickRule);
		CustomerPoolRecycleRule recycleRule = new CustomerPoolRecycleRule();
		recycleRule.setPoolId(id);
		customerPoolRecycleRuleMapper.delete(recycleRule);
		deleteCustomerPoolHiddenFieldByPoolId(id);

		// 设置操作对象
		OperationLogContext.setResourceName(Translator.get("module.customer.pool.setting"));
	}

	/**
	 * 启用/禁用公海池
	 * @param id 线索池ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#id}")
	public void switchStatus(String id, String currentUserId) {
		CustomerPool pool = checkPoolExist(id);
		pool.setEnable(!pool.getEnable());
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		customerPoolMapper.updateById(pool);

		OperationLogContext.setContext(
				LogContextInfo.builder()
						.resourceName(Translator.get("module.customer.pool.setting"))
						.originalValue(pool)
						.modifiedValue(customerPoolMapper.selectByPrimaryKey(id))
						.build()
		);
	}

	/**
	 * 校验公海池是否存在
	 * @param id 公海池ID
	 * @return 公海池
	 */
	private CustomerPool checkPoolExist(String id) {
		CustomerPool pool = customerPoolMapper.selectByPrimaryKey(id);
		if (pool == null) {
			throw new GenericException(Translator.get("customer_pool_not_exist"));
		}
		return pool;
	}

	/**
	 * 获取负责人默认公海ID
	 * @param ownerIds 负责人ID集合
	 * @param organizationId 组织ID
	 * @return 默认公海
	 */
	public Map<String, CustomerPool> getOwnersDefaultPoolMap(List<String> ownerIds, String organizationId) {
		Map<String, CustomerPool> poolMap = new HashMap<>(4);
		List<CustomerPool> pools = extCustomerPoolMapper.getAllPool(organizationId);
		Map<String, List<String>> ownerScopeMap = userExtendService.getMultiScopeMap(ownerIds, organizationId);
		ownerIds.forEach(ownerId -> {
			List<CustomerPool> matchPools = matchMultiScope(ownerScopeMap.get(ownerId), pools);
			if (CollectionUtils.isEmpty(matchPools)) {
				// not found pool for owner
				return;
			}
			poolMap.put(ownerId, matchPools.getFirst());
		});

		return poolMap;
	}

	/**
	 * 匹配多个范围的公海
	 * @param scopeIds 范围ID集合
	 * @param pools 公海列表
	 * @return 命中范围的公海列表
	 */
	public List<CustomerPool> matchMultiScope(List<String> scopeIds, List<CustomerPool> pools) {
		/*
		 * 命中线索池任意范围即返回(默认按照创建时间作为优先级)
		 */
		if (CollectionUtils.isEmpty(scopeIds) || CollectionUtils.isEmpty(pools)) {
			return new ArrayList<>();
		}
		return pools.stream()
				.filter(pool -> {
					List<String> poolScopes = JSON.parseArray(pool.getScopeId(), String.class);
					return CollectionUtils.isNotEmpty(poolScopes) && CollectionUtils.containsAny(scopeIds, poolScopes);
				})
				.sorted(Comparator.comparing(CustomerPool::getCreateTime).reversed())
				.collect(Collectors.toList());
	}

	/**
	 * 计算剩余归属天数
	 *
	 * @param pool 公海池
	 * @param customer 客户
	 * @return 剩余归属天数
	 */
	public Integer calcReservedDay(CustomerPool pool, CustomerPoolRecycleRule recycleRule, CustomerListResponse customer) {
		if (pool == null || !pool.getAuto() || recycleRule == null) {
			return null;
		}

		// 判断公海是否存在入库条件
		List<RuleConditionDTO> conditions = JSON.parseArray(recycleRule.getCondition(), RuleConditionDTO.class);
		return RecycleConditionUtils.calcRecycleDays(conditions, Math.min(customer.getCollectionTime(), customer.getCreateTime()));
	}

	/**
	 * 获取负责人最佳匹配公海
	 * @param pools 公海列表
	 * @return 公海集合
	 */
	public Map<List<String>, CustomerPool> getOwnersBestMatchPoolMap(List<CustomerPool> pools) {
		Map<List<String>, CustomerPool> poolMap = new HashMap<>(4);
		pools.sort(Comparator.comparing(CustomerPool::getCreateTime).reversed());
		for (CustomerPool pool : pools) {
			List<String> exitOwnerIds = poolMap.keySet().stream().flatMap(List::stream).toList();
			List<String> scopeIds = JSON.parseArray(pool.getScopeId(), String.class);
			List<String> ownerIds = userExtendService.getScopeOwnerIds(scopeIds, pool.getOrganizationId());
			List<String> defaultOwnerIds = ownerIds.stream().distinct().filter(ownerId -> !exitOwnerIds.contains(ownerId)).toList();
			if (CollectionUtils.isEmpty(defaultOwnerIds)) {
				continue;
			}
			poolMap.put(defaultOwnerIds, pool);
		}
		return poolMap;
	}

	/**
	 * 判断客户是否需要回收
	 * @return 是否回收
	 */
	public boolean checkRecycled(Customer customer, CustomerPoolRecycleRule recycleRule) {
		boolean allMatch = StringUtils.equals(CombineSearch.SearchMode.AND.name(), recycleRule.getOperator());
		List<RuleConditionDTO> conditions = JSON.parseArray(recycleRule.getCondition(), RuleConditionDTO.class);
		if (allMatch) {
			return conditions.stream().allMatch(condition -> matchTime(condition, customer));
		} else {
			return conditions.stream().anyMatch(condition -> matchTime(condition, customer));
		}
	}

	/**
	 * 是否匹配时间规则
	 * @param condition 规则
	 * @param customer 客户
	 * @return 是否匹配
	 */
	private boolean matchTime(RuleConditionDTO condition, Customer customer) {
		if (StringUtils.equals(condition.getColumn(), RecycleConditionColumnKey.STORAGE_TIME)) {
			if (condition.getScope().contains(RecycleConditionScopeKey.CREATED)) {
				return RecycleConditionUtils.matchTime(condition, customer.getCreateTime());
			} else if (condition.getScope().contains(RecycleConditionScopeKey.PICKED)) {
				return RecycleConditionUtils.matchTime(condition, customer.getCollectionTime());
			} else {
				return RecycleConditionUtils.matchTime(condition, customer.getCreateTime()) || RecycleConditionUtils.matchTime(condition, customer.getCollectionTime());
			}
		} else {
			return RecycleConditionUtils.matchTime(condition, customer.getFollowTime());
		}
	}
}
