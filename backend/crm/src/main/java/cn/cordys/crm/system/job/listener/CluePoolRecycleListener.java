package cn.cordys.crm.system.job.listener;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.domain.CluePoolRecycleRule;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.ClueOwnerHistoryService;
import cn.cordys.crm.clue.service.CluePoolService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 线索池回收监听器
 */
@Component
@Slf4j
public class CluePoolRecycleListener extends AbstractPoolRecycleListener<CluePool, Clue, CluePoolRecycleRule> {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<CluePoolRecycleRule> cluePoolRecycleRuleMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    @Override
    protected List<CluePool> getEnabledPools() {
        LambdaQueryWrapper<CluePool> qw = new LambdaQueryWrapper<CluePool>()
                .eq(CluePool::getEnable, true)
                .eq(CluePool::getAuto, true);
        return cluePoolMapper.selectListByLambda(qw);
    }

    @Override
    protected Map<List<String>, CluePool> getOwnersBestMatchPoolMap(List<CluePool> pools) {
        return cluePoolService.getOwnersBestMatchPoolMap(pools);
    }

    @Override
    protected List<Clue> getEntitiesForRecycle(List<String> ownerIds) {
        LambdaQueryWrapper<Clue> qw = new LambdaQueryWrapper<Clue>()
                .in(Clue::getOwner, ownerIds)
                .eq(Clue::getInSharedPool, false);
        return clueMapper.selectListByLambda(qw);
    }

    @Override
    protected Map<String, CluePoolRecycleRule> getRecycleRules(List<CluePool> pools) {
        List<String> poolIds = pools.stream().map(CluePool::getId).toList();
        LambdaQueryWrapper<CluePoolRecycleRule> qw = new LambdaQueryWrapper<CluePoolRecycleRule>()
                .in(CluePoolRecycleRule::getPoolId, poolIds);
        return cluePoolRecycleRuleMapper.selectListByLambda(qw).stream()
                .collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, r -> r));
    }

    @Override
    protected boolean checkRecycled(Clue clue, CluePoolRecycleRule rule) {
        return StringUtils.isBlank(clue.getTransitionId()) && cluePoolService.checkRecycled(clue, rule);
    }

    @Override
    protected void processEntityRecycle(Clue clue, CluePool pool) {
        commonNoticeSendService.sendNotice(
                NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_AUTOMATIC_MOVE_POOL,
                clue.getName(),
                InternalUser.ADMIN.getValue(),
                clue.getOrganizationId(),
                List.of(clue.getOwner()),
                true
        );

        clueOwnerHistoryService.add(clue, InternalUser.ADMIN.getValue(), false);

        clue.setPoolId(pool.getId());
        clue.setInSharedPool(true);
        clue.setOwner(null);
        clue.setCollectionTime(null);
        clue.setReasonId("system");
        clue.setUpdateUser(InternalUser.ADMIN.getValue());
        clue.setUpdateTime(System.currentTimeMillis());

        extClueMapper.moveToPool(clue);
    }

    @Override
    protected String getEntityDescription() {
        return "线索";
    }

    @Override
    protected CommonNoticeSendService getNoticeSendService() {
        return commonNoticeSendService;
    }

    @Override
    protected String getOwner(Clue clue) {
        return clue.getOwner();
    }

    @Override
    protected String getPoolId(CluePool pool) {
        return pool.getId();
    }
}
