package io.cordys.crm.clue.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.clue.domain.ClueCapacity;
import io.cordys.crm.clue.dto.ClueCapacityDTO;
import io.cordys.crm.clue.mapper.ExtClueCapacityMapper;
import io.cordys.crm.system.dto.request.CapacityAddRequest;
import io.cordys.crm.system.dto.request.CapacityUpdateRequest;
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
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ClueCapacityService {

	@Resource
	private BaseMapper<ClueCapacity> clueCapacityMapper;
	@Resource
	private UserExtendService userExtendService;
	@Resource
	private ExtClueCapacityMapper extClueCapacityMapper;

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

	public void add(CapacityAddRequest request, String currentUserId, String currentOrgId) {
		List<ClueCapacity> oldCapacities = clueCapacityMapper.selectAll(null);
		List<String> targetScopeIds = oldCapacities.stream().flatMap(capacity -> JSON.parseArray(capacity.getScopeId(), String.class).stream())
				.collect(Collectors.toList());
		boolean duplicate = userExtendService.hasDuplicateScopeObj(request.getScopeIds(), targetScopeIds, currentOrgId);
		if (duplicate) {
			throw new GenericException(Translator.get("capacity.scope.duplicate"));
		}
		ClueCapacity capacity = new ClueCapacity();
		capacity.setId(IDGenerator.nextStr());
		capacity.setOrganizationId(currentOrgId);
		capacity.setCapacity(request.getCapacity());
		capacity.setScopeId(JSON.toJSONString(request.getScopeIds()));
		capacity.setCreateTime(System.currentTimeMillis());
		capacity.setCreateUser(currentUserId);
		capacity.setUpdateTime(System.currentTimeMillis());
		capacity.setUpdateUser(currentUserId);
		clueCapacityMapper.insert(capacity);
	}

	public void update(CapacityUpdateRequest request, String currentUserId, String currentOrgId) {
		ClueCapacity oldCapacity = clueCapacityMapper.selectByPrimaryKey(request.getId());
		if (oldCapacity == null) {
			throw new GenericException(Translator.get("capacity.not.exist"));
		}
		LambdaQueryWrapper<ClueCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ClueCapacity::getOrganizationId, currentOrgId).nq(ClueCapacity::getId, request.getId());
		List<ClueCapacity> oldCapacities = clueCapacityMapper.selectListByLambda(wrapper);
		List<String> targetScopeIds = oldCapacities.stream().flatMap(capacity -> JSON.parseArray(capacity.getScopeId(), String.class).stream())
				.collect(Collectors.toList());
		boolean duplicate = userExtendService.hasDuplicateScopeObj(request.getScopeIds(), targetScopeIds, currentOrgId);
		if (duplicate) {
			throw new GenericException(Translator.get("capacity.scope.duplicate"));
		}
		oldCapacity.setScopeId(JSON.toJSONString(request.getScopeIds()));
		oldCapacity.setCapacity(request.getCapacity());
		oldCapacity.setUpdateTime(System.currentTimeMillis());
		oldCapacity.setUpdateUser(currentUserId);
		extClueCapacityMapper.updateCapacity(oldCapacity);
	}

	public void delete(String id) {
		clueCapacityMapper.deleteByPrimaryKey(id);
	}
}