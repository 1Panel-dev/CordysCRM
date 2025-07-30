package io.cordys.crm.system.job.listener;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.clue.service.ClueOwnerHistoryService;
import io.cordys.crm.clue.service.CluePoolService;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 线索池回收监听器
 * 负责监听执行事件并将符合条件的线索回收到线索池
 */
@Component
public class CluePoolRecycleListener implements ApplicationListener<ExecuteEvent> {

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
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    @Override
    public void onApplicationEvent(@NotNull ExecuteEvent event) {
        try {
            recycle();
        } catch (Exception e) {
            LogUtils.error("定时回收线索池异常：", e.getMessage());
        }
    }

    /**
     * 回收线索到线索池
     */
    public void recycle() {
        LogUtils.info("开始回收线索资源");

        // 获取启用的自动回收线索池
        List<CluePool> enabledPools = getEnabledPools();
        if (CollectionUtils.isEmpty(enabledPools)) {
            LogUtils.info("没有启用的自动回收线索池，回收任务结束");
            return;
        }

        // 获取所有者到线索池的最佳匹配映射
        Map<List<String>, CluePool> ownersPoolMap = cluePoolService.getOwnersBestMatchPoolMap(enabledPools);
        List<String> recycleOwnersIds = ownersPoolMap.keySet().stream()
                .flatMap(List::stream)
                .toList();

        // 获取需要检查的线索列表
        List<Clue> clues = getCluesForRecycle(recycleOwnersIds);
        if (CollectionUtils.isEmpty(clues)) {
            LogUtils.info("没有需要回收的线索，回收任务结束");
            return;
        }

        // 获取线索池回收规则
        Map<String, CluePoolRecycleRule> recycleRuleMap = getRecycleRules(enabledPools);

        // 处理每个线索的回收
        processCluesRecycle(clues, ownersPoolMap, recycleRuleMap);

        LogUtils.info("线索资源回收完成");
    }

    /**
     * 获取启用的自动回收线索池
     */
    private List<CluePool> getEnabledPools() {
        LambdaQueryWrapper<CluePool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CluePool::getEnable, true)
                .eq(CluePool::getAuto, true);
        return cluePoolMapper.selectListByLambda(queryWrapper);
    }

    /**
     * 获取需要检查回收的线索
     */
    private List<Clue> getCluesForRecycle(List<String> ownerIds) {
        LambdaQueryWrapper<Clue> clueQueryWrapper = new LambdaQueryWrapper<>();
        clueQueryWrapper.in(Clue::getOwner, ownerIds)
                .eq(Clue::getInSharedPool, false);
        return clueMapper.selectListByLambda(clueQueryWrapper);
    }

    /**
     * 获取线索池回收规则
     */
    private Map<String, CluePoolRecycleRule> getRecycleRules(List<CluePool> pools) {
        List<String> poolIds = pools.stream()
                .map(CluePool::getId)
                .toList();
        LambdaQueryWrapper<CluePoolRecycleRule> ruleQueryWrapper = new LambdaQueryWrapper<>();
        ruleQueryWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);
        List<CluePoolRecycleRule> recycleRules = cluePoolRecycleRuleMapper.selectListByLambda(ruleQueryWrapper);
        return recycleRules.stream()
                .collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, rule -> rule));
    }

    /**
     * 处理线索回收
     */
    private void processCluesRecycle(List<Clue> clues, Map<List<String>, CluePool> ownersPoolMap,
                                     Map<String, CluePoolRecycleRule> recycleRuleMap) {
        clues.forEach(clue ->
                ownersPoolMap.forEach((ownerIds, pool) -> {
                    if (ownerIds.contains(clue.getOwner())) {
                        CluePoolRecycleRule rule = recycleRuleMap.get(pool.getId());
                        if (rule != null && cluePoolService.checkRecycled(clue, rule)) {
                            recycleClueToPool(clue, pool);
                        }
                    }
                })
        );
    }

    /**
     * 将线索回收到线索池
     */
    private void recycleClueToPool(Clue clue, CluePool pool) {
        // 发送消息通知
        commonNoticeSendService.sendNotice(
                NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_AUTOMATIC_MOVE_POOL,
                clue.getName(),
                InternalUser.ADMIN.getValue(),
                clue.getOrganizationId(),
                List.of(clue.getOwner()),
                true
        );

        // 记录责任人历史
        clueOwnerHistoryService.add(clue, InternalUser.ADMIN.getValue());

        // 更新线索数据
        clue.setPoolId(pool.getId());
        clue.setInSharedPool(true);
        clue.setOwner(null);
        clue.setCollectionTime(null);
        clue.setReasonId("system");
        clue.setUpdateUser(InternalUser.ADMIN.getValue());
        clue.setUpdateTime(System.currentTimeMillis());

        // 回收线索至线索池
        extClueMapper.moveToPool(clue);
    }
}