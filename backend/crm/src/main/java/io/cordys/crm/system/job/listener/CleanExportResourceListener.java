package io.cordys.crm.system.job.listener;

import io.cordys.crm.system.service.ExportTaskCenterService;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author song-cc-rock
 */
@Component
public class CleanExportResourceListener implements ApplicationListener<ExecuteEvent> {

	@Resource
	private ExportTaskCenterService exportResourceService;


	@Override
	public void onApplicationEvent(@NotNull ExecuteEvent event) {
		exportResourceService.clean();
	}
}
