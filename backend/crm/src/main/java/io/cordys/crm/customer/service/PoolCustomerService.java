package io.cordys.crm.customer.service;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.util.JSON;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
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
	private ExtUserMapper extUserMapper;

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
	 * 删除客户
	 * @param id 客户ID
	 */
	public void delete(String id) {
		LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.eq(Customer::getId, id);
		customerMapper.deleteByLambda(customerWrapper);
	}

	/**
	 * 分配客户
	 * @param request 请求参数
	 */
	public void assign(PoolCustomerAssignRequest request) {
		ownCustomer(request.getCustomerId(), request.getAssignUserId() );
	}

	/**
	 * 领取客户
	 * @param id 客户ID
	 * @param currentUser 当前用户ID
	 */
	public void pick(String id, String currentUser) {
		ownCustomer(id, currentUser);
	}

	/**
	 * 拥有客户
	 * @param customerId 客户ID
	 * @param ownerId 拥有人ID
	 */
	private void ownCustomer(String customerId, String ownerId) {
		Customer customer = customerMapper.selectByPrimaryKey(customerId);
		customer.setInSharedPool(false);
		customer.setOwner(ownerId);
		customer.setCollectionTime(System.currentTimeMillis());
		customer.setUpdateTime(System.currentTimeMillis());
		customerMapper.updateById(customer);
	}
}
