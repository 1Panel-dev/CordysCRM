package io.cordys.crm.clue.service;

import io.cordys.common.dto.OptionDTO;
import io.cordys.common.util.JSON;
import io.cordys.common.util.TimeUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.ClueOwner;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolPickRule;
import io.cordys.crm.clue.dto.request.PoolCluePickRequest;
import io.cordys.crm.clue.mapper.ExtClueCapacityMapper;
import io.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import io.cordys.crm.system.dto.request.PoolBatchPickRequest;
import io.cordys.crm.system.mapper.ExtUserMapper;
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

@Service
public class PoolClueService {

	@Resource
	private BaseMapper<Clue> clueMapper;
	@Resource
	private BaseMapper<ClueOwner> ownerMapper;
	@Resource
	private BaseMapper<CluePool> poolMapper;
	@Resource
	private BaseMapper<CluePoolPickRule> pickRuleMapper;
	@Resource
	private ExtUserMapper extUserMapper;
	@Resource
	private ExtClueCapacityMapper extClueCapacityMapper;
	@Resource
	private UserExtendService userExtendService;

	public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

	/**
	 * 获取当前用户线索池选项
	 * @param currentUser 当前用户ID
	 * @param currentOrgId 当前组织ID
	 * @return 线索池选项
	 */
	public List<OptionDTO> getPoolOptions(String currentUser, String currentOrgId) {
		List<OptionDTO> options = new ArrayList<>();
		LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
		poolWrapper.eq(CluePool::getEnable, true).eq(CluePool::getOrganizationId, currentOrgId);
		List<CluePool> pools = poolMapper.selectListByLambda(poolWrapper);
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
		ownClue(request.getClueId(), currentUser, pickRule);
	}

	/**
	 * 分配线索
	 * @param id 客户ID
	 * @param assignUserId 分配用户ID
	 */
	public void assign(String id, String assignUserId, String currentOrgId) {
		validateCapacity(1, assignUserId, currentOrgId);
		ownClue(id, assignUserId, null);
	}

	/**
	 * 删除线索
	 * @param id 客户ID
	 */
	public void delete(String id) {
		LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
		clueWrapper.eq(Clue::getId, id);
		clueMapper.deleteByLambda(clueWrapper);
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
		request.getBatchIds().forEach(id -> ownClue(id, currentUser, pickRule));
	}

	/**
	 * 批量分配线索
	 * @param request 请求参数
	 * @param assignUserId 分配用户ID
	 * @param currentOrgId 当前组织ID
	 */
	public void batchAssign(PoolBatchAssignRequest request, String assignUserId, String currentOrgId) {
		validateCapacity(request.getBatchIds().size(), assignUserId, currentOrgId);
		request.getBatchIds().forEach(id -> ownClue(id, assignUserId, null));
	}

	/**
	 * 批量删除客户
	 * @param ids 客户ID集合
	 */
	public void batchDelete(List<String> ids) {
		LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
		clueWrapper.in(Clue::getId, ids);
		clueMapper.deleteByLambda(clueWrapper);
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
			throw new ArithmeticException(Translator.get("customer.capacity.over"));
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
				throw new ArithmeticException(Translator.get("customer.daily.pick.over"));
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
	private void ownClue(String clueId, String ownerId, CluePoolPickRule pickRule) {
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
					throw new ArithmeticException(Translator.get("customer.pre_owner.pick.limit"));
				}
			}
		}
		clue.setPoolId(null);
		clue.setInSharedPool(false);
		clue.setOwner(ownerId);
		clue.setCollectionTime(System.currentTimeMillis());
		clue.setUpdateTime(System.currentTimeMillis());
		clueMapper.updateById(clue);
	}
}
