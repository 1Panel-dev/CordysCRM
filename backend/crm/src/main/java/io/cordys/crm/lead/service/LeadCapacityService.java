package io.cordys.crm.lead.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.lead.domain.LeadCapacity;
import io.cordys.crm.lead.dto.LeadCapacityDTO;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.CapacityRequest;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class LeadCapacityService {

	@Resource
	private BaseMapper<LeadCapacity> leadCapacityMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Role> roleMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private UserExtendService userExtendService;

	/**
	 * 分页获取线索库容设置
	 * @return 线索库容设置列表
	 */
	public List<LeadCapacityDTO> list(String currentOrgId) {
		List<LeadCapacityDTO> capacityData = new ArrayList<>();
		LambdaQueryWrapper<LeadCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LeadCapacity::getOrganizationId, currentOrgId);
		List<LeadCapacity> capacities = leadCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(capacities)) {
			return new ArrayList<>();
		}
		List<String> scopeIds = new ArrayList<>();
		capacities.forEach(capacity -> scopeIds.addAll(JSON.parseArray(capacity.getScopeId(), String.class)));
		List<User> users = userMapper.selectByIds(scopeIds.toArray(new String[0]));
		List<Role> roles = roleMapper.selectByIds(scopeIds.toArray(new String[0]));
		List<Department> departments = departmentMapper.selectByIds(scopeIds.toArray(new String[0]));
		capacities.stream().sorted(Comparator.comparing(LeadCapacity::getCreateTime)).forEach(capacity -> {
			LeadCapacityDTO capacityDTO = new LeadCapacityDTO();
			capacityDTO.setId(capacity.getId());
			capacityDTO.setCapacity(capacity.getCapacity());
			capacityDTO.setMembers(userExtendService.getScope(users, roles, departments, JSON.parseArray(capacity.getScopeId(), String.class)));
			capacityData.add(capacityDTO);
		});
		return capacityData;
	}

	/**
	 * 保存客户库容设置
	 * @param capacities 库容容量集合
	 * @param currentUserId 当前用户ID
	 */
	public void save(List<CapacityRequest> capacities, String currentUserId, String currentOrgId) {
		LambdaQueryWrapper<LeadCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LeadCapacity::getOrganizationId, currentOrgId);
		List<LeadCapacity> oldCapacities = leadCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isNotEmpty(oldCapacities)) {
			LeadCapacity capacity = new LeadCapacity();
			capacity.setOrganizationId(currentOrgId);
			leadCapacityMapper.delete(capacity);
		}
		List<LeadCapacity> newCapacities = new ArrayList<>();
		capacities.forEach(capacityRequest -> {
			LeadCapacity capacity = new LeadCapacity();
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
		leadCapacityMapper.batchInsert(newCapacities);
	}
}
