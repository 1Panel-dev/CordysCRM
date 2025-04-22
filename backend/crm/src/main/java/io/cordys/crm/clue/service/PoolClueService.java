package io.cordys.crm.clue.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.TimeUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.clue.domain.*;
import io.cordys.crm.clue.dto.CluePoolDTO;
import io.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import io.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import io.cordys.crm.clue.dto.request.PoolCluePickRequest;
import io.cordys.crm.clue.mapper.ExtClueCapacityMapper;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.RuleConditionDTO;
import io.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import io.cordys.crm.system.dto.request.PoolBatchPickRequest;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PoolClueService {

	@Resource
	private BaseMapper<Clue> clueMapper;
	@Resource
	private BaseMapper<ClueOwner> ownerMapper;
	@Resource
	private BaseMapper<CluePool> poolMapper;
	@Resource
	private BaseMapper<User> userMapper;
	@Resource
	private BaseMapper<CluePoolPickRule> pickRuleMapper;
	@Resource
	private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
	@Resource
	private ExtClueCapacityMapper extClueCapacityMapper;
	@Resource
	private UserExtendService userExtendService;
	@Resource
	private LogService logService;

	public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

	/**
	 * 获取当前用户线索池选项
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 线索池选项
	 */
	public List<CluePoolDTO> getPoolOptions(String currentUser, String currentOrgId) {
		List<CluePoolDTO> options = new ArrayList<>();
		LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
		poolWrapper.eq(CluePool::getEnable, true).eq(CluePool::getOrganizationId, currentOrgId);
		List<CluePool> pools = poolMapper.selectListByLambda(poolWrapper);
		if (CollectionUtils.isEmpty(pools)) {
			return new ArrayList<>();
		}

		List<String> userIds = pools.stream().flatMap(pool -> Stream.of(pool.getCreateUser(), pool.getUpdateUser())).toList();
		List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
		Map<String, String> userMap = createOrUpdateUsers.stream()
				.collect(Collectors.toMap(User::getId, User::getName));

		List<String> poolIds = pools.stream()
				.map(CluePool::getId)
				.toList();

		LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.in(CluePoolPickRule::getPoolId, poolIds);

		List<CluePoolPickRule> pickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		Map<String, CluePoolPickRule> pickRuleMap = pickRules.stream()
				.collect(Collectors.toMap(CluePoolPickRule::getPoolId, pickRule -> pickRule));

		LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
		recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);

		List<CluePoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
		Map<String, CluePoolRecycleRule> recycleRuleMap = recycleRules.stream()
				.collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, recycleRule -> recycleRule));
		pools.forEach(pool -> {
			List<String> scopeIds = JSON.parseArray(pool.getScopeId(), String.class);
			List<String> ownerUserIds = userExtendService.getScopeOwnerIds(scopeIds, pool.getOrganizationId());
			if (ownerUserIds.contains(currentUser)) {
				CluePoolDTO poolDTO = new CluePoolDTO();
				BeanUtils.copyBean(poolDTO, pool);
				poolDTO.setMembers(userExtendService.getScope(JSON.parseArray(pool.getScopeId(), String.class)));
				poolDTO.setOwners(userExtendService.getScope(JSON.parseArray(pool.getOwnerId(), String.class)));
				poolDTO.setCreateUserName(userMap.get(pool.getCreateUser()));
				poolDTO.setUpdateUserName(userMap.get(pool.getUpdateUser()));

				CluePoolPickRuleDTO pickRule = new CluePoolPickRuleDTO();
				BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
				CluePoolRecycleRuleDTO recycleRule = new CluePoolRecycleRuleDTO();
				CluePoolRecycleRule cluePoolRecycleRule = recycleRuleMap.get(pool.getId());
				BeanUtils.copyBean(recycleRule, cluePoolRecycleRule);
				recycleRule.setConditions(JSON.parseArray(cluePoolRecycleRule.getCondition(), RuleConditionDTO.class));

				poolDTO.setPickRule(pickRule);
				poolDTO.setRecycleRule(recycleRule);
				options.add(poolDTO);
			}
		});
		return options;
	}

	/**
	 * 领取线索
	 * @param request 请求参数
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void pick(PoolCluePickRequest request, String currentUser, String currentOrgId) {
		validateCapacity(1, currentUser, currentOrgId);
		LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.eq(CluePoolPickRule::getPoolId, request.getPoolId());
		List<CluePoolPickRule> cluePoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		CluePoolPickRule pickRule = cluePoolPickRules.getFirst();
		validateDailyPickNum(1, currentUser, pickRule);
		ownClue(request.getClueId(), currentUser, pickRule, currentUser, LogType.PICK, currentOrgId);
	}

	/**
	 * 分配线索
	 * @param id 客户ID
	 * @param assignUserId 分配用户ID
	 */
	@OperationLog(module = LogModule.CLUE_POOL_INDEX, type = LogType.ASSIGN, resourceId = "{#request.clueId}")
	public void assign(String id, String assignUserId, String currentOrgId, String currentUser) {
		validateCapacity(1, assignUserId, currentOrgId);
		ownClue(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId);
	}

	/**
	 * 删除线索
	 * @param id 客户ID
	 */
	@OperationLog(module = LogModule.CLUE_POOL_INDEX, type = LogType.DELETE, resourceId = "{#id}")
	public void delete(String id) {
		Clue clue = clueMapper.selectByPrimaryKey(id);
		LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
		clueWrapper.eq(Clue::getId, id);
		clueMapper.deleteByLambda(clueWrapper);

		// 设置操作对象
		OperationLogContext.setResourceName(clue.getName());
	}

	/**
	 * 批量领取线索
	 * @param request 请求参数
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchPick(PoolBatchPickRequest request, String currentUser, String currentOrgId) {
		validateCapacity(request.getBatchIds().size(), currentUser, currentOrgId);
		LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
		pickRuleWrapper.eq(CluePoolPickRule::getPoolId, request.getPoolId());
		List<CluePoolPickRule> cluePoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
		CluePoolPickRule pickRule = cluePoolPickRules.getFirst();
		validateDailyPickNum(request.getBatchIds().size(), currentUser, pickRule);
		request.getBatchIds().forEach(id -> ownClue(id, currentUser, pickRule, currentUser, LogType.PICK, currentOrgId));
	}

	/**
	 * 批量分配线索
	 * @param request 请求参数
	 * @param assignUserId 分配用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchAssign(PoolBatchAssignRequest request, String assignUserId, String currentOrgId, String currentUser) {
		validateCapacity(request.getBatchIds().size(), assignUserId, currentOrgId);
		request.getBatchIds().forEach(id -> ownClue(id, assignUserId, null, currentUser, LogType.PICK, currentOrgId));
	}

	/**
	 * 批量删除客户
	 * @param ids 客户ID集合
	 */
	public void batchDelete(List<String> ids, String userId, String orgId) {
		List<Clue> clues = clueMapper.selectByIds(ids);
		LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
		clueWrapper.in(Clue::getId, ids);
		clueMapper.deleteByLambda(clueWrapper);

		List<LogDTO> logs = clues.stream()
				.map(clue ->
						new LogDTO(orgId, clue.getId(), userId, LogType.DELETE, LogModule.CLUE_POOL_INDEX, clue.getName())
				)
				.toList();
		logService.batchAdd(logs);
	}

	/**
	 * 校验库容
	 * @param processCount 处理数量
	 * @param ownUserId 负责人用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void validateCapacity(int processCount, String ownUserId, String currentOrgId) {
		// 实际可处理条数 = 负责人库容容量 - 所领取的数量 < 处理数量, 提示库容不足.
		Integer capacity = getUserCapacity(ownUserId, currentOrgId);
		LambdaQueryWrapper<Clue> customerWrapper = new LambdaQueryWrapper<>();
		customerWrapper.eq(Clue::getOwner, ownUserId).eq(Clue::getInSharedPool, false);
		List<Clue> clues = clueMapper.selectListByLambda(customerWrapper);
		int ownCount = clues.size();
		if (capacity != null && capacity - ownCount < processCount) {
			throw new GenericException(Translator.get("customer.capacity.over"));
		}
	}

	/**
	 * 校验每日领取数量
	 *
	 * @param pickingCount 领取数量
	 * @param ownUserId 负责人用户ID
	 * @param pickRule 领取规则
	 */
	public void validateDailyPickNum(int pickingCount, String ownUserId, CluePoolPickRule pickRule) {
		if (pickRule.getLimitOnNumber()) {
			LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
			clueWrapper
					.eq(Clue::getOwner, ownUserId)
					.eq(Clue::getInSharedPool, false)
					.between(Clue::getCollectionTime, TimeUtils.getTodayStart(), TimeUtils.getTodayStart() + DAY_MILLIS);
			List<Clue> clues = clueMapper.selectListByLambda(clueWrapper);
			int pickedCount = clues.size();
			if (pickingCount + pickedCount > pickRule.getPickNumber()) {
				throw new GenericException(Translator.get("customer.daily.pick.over"));
			}
		}
	}

	/**
	 * 获取用户库容
	 * @param userId 用户ID
	 * @param organizationId 组织ID
	 * @return 库容
	 */
	public Integer getUserCapacity(String userId, String organizationId) {
		List<String> scopeIds = userExtendService.getUserScopeIds(userId, organizationId);
		return extClueCapacityMapper.getCapacityByScopeIds(scopeIds, organizationId);
	}

	/**
	 * 拥有客户
	 * @param clueId 线索ID
	 * @param ownerId 拥有人ID
	 */
	private void ownClue(String clueId, String ownerId, CluePoolPickRule pickRule, String operateUserId, String logType, String currentOrgId) {
		Clue clue = clueMapper.selectByPrimaryKey(clueId);
		if (clue == null) {
			throw new IllegalArgumentException(Translator.get("clue.not.exist"));
		}
		if (pickRule != null && pickRule.getLimitPreOwner()) {
			LambdaQueryWrapper<ClueOwner> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(ClueOwner::getClueId, clueId);
			List<ClueOwner> clueOwners = ownerMapper.selectListByLambda(queryWrapper);
			if (CollectionUtils.isNotEmpty(clueOwners)) {
				clueOwners.sort(Comparator.comparingLong(ClueOwner::getCollectionTime).reversed());
				ClueOwner lastOwner = clueOwners.getFirst();
				if (StringUtils.equals(lastOwner.getOwner(), ownerId) &&
						System.currentTimeMillis() - lastOwner.getCollectionTime() < pickRule.getPickIntervalDays() * DAY_MILLIS) {
					throw new GenericException(Translator.get("customer.pre_owner.pick.limit"));
				}
			}
		}
		clue.setPoolId(null);
		clue.setInSharedPool(false);
		clue.setOwner(ownerId);
		clue.setCollectionTime(System.currentTimeMillis());
		clue.setUpdateTime(System.currentTimeMillis());
		clueMapper.updateById(clue);

		// 日志
		LogDTO logDTO = new LogDTO(currentOrgId, clue.getId(), operateUserId, logType, LogModule.CLUE_POOL_INDEX, clue.getName());
		logService.add(logDTO);
	}
}
