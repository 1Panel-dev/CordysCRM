package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LeadPoolRecycleJob {

	@Resource
	private BaseMapper<LeadPool> leadPoolMapper;

	/**
	 * 回收线索 每天凌晨一点执行
	 */
	@QuartzScheduled(cron = "0 0 1 * * ?")
	public void recycle() {
		LambdaQueryWrapper<LeadPool> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(LeadPool::getEnable, true).eq(LeadPool::getAuto, true);
		List<LeadPool> leadPools = leadPoolMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isNotEmpty(leadPools)) {
			Map<String, List<LeadPool>> poolOrgGroups = leadPools.stream().collect(Collectors.groupingBy(LeadPool::getOrganizationId));
			poolOrgGroups.forEach((orgId, pools) -> {
				// TODO: recycle pool lead

			});
		}
	}
}
