package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.service.OpportunityRuleService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpportunityRuleJob {

	@Resource
	private BaseMapper<OpportunityRule> opportunityRuleMapper;
	@Resource
	private OpportunityRuleService opportunityRuleService;
	@Resource
	private BaseMapper<Opportunity> opportunityMapper;

	/**
	 * 商机关闭规则任务
	 */
	@QuartzScheduled(cron = "0 0 1 * * ?")
	public void execute() {
		LogUtils.info("Start closed opportunity resource");
		LambdaQueryWrapper<OpportunityRule> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OpportunityRule::getEnable, true).eq(OpportunityRule::getAuto, true);
		List<OpportunityRule> rules = opportunityRuleMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isEmpty(rules)) {
			return;
		}
		Map<List<String>, OpportunityRule> ownersBestMatchRuleMap = opportunityRuleService.getOwnersBestMatchRuleMap(rules);
		List<String> handleOwnerIds = ownersBestMatchRuleMap.keySet().stream().flatMap(List::stream).toList();
		LambdaQueryWrapper<Opportunity> opportunityWrapper = new LambdaQueryWrapper<>();
		opportunityWrapper.in(Opportunity::getOwner, handleOwnerIds);
		List<Opportunity> opportunities = opportunityMapper.selectListByLambda(opportunityWrapper);
		if (CollectionUtils.isEmpty(opportunities)) {
			return;
		}
		opportunities.forEach(opportunity -> ownersBestMatchRuleMap.forEach((ownerIds, rule) -> {
			if (ownerIds.contains(opportunity.getOwner())) {
				boolean closed = opportunityRuleService.checkClosed(opportunity, rule);
				if (closed) {
					opportunity.setStatus(false);
					opportunityMapper.updateById(opportunity);
				}
			}
		}));
		LogUtils.info("Closed opportunity resource done");
	}
}
