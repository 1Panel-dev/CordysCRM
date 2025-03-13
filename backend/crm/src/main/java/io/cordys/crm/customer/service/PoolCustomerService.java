package io.cordys.crm.customer.service;

import io.cordys.common.dto.OptionDTO;
import io.cordys.common.util.JSON;
import io.cordys.common.util.TimeUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.dto.request.PoolCustomerBatchAssignRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerBatchRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import io.cordys.crm.customer.mapper.ExtCustomerCapacityMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PoolCustomerService {

	@Resource
	private BaseMapper<Customer> customerMapper;
	@Resource
	private BaseMapper<CustomerPool> poolMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> pickRuleMapper;
	@Resource
	private ExtUserMapper extUserMapper;
	@Resource
	private ExtCustomerCapacityMapper extCustomerCapacityMapper;
	@Resource
	private UserExtendService userExtendService;

	public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

	/**
	 * 获取当前用户公海选项
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 公海选项
	 */
	public List<OptionDTO> getPoolOptions(String currentUser, String currentOrgId) {
		List<OptionDTO> options = new ArrayList<>();
		LambdaQueryWrapper<CustomerPool> poolWrapper = new LambdaQueryWrapper<>();
		poolWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getOrganizationId, currentOrgId);
		List<CustomerPool> pools = poolMapper.selectListByLambda(poolWrapper);
		pools.forEach(pool -> {
			List<String> scopeIds = JSON.parseArray(pool.getScopeId(), String.class);
			List<String> ownerUserIds = extUserMapper.getUserIdsByScope(scopeIds, currentOrgId);
			if (ownerUserIds.contains(currentUser)) {
				OptionDTO optionDTO = new OptionDTO();
				optionDTO.setId(pool.getId());
				optionDTO.setName(pool.getName());
				options.add(optionDTO);
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
		validateCapacity(1, currentUser, currentOrgId);
		LambdaQueryWrapper<CustomerPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.eq(CustomerPoolPickRule::getPoolId, request.getPoolId());
		List<CustomerPoolPickRule> customerPoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		CustomerPoolPickRule pickRule = customerPoolPickRules.getFirst();
		validateDailyPickNum(1, currentUser, pickRule);
		ownCustomer(request.getCustomerId(), currentUser, pickRule);
	}

	/**
	 * 分配客户
	 * @param id 客户ID
	 * @param assignUserId 分配用户ID
	 */
	public void assign(String id, String assignUserId, String currentOrgId) {
		validateCapacity(1, assignUserId, currentOrgId);
		ownCustomer(id, assignUserId, null);
	}

	/**
	 * 删除客户
	 * @param id 客户ID
	 */
	public void delete(String id) {
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.eq(Customer::getId, id);
		customerMapper.deleteByLambda(customerWrapper);
	}

	/**
	 * 批量领取客户
	 * @param request 请求参数
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchPick(PoolCustomerBatchRequest request, String currentUser, String currentOrgId) {
		validateCapacity(request.getBatchIds().size(), currentUser, currentOrgId);
		LambdaQueryWrapper<CustomerPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.eq(CustomerPoolPickRule::getPoolId, request.getPoolId());
		List<CustomerPoolPickRule> customerPoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		CustomerPoolPickRule pickRule = customerPoolPickRules.getFirst();
		validateDailyPickNum(request.getBatchIds().size(), currentUser, pickRule);
		request.getBatchIds().forEach(id -> ownCustomer(id, currentUser, pickRule));
	}

	/**
	 * 批量分配客户
	 * @param request 请求参数
	 * @param assignUserId 分配用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchAssign(PoolCustomerBatchAssignRequest request, String assignUserId, String currentOrgId) {
		validateCapacity(request.getBatchIds().size(), assignUserId, currentOrgId);
		request.getBatchIds().forEach(id -> ownCustomer(id, assignUserId, null));
	}

	/**
	 * 批量删除客户
	 * @param ids 客户ID集合
	 */
	public void batchDelete(List<String> ids) {
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.in(Customer::getId, ids);
		customerMapper.deleteByLambda(customerWrapper);
	}

	/**
	 * 校验库容
	 * @param processCount 处理数量
	 * @param ownUserId 负责人用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void validateCapacity(int processCount, String ownUserId, String currentOrgId) {
		// 实际可处理条数 = 负责人库容容量 - 所领取的数量 < 处理数量, 提示库容不足.
		Integer capacity = getUserCapacity(ownUserId, currentOrgId);
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.eq(Customer::getOwner, ownUserId).eq(Customer::getInSharedPool, false);
		List<Customer> customers = customerMapper.selectListByLambda(customerWrapper);
		int ownCount = customers.size();
		if (capacity != null && capacity - ownCount < processCount) {
			throw new ArithmeticException(Translator.get("customer.capacity.over"));
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
				throw new ArithmeticException(Translator.get("customer.daily.pick.over"));
			}
		}
	}

	/**
	 * 获取用户库容
	 * @param userId 用户ID
	 * @param organizationId 组织ID
	 * @return 库容
	 */
	public Integer getUserCapacity(String userId, String organizationId) {
		List<String> scopeIds = userExtendService.getUserScopeIds(userId, organizationId);
		return extCustomerCapacityMapper.getCapacityByScopeIds(scopeIds, organizationId);
	}

	/**
	 * 拥有客户
	 * @param customerId 客户ID
	 * @param ownerId 拥有人ID
	 */
	private void ownCustomer(String customerId, String ownerId, CustomerPoolPickRule pickRule) {
		Customer customer = customerMapper.selectByPrimaryKey(customerId);
		if (customer == null) {
			throw new IllegalArgumentException(Translator.get("customer.not.exist"));
		}
		if (pickRule != null && pickRule.getLimitPreOwner() && StringUtils.equals(customer.getOwner(), ownerId)) {
			if (System.currentTimeMillis() - customer.getCollectionTime() < pickRule.getPickIntervalDays() * DAY_MILLIS) {
				throw new ArithmeticException(Translator.get("customer.pre_owner.pick.limit"));
			}
		}
		customer.setInSharedPool(false);
		customer.setOwner(ownerId);
		customer.setCollectionTime(System.currentTimeMillis());
		customer.setUpdateTime(System.currentTimeMillis());
		customerMapper.updateById(customer);
	}
}
