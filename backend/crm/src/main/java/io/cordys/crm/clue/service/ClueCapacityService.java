package io.cordys.crm.clue.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.clue.domain.ClueCapacity;
import io.cordys.crm.clue.dto.ClueCapacityDTO;
import io.cordys.crm.customer.domain.CustomerCapacity;
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
public class ClueCapacityService {

	@Resource
	private BaseMapper<ClueCapacity> clueCapacityMapper;
	@Resource
	private UserExtendService userExtendService;

	/**
	 * 分页获取线索库容设置
	 * @return 线索库容设置列表
	 */
	public List<ClueCapacityDTO> list(String currentOrgId) {
		List<ClueCapacityDTO> capacityData = new ArrayList<>();
		LambdaQueryWrapper<ClueCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ClueCapacity::getOrganizationId, currentOrgId).orderByDesc(ClueCapacity::getCreateTime);;
		List<ClueCapacity> capacities = clueCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(capacities)) {
			return new ArrayList<>();
		}
		capacities.stream().sorted(Comparator.comparing(ClueCapacity::getCreateTime)).forEach(capacity -> {
			ClueCapacityDTO capacityDTO = new ClueCapacityDTO();
			capacityDTO.setId(capacity.getId());
			capacityDTO.setCapacity(capacity.getCapacity());
			capacityDTO.setMembers(userExtendService.getScope(JSON.parseArray(capacity.getScopeId(), String.class)));
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
		LambdaQueryWrapper<ClueCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ClueCapacity::getOrganizationId, currentOrgId);
		List<ClueCapacity> oldCapacities = clueCapacityMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isNotEmpty(oldCapacities)) {
			oldCapacities.forEach(capacity -> clueCapacityMapper.deleteByPrimaryKey(capacity.getId()));
		}
		List<ClueCapacity> newCapacities = new ArrayList<>();
		capacities.forEach(capacityRequest -> {
			ClueCapacity capacity = new ClueCapacity();
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
		clueCapacityMapper.batchInsert(newCapacities);
	}
}
