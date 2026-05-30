package cn.cordys.crm.system.job.listener;

import cn.cordys.crm.system.service.ExportTaskCenterService;
import jakarta.annotation.Resource;
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
    public void onApplicationEvent(ExecuteEvent event) {
        exportResourceService.clean();
    }
}
