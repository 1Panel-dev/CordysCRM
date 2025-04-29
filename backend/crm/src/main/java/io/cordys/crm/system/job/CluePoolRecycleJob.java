package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.clue.service.ClueOwnerHistoryService;
import io.cordys.crm.clue.service.CluePoolService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CluePoolRecycleJob {

	@Resource
	private BaseMapper<Clue> clueMapper;
	@Resource
	private BaseMapper<CluePool> cluePoolMapper;
	@Resource
	private ExtClueMapper extClueMapper;
	@Resource
	private BaseMapper<CluePoolRecycleRule> cluePoolRecycleRuleMapper;
	@Resource
	private CluePoolService cluePoolService;
	@Resource
	private ClueOwnerHistoryService clueOwnerHistoryService;

	/**
	 * 回收线索
	 */
	@QuartzScheduled(cron = "0 0 1 * * ?")
	public void recycle() {
		LogUtils.info("Start recycle clue resource");
		LambdaQueryWrapper<CluePool> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(CluePool::getEnable, true).eq(CluePool::getAuto, true);
		List<CluePool> pools = cluePoolMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isEmpty(pools)) {
			return;
		}
		Map<List<String>, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersBestMatchPoolMap(pools);
		List<String> recycleOwnersIds = ownersDefaultPoolMap.keySet().stream().flatMap(List::stream).toList();
		LambdaQueryWrapper<Clue> clueQueryWrapper = new LambdaQueryWrapper<>();
		clueQueryWrapper.in(Clue::getOwner, recycleOwnersIds).eq(Clue::getInSharedPool, false);
		List<Clue> clues = clueMapper.selectListByLambda(clueQueryWrapper);
		if (CollectionUtils.isEmpty(clues)) {
			return;
		}
		List<String> poolIds = pools.stream().map(CluePool::getId).toList();
		LambdaQueryWrapper<CluePoolRecycleRule> ruleQueryWrapper = new LambdaQueryWrapper<>();
		ruleQueryWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);
		List<CluePoolRecycleRule> recycleRules = cluePoolRecycleRuleMapper.selectListByLambda(ruleQueryWrapper);
		Map<String, CluePoolRecycleRule> recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, r -> r));
		clues.forEach(clue -> ownersDefaultPoolMap.forEach((ownerIds, pool) -> {
			if (ownerIds.contains(clue.getOwner())) {
				CluePoolRecycleRule rule = recycleRuleMap.get(pool.getId());
				boolean recycle = cluePoolService.checkRecycled(clue, rule);
				if (recycle) {
					// 插入责任人历史
					clueOwnerHistoryService.add(clue, InternalUser.ADMIN.getValue());
					clue.setPoolId(pool.getId());
					clue.setInSharedPool(true);
					clue.setOwner(null);
					clue.setCollectionTime(null);
					clue.setUpdateUser(InternalUser.ADMIN.getValue());
					clue.setUpdateTime(System.currentTimeMillis());
					// 回收线索至线索池
					extClueMapper.moveToPool(clue);
				}
			}
		}));
		LogUtils.info("Recycle clue resource done");
	}
}
