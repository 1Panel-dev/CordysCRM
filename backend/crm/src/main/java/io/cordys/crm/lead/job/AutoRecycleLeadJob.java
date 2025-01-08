package io.cordys.crm.lead.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.util.LogUtils;
import org.springframework.stereotype.Component;

@Component
public class AutoRecycleLeadJob {

	@QuartzScheduled(cron = "0 0 0 * * ?")
	public void recycle() {
		/*
		 * TODO
		 * 所属组织架构下的线索池且开启自动回收规则, 按照创建时间顺序回收 (规则: 根据回收条件, 筛选满足条件的线索{已分配, 待回收})
		 */
		LogUtils.info("recycle leads start");
		LogUtils.info("recycle leads end");
	}
}
