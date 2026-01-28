package cn.cordys.crm.system.job.listener;

import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** 临时资源清理监听器 负责监听执行事件并清理系统临时目录中的资源文件 */
@Component
@Slf4j
public class CleanTempResourceListener implements ApplicationListener<ExecuteEvent> {

  @Resource private FileCommonService fileCommonService;

  @Override
  public void onApplicationEvent(ExecuteEvent event) {
    clean();
  }

  /** 清理临时图片资源 删除系统临时目录中的所有资源文件 */
  private void clean() {
    log.info("开始清理临时目录资源");
    try {
      FileRequest request =
          new FileRequest(DefaultRepositoryDir.getTmpDir(), StorageType.LOCAL.name(), null);
      fileCommonService.cleanTempResource(request);
      log.info("临时目录资源清理完成");
    } catch (Exception e) {
      log.error("临时目录资源清理异常", e);
    }
  }
}
