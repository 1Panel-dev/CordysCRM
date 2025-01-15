package io.cordys.crm.customer.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.dto.request.CustomerCapacitySaveRequest;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerCapacityService {

	@Resource
	private BaseMapper<CustomerCapacity> customerCapacityMapper;

	/**
	 * 分页获取客户库容设置
	 * @return 客户库容设置列表
	 */
	public List<CustomerCapacity> page() {
		LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerCapacity::getOrganizationId, OrganizationContext.getOrganizationId());
		return customerCapacityMapper.selectListByLambda(wrapper);
	}

	/**
	 * 保存客户库容规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	public void save(CustomerCapacitySaveRequest request, String currentUserId) {
		CustomerCapacity customerCapacity = new CustomerCapacity();
		BeanUtils.copyBean(customerCapacity, request);
		if (customerCapacity.getId() == null) {
			customerCapacity.setId(IDGenerator.nextStr());
			customerCapacity.setCreateTime(System.currentTimeMillis());
			customerCapacity.setCreateUser(currentUserId);
			customerCapacity.setUpdateTime(System.currentTimeMillis());
			customerCapacity.setUpdateUser(currentUserId);
			customerCapacity.setOrganizationId(OrganizationContext.getOrganizationId());
			customerCapacityMapper.insert(customerCapacity);
		} else {
			checkCustomerCapacityExist(customerCapacity.getId());
			customerCapacity.setUpdateUser(currentUserId);
			customerCapacity.setUpdateTime(System.currentTimeMillis());
			customerCapacityMapper.update(customerCapacity);
		}
	}

	/**
	 * 删除客户库容规则
	 * @param id 客户库容规则ID
	 */
	public void delete(String id) {
		checkCustomerCapacityExist(id);
		customerCapacityMapper.deleteByPrimaryKey(id);
	}

	/**
	 * 校验客户库容规则是否存在
	 * @param id 客户库容规则ID
	 */
	private void checkCustomerCapacityExist(String id) {
		CustomerCapacity customerCapacity = customerCapacityMapper.selectByPrimaryKey(id);
		if (customerCapacity == null) {
			throw new RuntimeException(Translator.get("customer_capacity_not_exist"));
		}
	}
}
