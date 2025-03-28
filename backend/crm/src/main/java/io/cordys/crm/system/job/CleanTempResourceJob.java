package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.service.FileCommonService;
import io.cordys.file.engine.DefaultRepositoryDir;
import io.cordys.file.engine.FileRequest;
import io.cordys.file.engine.StorageType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CleanTempResourceJob {

	@Resource
	private FileCommonService fileCommonService;

	/**
	 * 清理临时图片资源
	 */
	@QuartzScheduled(cron = "0 0 2 * * ?")
	public void clean() {
		LogUtils.info("Start cleaning temp dir resource");
		FileRequest request = new FileRequest(DefaultRepositoryDir.getTmpDir(), StorageType.LOCAL.name(), null);
		fileCommonService.cleanTempResource(request);
		LogUtils.info("Temp dir resource cleanup completed");
	}
}
