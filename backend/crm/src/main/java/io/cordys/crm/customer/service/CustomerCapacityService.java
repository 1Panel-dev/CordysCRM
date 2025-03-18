package io.cordys.crm.customer.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.dto.CustomerCapacityDTO;
import io.cordys.crm.system.dto.request.CapacityRequest;
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
	private UserExtendService userExtendService;
	@Resource
	private BaseMapper<CustomerCapacity> customerCapacityMapper;

	/**
	 * 获取客户库容设置
	 * @return 客户库容设置集合
	 */
	public List<CustomerCapacityDTO> list(String currentOrgId) {
		List<CustomerCapacityDTO> capacityDTOS = new ArrayList<>();
		LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerCapacity::getOrganizationId, currentOrgId);
		List<CustomerCapacity> capacities = customerCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(capacities)) {
			return new ArrayList<>();
		}
		capacities.forEach(capacity -> {
			CustomerCapacityDTO capacityDTO = new CustomerCapacityDTO();
			capacityDTO.setId(capacity.getId());
			capacityDTO.setCapacity(capacity.getCapacity());
			capacityDTO.setMembers(userExtendService.getScope(JSON.parseArray(capacity.getScopeId(), String.class)));
			capacityDTOS.add(capacityDTO);
		});
		return capacityDTOS;
	}

	/**
	 * 保存客户库容设置
	 * @param capacities 容量集合
	 * @param currentUserId 当前用户ID
	 */
	public void save(List<CapacityRequest> capacities, String currentUserId, String currentOrgId) {
		LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerCapacity::getOrganizationId, currentOrgId);
		List<CustomerCapacity> oldCapacities = customerCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isNotEmpty(oldCapacities)) {
			oldCapacities.forEach(capacity -> customerCapacityMapper.deleteByPrimaryKey(capacity.getId()));
		}
		List<CustomerCapacity> newCapacities = new ArrayList<>();
		capacities.forEach(capacityRequest -> {
			CustomerCapacity capacity = new CustomerCapacity();
			capacity.setId(IDGenerator.nextStr());
			capacity.setOrganizationId(currentOrgId);
			capacity.setCapacity(capacityRequest.getCapacity());
			capacity.setScopeId(JSON.toJSONString(capacityRequest.getScopeIds()));
			capacity.setCreateTime(System.currentTimeMillis());
			capacity.setCreateUser(currentUserId);
			capacity.setUpdateTime(System.currentTimeMillis());
			capacity.setUpdateUser(currentUserId);
			newCapacities.add(capacity);
		});
		customerCapacityMapper.batchInsert(newCapacities);
	}
}
