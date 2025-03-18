package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import org.springframework.stereotype.Component;

@Component
public class CluePoolRecycleJob {

	/**
	 * 回收线索 每天凌晨一点执行
	 */
	@QuartzScheduled(cron = "0 0 1 * * ?")
	public void recycle() {
		// TODO: Recycle Clue
	}
}
