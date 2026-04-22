package cn.cordys.crm.system.job.listener;

import cn.cordys.crm.system.notice.CommonNoticeSendService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Map;

/**
 * 公海/线索池回收监听器抽象基类
 * <p>
 * 封装了"启用自动回收的池 → 查询负责人 → 查询数据 → 按规则检查 → 回收"的通用流程。
 * 子类只需实现数据查询和回收操作的具体逻辑。
 * </p>
 *
 * @param <P> 池类型（CustomerPool / CluePool）
 * @param <E> 实体类型（Customer / Clue）
 * @param <R> 回收规则类型（CustomerPoolRecycleRule / CluePoolRecycleRule）
 */
@Slf4j
public abstract class AbstractPoolRecycleListener<P, E, R> implements ApplicationListener<ExecuteEvent> {

    @Override
    public void onApplicationEvent(ExecuteEvent event) {
        try {
            recycle();
        } catch (Exception e) {
            log.error("定时回收异常", e);
        }
    }

    /**
     * 执行回收流程
     */
    public void recycle() {
        log.info("开始回收 {}", getEntityDescription());

        List<P> enabledPools = getEnabledPools();
        if (CollectionUtils.isEmpty(enabledPools)) {
            log.info("没有启用的自动回收 {} 池，回收结束", getEntityDescription());
            return;
        }

        Map<List<String>, P> ownersPoolMap = getOwnersBestMatchPoolMap(enabledPools);
        List<String> allOwnerIds = ownersPoolMap.keySet().stream().flatMap(List::stream).toList();

        List<E> entities = getEntitiesForRecycle(allOwnerIds);
        if (CollectionUtils.isEmpty(entities)) {
            log.info("没有需要回收的 {}，回收结束", getEntityDescription());
            return;
        }

        Map<String, R> recycleRuleMap = getRecycleRules(enabledPools);
        processRecycle(entities, ownersPoolMap, recycleRuleMap);

        log.info("{} 回收完成", getEntityDescription());
    }

    /**
     * 获取已启用且设置为自动回收的池列表
     */
    protected abstract List<P> getEnabledPools();

    /**
     * 获取负责人与池的最佳匹配映射
     */
    protected abstract Map<List<String>, P> getOwnersBestMatchPoolMap(List<P> pools);

    /**
     * 获取需要检查回收的实体列表
     */
    protected abstract List<E> getEntitiesForRecycle(List<String> ownerIds);

    /**
     * 获取池 ID 到回收规则的映射
     */
    protected abstract Map<String, R> getRecycleRules(List<P> pools);

    /**
     * 根据规则检查实体是否需要回收
     */
    protected abstract boolean checkRecycled(E entity, R rule);

    /**
     * 执行单个实体的回收操作
     */
    protected abstract void processEntityRecycle(E entity, P pool);

    /**
     * 获取实体中文描述（用于日志）
     */
    protected abstract String getEntityDescription();

    /**
     * 获取通知服务（子类调用）
     */
    protected abstract CommonNoticeSendService getNoticeSendService();

    private void processRecycle(List<E> entities, Map<List<String>, P> ownersPoolMap,
                                Map<String, R> recycleRuleMap) {
        for (E entity : entities) {
            for (Map.Entry<List<String>, P> entry : ownersPoolMap.entrySet()) {
                List<String> ownerIds = entry.getKey();
                P pool = entry.getValue();
                if (ownerIds.contains(getOwner(entity))) {
                    String poolId = getPoolId(pool);
                    R rule = recycleRuleMap.get(poolId);
                    if (rule != null && checkRecycled(entity, rule)) {
                        processEntityRecycle(entity, pool);
                    }
                }
            }
        }
    }

    /**
     * 获取实体的负责人 ID
     */
    protected abstract String getOwner(E entity);

    /**
     * 获取池的 ID
     */
    protected abstract String getPoolId(P pool);
}
