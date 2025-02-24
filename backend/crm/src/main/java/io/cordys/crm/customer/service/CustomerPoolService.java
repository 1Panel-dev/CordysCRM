package io.cordys.crm.customer.service;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.domain.CustomerPoolRelation;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolAddRequest;
import io.cordys.crm.customer.dto.CustomerPoolPickRuleDTO;
import io.cordys.crm.customer.dto.CustomerPoolRecycleRuleDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolUpdateRequest;
import io.cordys.crm.customer.mapper.ExtCustomerPoolMapper;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.RuleConditionDTO;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
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
public class CustomerPoolService {

	@Resource
	private ExtUserMapper extUserMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Role> roleMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private BaseMapper<CustomerPool> customerPoolMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> customerPoolPickRuleMapper;
	@Resource
	private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
	@Resource
	private BaseMapper<CustomerPoolRelation> customerPoolRelationMapper;
	@Resource
	private ExtCustomerPoolMapper extCustomerPoolMapper;
	@Resource
	private UserExtendService userExtendService;

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
		List<String> userIds = new ArrayList<>();
		List<String> scopeIds = new ArrayList<>();
		List<String> ownerIds = new ArrayList<>();

		pools.forEach(pool -> {
			userIds.add(pool.getCreateUser());
			userIds.add(pool.getUpdateUser());
			scopeIds.addAll(JSON.parseArray(pool.getScopeId(), String.class));
			ownerIds.addAll(JSON.parseArray(pool.getOwnerId(), String.class));
		});

		List<String> unionIds = ListUtils.union(scopeIds, ownerIds)
				.stream()
				.distinct()
				.toList();

		List<User> users = userMapper.selectByIds(unionIds.toArray(new String[0]));
		List<Role> roles = roleMapper.selectByIds(unionIds.toArray(new String[0]));
		List<Department> departments = departmentMapper.selectByIds(unionIds.toArray(new String[0]));
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

		pools.forEach(pool -> {
			pool.setMembers(userExtendService.getScope(users, roles, departments,
					JSON.parseArray(pool.getScopeId(), String.class)));
			pool.setOwners(userExtendService.getScope(users, roles, departments,
					JSON.parseArray(pool.getOwnerId(), String.class)));
			pool.setCreateUserName(userMap.get(pool.getCreateUser()));
			pool.setUpdateUserName(userMap.get(pool.getUpdateUser()));

			CustomerPoolPickRuleDTO pickRule = new CustomerPoolPickRuleDTO();
			BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
			CustomerPoolRecycleRuleDTO recycleRule = new CustomerPoolRecycleRuleDTO();
			CustomerPoolRecycleRule customerPoolRecycleRule = recycleRuleMap.get(pool.getId());
			BeanUtils.copyBean(recycleRule, customerPoolRecycleRule);
			recycleRule.setConditions(JSON.parseArray(customerPoolRecycleRule.getCondition(), RuleConditionDTO.class));

			pool.setPickRule(pickRule);
			pool.setRecycleRule(recycleRule);
		});

		return pools;
	}

	/**
	 * 新增公海池
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
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
		recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
		recycleRule.setPoolId(pool.getId());
		recycleRule.setCreateUser(currentUserId);
		recycleRule.setCreateTime(System.currentTimeMillis());
		recycleRule.setUpdateUser(currentUserId);
		recycleRule.setUpdateTime(System.currentTimeMillis());
		customerPoolRecycleRuleMapper.insert(recycleRule);
	}

	/**
	 * 修改公海池
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	public void update(CustomerPoolUpdateRequest request, String currentUserId, String organizationId) {
		CustomerPool oldPool = checkPoolExist(request.getId());
		checkPoolOwner(oldPool, currentUserId);
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
		recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
		recycleRule.setUpdateUser(currentUserId);
		recycleRule.setUpdateTime(System.currentTimeMillis());
		extCustomerPoolMapper.updateRecycleRule(recycleRule);
	}

	/**
	 * 公海池是否存在未领取线索
	 *
	 * @param id 线索池ID
	 */
	public boolean checkNoPick(String id) {
		LambdaQueryWrapper<CustomerPoolRelation> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerPoolRelation::getPoolId, id)
				.eq(CustomerPoolRelation::getPicked, false);
		List<CustomerPoolRelation> relations = customerPoolRelationMapper.selectListByLambda(wrapper);
		return CollectionUtils.isNotEmpty(relations);
	}

	/**
	 * 删除公海池
	 */
	public void delete(String id, String currentUserId) {
		CustomerPool pool = checkPoolExist(id);
		checkPoolOwner(pool, currentUserId);
		customerPoolMapper.deleteByPrimaryKey(id);
		CustomerPoolPickRule pickRule = new CustomerPoolPickRule();
		pickRule.setPoolId(id);
		customerPoolPickRuleMapper.delete(pickRule);
		CustomerPoolRecycleRule recycleRule = new CustomerPoolRecycleRule();
		recycleRule.setPoolId(id);
		customerPoolRecycleRuleMapper.delete(recycleRule);
	}

	/**
	 * 启用/禁用公海池
	 * @param id 线索池ID
	 */
	public void switchStatus(String id, String currentUserId) {
		CustomerPool pool = checkPoolExist(id);
		checkPoolOwner(pool, currentUserId);
		pool.setEnable(!pool.getEnable());
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		customerPoolMapper.updateById(pool);
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
	 * 校验是否公海池的管理员
	 * @param pool 公海池
	 * @param accessUserId 访问用户ID
	 */
	private void checkPoolOwner(CustomerPool pool, String accessUserId) {
		List<String> ownerIds = JSON.parseArray(pool.getOwnerId(), String.class);
		List<String> ownerUserIds = extUserMapper.getUserIdsByScope(ownerIds, pool.getOrganizationId());
		if (!ownerUserIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("customer_pool_access_fail"));
		}
	}
}
