package cn.cordys.crm.system.service;

import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.mapper.ExtExportTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ExportTaskStopService {

    @Resource
    private ExtExportTaskMapper extExportTaskMapper;

    /**
     * 重启时将所有导出中的任务停止,并且需要设置为失败
      */
    public void stopPreparedAll() {
        extExportTaskMapper.updateExportTaskStatus(ExportConstants.ExportStatus.ERROR.name(),ExportConstants.ExportStatus.PREPARED.name());
    }
}
