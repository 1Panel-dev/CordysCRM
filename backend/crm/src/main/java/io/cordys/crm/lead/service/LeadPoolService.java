package io.cordys.crm.lead.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.crm.lead.domain.LeadPoolPickRule;
import io.cordys.crm.lead.domain.LeadPoolRecycleRule;
import io.cordys.crm.lead.domain.LeadPoolRelation;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.request.LeadPoolPageRequest;
import io.cordys.crm.lead.dto.request.LeadPoolSaveRequest;
import io.cordys.crm.lead.mapper.ExtLeadPoolMapper;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.User;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeadPoolService {

	@Resource
	private BaseMapper<LeadPool> leadPoolMapper;
	@Resource
	private BaseMapper<LeadPoolPickRule> leadPoolPickRuleMapper;
	@Resource
	private BaseMapper<LeadPoolRecycleRule> leadPoolRecycleRuleMapper;
	@Resource
	private BaseMapper<LeadPoolRelation> leadPoolRelationMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<Department> departmentMapper;
	@Resource
	private ExtLeadPoolMapper extLeadPoolMapper;

	/**
	 * 分页查询线索池
	 * @param request 分页参数
	 * @return 线索池列表
	 */
	public List<LeadPoolDTO> page(LeadPoolPageRequest request) {
		List<LeadPoolDTO> leadPools = extLeadPoolMapper.list(request);
		if (CollectionUtils.isEmpty(leadPools)) {
			return new ArrayList<>();
		}
		List<String> ownerIds = leadPools.stream().map(LeadPoolDTO::getOwnerId).toList();
		List<String> scopeIds = leadPools.stream().map(LeadPoolDTO::getScopeId).toList();
		List<String> userIds = ListUtils.union(ownerIds, scopeIds).stream().distinct().toList();
		List<User> users = userMapper.selectByIds(userIds.toArray(new String[0]));
		Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getName));
		List<Department> departments = departmentMapper.selectByIds(scopeIds.toArray(new String[0]));
		Map<String, String> departmentMap = departments.stream().collect(Collectors.toMap(Department::getId, Department::getName));
		return leadPools.stream().peek(leadPool -> {
			leadPool.setOwnerName(userMap.get(leadPool.getOwnerId()));
			leadPool.setScopeName(departmentMap.containsKey(leadPool.getScopeId()) ?
					departmentMap.get(leadPool.getScopeId()) : userMap.get(leadPool.getScopeId()));
		}).toList();
	}

	/**
	 * 保存线索池
	 * @param request 保存参数
	 */
	public void save(LeadPoolSaveRequest request, String currentUserId) {
		LeadPool pool = new LeadPool();
		BeanUtils.copyBean(pool, request);
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		LeadPoolPickRule pickRule = request.getPickRule();
		pickRule.setUpdateTime(System.currentTimeMillis());
		pickRule.setUpdateUser(currentUserId);
		LeadPoolRecycleRule recycleRule = request.getRecycleRule();
		recycleRule.setUpdateTime(System.currentTimeMillis());
		recycleRule.setUpdateUser(currentUserId);
		if (pool.getId() == null) {
			pool.setId(IDGenerator.nextStr());
			pool.setCreateTime(System.currentTimeMillis());
			pool.setCreateUser(currentUserId);
			leadPoolMapper.insert(pool);
			pickRule.setId(IDGenerator.nextStr());
			pickRule.setPoolId(pool.getId());
			pickRule.setCreateTime(System.currentTimeMillis());
			pickRule.setCreateUser(currentUserId);
			leadPoolPickRuleMapper.insert(pickRule);
			recycleRule.setId(IDGenerator.nextStr());
			recycleRule.setPoolId(pool.getId());
			recycleRule.setCreateTime(System.currentTimeMillis());
			recycleRule.setCreateUser(currentUserId);
			leadPoolRecycleRuleMapper.insert(recycleRule);
		} else {
			LeadPool oldPool = checkPoolExist(pool.getId());
			checkPoolOwner(oldPool, currentUserId);
			leadPoolMapper.update(pool);
			leadPoolPickRuleMapper.update(pickRule);
			leadPoolRecycleRuleMapper.update(recycleRule);
		}
	}

	/**
	 * 删除线索池
	 * @param id 线索池ID
	 */
	public void delete(String id,String currentUserId) {
		LeadPool pool = checkPoolExist(id);
		checkPoolOwner(pool, currentUserId);
		LambdaQueryWrapper<LeadPoolRelation> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LeadPoolRelation::getPoolId, id);
		List<LeadPoolRelation> leadPoolRelations = leadPoolRelationMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isNotEmpty(leadPoolRelations)) {
			throw new GenericException(Translator.get("lead_pool_related"));
		}
		leadPoolMapper.deleteByPrimaryKey(id);
		LeadPoolPickRule pickRule = new LeadPoolPickRule();
		pickRule.setPoolId(id);
		leadPoolPickRuleMapper.delete(pickRule);
		LeadPoolRecycleRule recycleRule = new LeadPoolRecycleRule();
		recycleRule.setPoolId(id);
		leadPoolRecycleRuleMapper.delete(recycleRule);
	}

	/**
	 * 启用/禁用线索池
	 * @param id 线索池ID
	 */
	public void switchStatus(String id, String currentUserId) {
		LeadPool pool = checkPoolExist(id);
		checkPoolOwner(pool, currentUserId);
		pool.setEnable(pool.getEnable() ^ 1);
		pool.setUpdateTime(System.currentTimeMillis());
		pool.setUpdateUser(currentUserId);
		leadPoolMapper.updateById(pool);
	}

	/**
	 * 校验线索池是否存在
	 * @param id 线索池ID
	 * @return 线索池
	 */
	private LeadPool checkPoolExist(String id) {
		LeadPool pool = leadPoolMapper.selectByPrimaryKey(id);
		if (pool == null) {
			throw new GenericException(Translator.get("lead_pool_not_exist"));
		}
		return pool;
	}

	/**
	 * 校验是否线索池的管理员
	 * @param pool 线索池
	 * @param accessUserId 访问用户ID
	 */
	private void checkPoolOwner(LeadPool pool, String accessUserId) {
		// split multiple owner by comma
		List<String> ownerIds = List.of(pool.getOwnerId().split(","));
		if (!ownerIds.contains(accessUserId)) {
			throw new GenericException(Translator.get("lead_pool_access_fail"));
		}
	}
}
