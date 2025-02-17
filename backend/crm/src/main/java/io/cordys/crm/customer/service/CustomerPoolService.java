package io.cordys.crm.customer.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.domain.CustomerPoolRelation;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolPageRequest;
import io.cordys.crm.customer.dto.request.CustomerPoolSaveRequest;
import io.cordys.crm.customer.mapper.ExtCustomerPoolMapper;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.User;
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
	private BaseMapper<CustomerPool> customerPoolMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> customerPoolPickRuleMapper;
	@Resource
	private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
	@Resource
	private BaseMapper<CustomerPoolRelation> customerPoolRelationMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private ExtCustomerPoolMapper extCustomerPoolMapper;

	/**
	 * 分页获取公海池
	 * @param request 分页参数
	 * @return 公海池列表
	 */
	public List<CustomerPoolDTO> page(CustomerPoolPageRequest request, String organizationId) {
		List<CustomerPoolDTO> customerPools = extCustomerPoolMapper.list(request, organizationId);
		if (CollectionUtils.isEmpty(customerPools)) {
			return new ArrayList<>();
		}
		List<String> scopeIds = new ArrayList<>();
		List<String> ownerIds = new ArrayList<>();
		customerPools.forEach(customerPool -> {
			scopeIds.addAll(List.of(customerPool.getScopeId().split(",")));
			ownerIds.addAll(List.of(customerPool.getOwnerId().split(",")));
		});
		List<String> unionIds = ListUtils.union(scopeIds, ownerIds);
		List<User> users = userMapper.selectByIds(unionIds.toArray(new String[0]));
		List<Department> departments = departmentMapper.selectByIds(unionIds.toArray(new String[0]));
		customerPools.forEach(customerPoolDTO -> {
			customerPoolDTO.setScopeNames(transferIdToName(users, departments, List.of(customerPoolDTO.getScopeId().split(","))));
			customerPoolDTO.setOwnerNames(transferIdToName(users, departments, List.of(customerPoolDTO.getOwnerId().split(","))));
		});
		return customerPools;
	}

	/**
	 * 保存公海池
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	public void save(CustomerPoolSaveRequest request, String currentUserId) {
		CustomerPool customerPool = new CustomerPool();
		BeanUtils.copyBean(customerPool, request);
		customerPool.setUpdateTime(System.currentTimeMillis());
		customerPool.setUpdateUser(currentUserId);
		CustomerPoolPickRule pickRule = request.getPickRule();
		pickRule.setUpdateTime(System.currentTimeMillis());
		pickRule.setUpdateUser(currentUserId);
		CustomerPoolRecycleRule recycleRule = request.getRecycleRule();
		recycleRule.setUpdateTime(System.currentTimeMillis());
		recycleRule.setUpdateUser(currentUserId);
		if (customerPool.getId() == null) {
			customerPool.setId(IDGenerator.nextStr());
			customerPool.setCreateTime(System.currentTimeMillis());
			customerPool.setCreateUser(currentUserId);
			customerPoolMapper.insert(customerPool);
			pickRule.setId(IDGenerator.nextStr());
			pickRule.setPoolId(customerPool.getId());
			pickRule.setCreateTime(System.currentTimeMillis());
			pickRule.setCreateUser(currentUserId);
			customerPoolPickRuleMapper.insert(pickRule);
			recycleRule.setId(IDGenerator.nextStr());
			recycleRule.setPoolId(customerPool.getId());
			recycleRule.setCreateTime(System.currentTimeMillis());
			recycleRule.setCreateUser(currentUserId);
			customerPoolRecycleRuleMapper.insert(recycleRule);
		} else {
			CustomerPool oldPool = checkPoolExist(customerPool.getId());
			checkPoolOwner(oldPool, currentUserId);
			customerPoolMapper.update(customerPool);
			customerPoolPickRuleMapper.update(pickRule);
			customerPoolRecycleRuleMapper.update(recycleRule);
		}
	}

	/**
	 * 删除公海池
	 */
	public void delete(String id, String currentUserId) {
		CustomerPool pool = checkPoolExist(id);
		checkPoolOwner(pool, currentUserId);
		LambdaQueryWrapper<CustomerPoolRelation> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerPoolRelation::getPoolId, id);
		List<CustomerPoolRelation> customerPoolRelations = customerPoolRelationMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isNotEmpty(customerPoolRelations)) {
			throw new GenericException(Translator.get("customer_pool_related"));
		}
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
	 * @param id 线索池ID
	 * @return 线索池
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
	 * @param pool 线索池
	 * @param accessUserId 访问用户ID
	 */
	private void checkPoolOwner(CustomerPool pool, String accessUserId) {
		// split multiple owner by comma
		List<String> ownerIds = List.of(pool.getOwnerId().split(","));
		if (!ownerIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("customer_pool_access_fail"));
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
