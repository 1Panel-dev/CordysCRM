package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.crm.clue.domain.CluePool;
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
	private BaseMapper<CluePool> cluePoolMapper;

	/**
	 * 回收线索 每天凌晨一点执行
	 */
	@QuartzScheduled(cron = "0 0 1 * * ?")
	public void recycle() {
		LambdaQueryWrapper<CluePool> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(CluePool::getEnable, true).eq(CluePool::getAuto, true);
		List<CluePool> cluePools = cluePoolMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isNotEmpty(cluePools)) {
			Map<String, List<CluePool>> poolOrgGroups = cluePools.stream().collect(Collectors.groupingBy(CluePool::getOrganizationId));
			poolOrgGroups.forEach((orgId, pools) -> {
				// TODO: recycle pool clue

			});
		}
	}
}
