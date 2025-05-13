package io.cordys.crm.system.job.listener;

import io.cordys.common.util.LogUtils;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.service.OpportunityRuleService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 商机规则监听器
 * <p>
 * 此监听器负责监听定时执行事件，根据预设规则自动处理商机状态。
 * 当触发执行事件时，会根据启用的自动化规则检查所有相关商机，
 * 并根据规则条件自动关闭符合条件的商机。
 * </p>
 */
@Component
public class OpportunityRuleListener implements ApplicationListener<ExecuteEvent> {

    @Resource
    private BaseMapper<OpportunityRule> opportunityRuleMapper;

    @Resource
    private OpportunityRuleService opportunityRuleService;

    @Resource
    private BaseMapper<Opportunity> opportunityMapper;

    /**
     * 处理应用事件
     * <p>
     * 当接收到执行事件时，触发商机规则处理逻辑。
     * </p>
     *
     * @param event 执行事件
     */
    @Override
    public void onApplicationEvent(@NotNull ExecuteEvent event) {
        try {
            execute();
        } catch (Exception e) {
            LogUtils.error("商机资源回收异常: " , e.getMessage());
        }
    }

    /**
     * 执行商机关闭规则任务
     * <p>
     * 此方法执行以下步骤：
     * 1. 查询所有已启用且设置为自动执行的商机规则
     * 2. 获取规则对应的最佳匹配所有者映射关系
     * 3. 查询相关所有者的所有商机
     * 4. 根据规则检查每个商机是否需要关闭
     * 5. 自动关闭符合条件的商机
     * </p>
     */
    public void execute() {
        LogUtils.info("开始回收商机资源");

        // 查询已启用的自动执行规则
        LambdaQueryWrapper<OpportunityRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OpportunityRule::getEnable, true).eq(OpportunityRule::getAuto, true);
        List<OpportunityRule> rules = opportunityRuleMapper.selectListByLambda(queryWrapper);
        if (CollectionUtils.isEmpty(rules)) {
            LogUtils.info("没有启用的自动回收商机规则，回收任务结束");
            return;
        }

        // 获取所有者与规则的最佳匹配映射
        Map<List<String>, OpportunityRule> ownersBestMatchRuleMap = opportunityRuleService.getOwnersBestMatchRuleMap(rules);
        List<String> handleOwnerIds = ownersBestMatchRuleMap.keySet().stream().flatMap(List::stream).toList();

        // 查询相关所有者的商机
        LambdaQueryWrapper<Opportunity> opportunityWrapper = new LambdaQueryWrapper<>();
        opportunityWrapper.in(Opportunity::getOwner, handleOwnerIds);
        List<Opportunity> opportunities = opportunityMapper.selectListByLambda(opportunityWrapper);
        if (CollectionUtils.isEmpty(opportunities)) {
            LogUtils.info("没有需要回收的商机，回收任务结束");
            return;
        }

        // 根据规则处理每个商机
        opportunities.forEach(opportunity -> ownersBestMatchRuleMap.forEach((ownerIds, rule) -> {
            if (ownerIds.contains(opportunity.getOwner())) {
                boolean closed = opportunityRuleService.checkClosed(opportunity, rule);
                if (closed) {
                    opportunity.setStatus(false);
                    opportunityMapper.updateById(opportunity);
                }
            }
        }));

        LogUtils.info("商机资源回收完成");
    }
}