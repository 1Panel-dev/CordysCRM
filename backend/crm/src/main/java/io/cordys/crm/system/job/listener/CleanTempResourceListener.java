package io.cordys.crm.system.job.listener;

import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.service.FileCommonService;
import io.cordys.file.engine.DefaultRepositoryDir;
import io.cordys.file.engine.FileRequest;
import io.cordys.file.engine.StorageType;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 临时资源清理监听器
 * 负责监听执行事件并清理系统临时目录中的资源文件
 */
@Component
public class CleanTempResourceListener implements ApplicationListener<ExecuteEvent> {

    @Resource
    private FileCommonService fileCommonService;

    @Override
    public void onApplicationEvent(@NotNull ExecuteEvent event) {
        clean();
    }

    /**
     * 清理临时图片资源
     * 删除系统临时目录中的所有资源文件
     */
    private void clean() {
        LogUtils.info("开始清理临时目录资源");
        try {
            FileRequest request = new FileRequest(DefaultRepositoryDir.getTmpDir(), StorageType.LOCAL.name(), null);
            fileCommonService.cleanTempResource(request);
            LogUtils.info("临时目录资源清理完成");
        } catch (Exception e) {
            LogUtils.error("临时目录资源清理异常", e);
        }
    }
}