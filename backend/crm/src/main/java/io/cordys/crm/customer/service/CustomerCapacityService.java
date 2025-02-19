package io.cordys.crm.customer.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.dto.CustomerCapacityDTO;
import io.cordys.crm.customer.dto.request.CustomerCapacitySaveRequest;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerCapacityService {

	@Resource
	private BaseMapper<CustomerCapacity> customerCapacityMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Role> roleMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private UserExtendService userExtendService;

	/**
	 * 分页获取客户库容设置
	 * @return 客户库容设置列表
	 */
	public List<CustomerCapacityDTO> page(String currentOrgId) {
		List<CustomerCapacityDTO> capacityDTOS = new ArrayList<>();
		LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerCapacity::getOrganizationId, currentOrgId);
		List<CustomerCapacity> capacities = customerCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(capacities)) {
			return new ArrayList<>();
		}
		List<String> scopeIds = new ArrayList<>();
		capacities.forEach(capacity -> scopeIds.addAll(JSON.parseArray(capacity.getScopeId(), String.class)));
		List<User> users = userMapper.selectByIds(scopeIds.toArray(new String[0]));
		List<Role> roles = roleMapper.selectByIds(scopeIds.toArray(new String[0]));
		List<Department> departments = departmentMapper.selectByIds(scopeIds.toArray(new String[0]));
		capacities.forEach(capacity -> {
			CustomerCapacityDTO capacityDTO = new CustomerCapacityDTO();
			capacityDTO.setId(capacity.getId());
			capacityDTO.setCapacity(capacity.getCapacity());
			capacityDTO.setMembers(userExtendService.getScope(users, roles, departments, JSON.parseArray(capacity.getScopeId(), String.class)));
			capacityDTOS.add(capacityDTO);
		});
		return capacityDTOS;
	}

	/**
	 * 保存客户库容规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	public void save(CustomerCapacitySaveRequest request, String currentUserId, String currentOrgId) {
		LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerCapacity::getOrganizationId, currentOrgId);
		List<CustomerCapacity> oldCapacities = customerCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isNotEmpty(oldCapacities)) {
			CustomerCapacity capacity = new CustomerCapacity();
			capacity.setOrganizationId(currentOrgId);
			customerCapacityMapper.delete(capacity);
		}
		List<CustomerCapacity> capacities = new ArrayList<>();
		request.getCapacities().forEach(capacityRequest -> {
			CustomerCapacity capacity = new CustomerCapacity();
			capacity.setId(IDGenerator.nextStr());
			capacity.setOrganizationId(currentOrgId);
			capacity.setCapacity(capacityRequest.getCapacity());
			capacity.setScopeId(JSON.toJSONString(capacityRequest.getScopeIds()));
			capacity.setCreateTime(System.currentTimeMillis());
			capacity.setCreateUser(currentUserId);
			capacity.setUpdateTime(System.currentTimeMillis());
			capacity.setUpdateUser(currentUserId);
			capacities.add(capacity);
		});
		customerCapacityMapper.batchInsert(capacities);
	}
}
