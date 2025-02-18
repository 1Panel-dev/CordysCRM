package io.cordys.crm.customer.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.condition.BasePageRequest;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.domain.CustomerPoolRelation;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolPageRequest;
import io.cordys.crm.customer.dto.request.CustomerPoolSaveRequest;
import io.cordys.crm.customer.mapper.ExtCustomerPoolMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerPoolService {

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

	/**
	 * 分页获取公海池
	 * @param request 分页参数
	 * @return 公海池列表
	 */
	public List<CustomerPoolDTO> page(BasePageRequest request, String organizationId) {
		return extCustomerPoolMapper.list(request, organizationId);
	}

	/**
	 * 保存公海池
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	public void save(CustomerPoolSaveRequest request, String currentUserId, String organizationId) {
		CustomerPool pool = new CustomerPool();
		BeanUtils.copyBean(pool, request);
		pool.setOrganizationId(organizationId);
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		CustomerPoolPickRule pickRule = new CustomerPoolPickRule();
		BeanUtils.copyBean(pickRule, request.getPickRule());
		pickRule.setUpdateUser(currentUserId);
		pickRule.setUpdateTime(System.currentTimeMillis());
		CustomerPoolRecycleRule recycleRule = new CustomerPoolRecycleRule();
		BeanUtils.copyBean(recycleRule, request.getRecycleRule());
		recycleRule.setUpdateUser(currentUserId);
		recycleRule.setUpdateTime(System.currentTimeMillis());
		if (pool.getId() == null) {
			pool.setId(IDGenerator.nextStr());
			pool.setCreateTime(System.currentTimeMillis());
			pool.setCreateUser(currentUserId);
			customerPoolMapper.insert(pool);
			pickRule.setId(IDGenerator.nextStr());
			pickRule.setPoolId(pool.getId());
			pickRule.setCreateUser(currentUserId);
			pickRule.setCreateTime(System.currentTimeMillis());
			customerPoolPickRuleMapper.insert(pickRule);
			recycleRule.setId(IDGenerator.nextStr());
			recycleRule.setPoolId(pool.getId());
			recycleRule.setCreateUser(currentUserId);
			recycleRule.setCreateTime(System.currentTimeMillis());
			customerPoolRecycleRuleMapper.insert(recycleRule);
		} else {
			CustomerPool oldPool = checkPoolExist(pool.getId());
			checkPoolOwner(oldPool, currentUserId);
			customerPoolMapper.update(pool);
			pickRule.setPoolId(pool.getId());
			extCustomerPoolMapper.updatePickRule(pickRule);
			recycleRule.setPoolId(pool.getId());
			extCustomerPoolMapper.updateRecycleRule(recycleRule);
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
		// TODO: split multiple owner by comma
		List<String> ownerIds = List.of(pool.getOwnerId().split(","));
		if (!ownerIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("customer_pool_access_fail"));
		}
	}
}
