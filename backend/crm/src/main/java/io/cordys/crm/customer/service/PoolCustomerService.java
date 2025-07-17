package io.cordys.crm.customer.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.TimeUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.*;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.CustomerPoolPickRuleDTO;
import io.cordys.crm.customer.dto.CustomerPoolRecycleRuleDTO;
import io.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import io.cordys.crm.customer.mapper.ExtCustomerCapacityMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.FilterConditionDTO;
import io.cordys.crm.system.dto.RuleConditionDTO;
import io.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import io.cordys.crm.system.dto.request.PoolBatchPickRequest;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PoolCustomerService {

	@Resource
	private BaseMapper<Customer> customerMapper;
	@Resource
	private ExtCustomerMapper extCustomerMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<CustomerOwner> ownerMapper;
	@Resource
	private BaseMapper<CustomerPool> poolMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> pickRuleMapper;
	@Resource
	private BaseMapper<CustomerPoolRecycleRule> recycleRuleMapper;
	@Resource
	private ExtCustomerCapacityMapper extCustomerCapacityMapper;
	@Resource
	private UserExtendService userExtendService;
	@Resource
	private LogService logService;
	@Resource
	private CommonNoticeSendService commonNoticeSendService;

	public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

	/**
	 * 获取当前用户公海选项
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 公海选项
	 */
	public List<CustomerPoolDTO> getPoolOptions(String currentUser, String currentOrgId) {
		List<CustomerPoolDTO> options = new ArrayList<>();
		LambdaQueryWrapper<CustomerPool> poolWrapper = new LambdaQueryWrapper<>();
		poolWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getOrganizationId, currentOrgId);
		poolWrapper.orderByDesc(CustomerPool::getUpdateTime);
		List<CustomerPool> pools = poolMapper.selectListByLambda(poolWrapper);
		if (CollectionUtils.isEmpty(pools)) {
			return new ArrayList<>();
		}


		List<String> userIds = pools.stream().flatMap(pool -> Stream.of(pool.getCreateUser(), pool.getUpdateUser())).toList();
		List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
		Map<String, String> userMap = createOrUpdateUsers.stream()
				.collect(Collectors.toMap(User::getId, User::getName));

		List<String> poolIds = pools.stream()
				.map(CustomerPool::getId)
				.toList();
		LambdaQueryWrapper<CustomerPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.in(CustomerPoolPickRule::getPoolId, poolIds);

		List<CustomerPoolPickRule> pickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		Map<String, CustomerPoolPickRule> pickRuleMap = pickRules.stream()
				.collect(Collectors.toMap(CustomerPoolPickRule::getPoolId, pickRule -> pickRule));

		LambdaQueryWrapper<CustomerPoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
		recycleRuleWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);

		List<CustomerPoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
		Map<String, CustomerPoolRecycleRule> recycleRuleMap = recycleRules.stream()
				.collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, recycleRule -> recycleRule));

		pools.forEach(pool -> {
			List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), currentOrgId);
			List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), currentOrgId);
			if (scopeIds.contains(currentUser) || ownerIds.contains(currentUser) || StringUtils.equals(currentUser, InternalUser.ADMIN.getValue())) {
				CustomerPoolDTO poolDTO = new CustomerPoolDTO();
				BeanUtils.copyBean(poolDTO, pool);

				poolDTO.setMembers(userExtendService.getScope(JSON.parseArray(pool.getScopeId(), String.class)));
				poolDTO.setOwners(userExtendService.getScope(JSON.parseArray(pool.getOwnerId(), String.class)));
				poolDTO.setCreateUserName(userMap.get(pool.getCreateUser()));
				poolDTO.setUpdateUserName(userMap.get(pool.getUpdateUser()));

				CustomerPoolPickRuleDTO pickRule = new CustomerPoolPickRuleDTO();
				BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
				CustomerPoolRecycleRuleDTO recycleRule = new CustomerPoolRecycleRuleDTO();
				CustomerPoolRecycleRule customerPoolRecycleRule = recycleRuleMap.get(pool.getId());
				BeanUtils.copyBean(recycleRule, customerPoolRecycleRule);
				recycleRule.setConditions(JSON.parseArray(customerPoolRecycleRule.getCondition(), RuleConditionDTO.class));
				poolDTO.setPickRule(pickRule);
				poolDTO.setRecycleRule(recycleRule);
				poolDTO.setEditable(ownerIds.contains(currentUser));
				options.add(poolDTO);
			}
		});
		return options;
	}

	/**
	 * 领取客户
	 * @param request 请求参数
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void pick(PoolCustomerPickRequest request, String currentUser, String currentOrgId) {
		CustomerPool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
		validateCapacity(1, currentUser, currentOrgId);
		LambdaQueryWrapper<CustomerPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.eq(CustomerPoolPickRule::getPoolId, request.getPoolId());
		List<CustomerPoolPickRule> customerPoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		CustomerPoolPickRule pickRule = customerPoolPickRules.getFirst();
		boolean poolAdmin = userExtendService.isPoolAdmin(JSON.parseArray(pool.getOwnerId(), String.class), currentUser, currentOrgId);
		if (!poolAdmin) {
			validateDailyPickNum(1, currentUser, pickRule);
		}
		ownCustomer(request.getCustomerId(), currentUser, pickRule, currentUser, LogType.PICK, currentOrgId, poolAdmin);
	}

	/**
	 * 分配客户
	 * @param id 客户ID
	 * @param assignUserId 分配用户ID
	 */
	public void assign(String id, String assignUserId, String currentOrgId, String currentUser) {
		validateCapacity(1, assignUserId, currentOrgId);
		ownCustomer(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId, false);
	}

	/**
	 * 删除客户
	 * @param id 客户ID
	 */
	@OperationLog(module = LogModule.CUSTOMER_POOL, type = LogType.DELETE, resourceId = "{#id}")
	public void delete(String id) {
		Customer customer = customerMapper.selectByPrimaryKey(id);
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.eq(Customer::getId, id);
		customerMapper.deleteByLambda(customerWrapper);

		// 设置操作对象
		OperationLogContext.setResourceName(customer.getName());
	}

	/**
	 * 批量领取客户
	 * @param request 请求参数
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchPick(PoolBatchPickRequest request, String currentUser, String currentOrgId) {
		CustomerPool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
		validateCapacity(request.getBatchIds().size(), currentUser, currentOrgId);
		LambdaQueryWrapper<CustomerPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.eq(CustomerPoolPickRule::getPoolId, request.getPoolId());
		List<CustomerPoolPickRule> customerPoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		CustomerPoolPickRule pickRule = customerPoolPickRules.getFirst();
		boolean poolAdmin = userExtendService.isPoolAdmin(JSON.parseArray(pool.getOwnerId(), String.class), currentUser, currentOrgId);
		if (!poolAdmin) {
			validateDailyPickNum(request.getBatchIds().size(), currentUser, pickRule);
		}
		request.getBatchIds().forEach(id -> ownCustomer(id, currentUser, pickRule, currentUser, LogType.PICK, currentOrgId, poolAdmin));
	}

	/**
	 * 批量分配客户
	 * @param request 请求参数
	 * @param assignUserId 分配用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchAssign(PoolBatchAssignRequest request, String assignUserId, String currentOrgId, String currentUser) {
		validateCapacity(request.getBatchIds().size(), assignUserId, currentOrgId);
		request.getBatchIds().forEach(id -> ownCustomer(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId, false));
	}

	/**
	 * 批量删除客户
	 * @param ids 客户ID集合
	 */
	public void batchDelete(List<String> ids, String userId, String orgId) {
		List<Customer> customers = customerMapper.selectByIds(ids);
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.in(Customer::getId, ids);
		customerMapper.deleteByLambda(customerWrapper);

		List<LogDTO> logs = customers.stream()
				.map(customer ->
						new LogDTO(orgId, customer.getId(), userId, LogType.DELETE, LogModule.CUSTOMER_POOL, customer.getName())
				)
				.toList();
		logService.batchAdd(logs);
	}

	/**
	 * 校验库容
	 * @param processCount 处理数量
	 * @param ownUserId 负责人用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void validateCapacity(int processCount, String ownUserId, String currentOrgId) {
		// 实际可处理条数 = 负责人库容容量 - 所领取的数量 < 处理数量, 提示库容不足.
		CustomerCapacity customerCapacity = getUserCapacity(ownUserId, currentOrgId);
		if (customerCapacity == null || customerCapacity.getCapacity() == null) {
			return;
		}
		List<FilterConditionDTO> conditions = StringUtils.isEmpty(customerCapacity.getFilter()) ?
				new ArrayList<>() : JSON.parseArray(customerCapacity.getFilter(), FilterConditionDTO.class);
		List<FilterConditionDTO> enableConditions = conditions.stream().filter(FilterConditionDTO::getEnable).toList();
		int filter = 0;
		if (CollectionUtils.isNotEmpty(enableConditions)) {
			filter = (int) extCustomerMapper.filterOwnerCount(ownUserId, enableConditions);
		}
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.eq(Customer::getOwner, ownUserId).eq(Customer::getInSharedPool, false);
		int ownCount = customerMapper.selectListByLambda(customerWrapper).size();
		if (customerCapacity.getCapacity() - (ownCount - filter) < processCount) {
			throw new GenericException(Translator.getWithArgs("customer.capacity.over", Math.max(customerCapacity.getCapacity() - ownCount, 0)));
		}
	}

	/**
	 * 校验每日领取数量
	 *
	 * @param pickingCount 领取数量
	 * @param ownUserId 负责人用户ID
	 * @param pickRule 领取规则
	 */
	public void validateDailyPickNum(int pickingCount, String ownUserId, CustomerPoolPickRule pickRule) {
		if (pickRule.getLimitOnNumber()) {
			LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
			customerWrapper
					.eq(Customer::getOwner, ownUserId)
					.eq(Customer::getInSharedPool, false)
					.between(Customer::getCollectionTime, TimeUtils.getTodayStart(), TimeUtils.getTodayStart() + DAY_MILLIS);
			List<Customer> customers = customerMapper.selectListByLambda(customerWrapper);
			int pickedCount = customers.size();
			if (pickingCount + pickedCount > pickRule.getPickNumber()) {
				throw new GenericException(Translator.get("customer.daily.pick.over"));
			}
		}
	}

	/**
	 * 获取用户库容
	 * @param userId 用户ID
	 * @param organizationId 组织ID
	 * @return 库容
	 */
	public CustomerCapacity getUserCapacity(String userId, String organizationId) {
		List<String> scopeIds = userExtendService.getUserScopeIds(userId, organizationId);
		return extCustomerCapacityMapper.getCapacityByScopeIds(scopeIds, organizationId);
	}

	/**
	 * 拥有客户
	 * @param customerId 客户ID
	 * @param ownerId 拥有人ID
	 */
	private void ownCustomer(String customerId, String ownerId, CustomerPoolPickRule pickRule,
							 String operateUserId, String logType, String currentOrgId, boolean isPoolAdmin) {
		Customer customer = customerMapper.selectByPrimaryKey(customerId);
		if (customer == null) {
			throw new IllegalArgumentException(Translator.get("customer.not.exist"));
		}
		if (!isPoolAdmin && pickRule != null && pickRule.getLimitPreOwner()) {
			LambdaQueryWrapper<CustomerOwner> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(CustomerOwner::getCustomerId, customerId);
			List<CustomerOwner> customerOwners = ownerMapper.selectListByLambda(queryWrapper);
			if (CollectionUtils.isNotEmpty(customerOwners)) {
				customerOwners.sort(Comparator.comparingLong(CustomerOwner::getCollectionTime).reversed());
				CustomerOwner lastOwner = customerOwners.getFirst();
				if (StringUtils.equals(lastOwner.getOwner(), ownerId) &&
						System.currentTimeMillis() < pickRule.getPickIntervalDays() * DAY_MILLIS + lastOwner.getEndTime()) {
					LocalDateTime nextPickTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(pickRule.getPickIntervalDays() * DAY_MILLIS + lastOwner.getEndTime()),
							ZoneId.systemDefault());
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					throw new GenericException(Translator.getWithArgs("customer.pre_owner.pick.limit", nextPickTime.format(formatter)));
				}
			}
		}
		customer.setPoolId(null);
		customer.setInSharedPool(false);
		customer.setOwner(ownerId);
		customer.setCollectionTime(System.currentTimeMillis());
		customer.setUpdateTime(System.currentTimeMillis());
		customerMapper.updateById(customer);

		// 日志
		LogDTO logDTO = new LogDTO(currentOrgId, customer.getId(), operateUserId, logType, LogModule.CUSTOMER_POOL, customer.getName());
		logService.add(logDTO);

		// 分配通知
		if (StringUtils.equals(logType, LogType.ASSIGN)) {
			commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER, NotificationConstants.Event.HIGH_SEAS_CUSTOMER_DISTRIBUTED,
					customer.getName(), operateUserId, currentOrgId, List.of(customer.getOwner()), true);
		}
	}
}
