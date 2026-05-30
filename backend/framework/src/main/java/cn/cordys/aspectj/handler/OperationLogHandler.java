package cn.cordys.aspectj.handler;

import cn.cordys.aspectj.dto.LogDTO;

public interface OperationLogHandler {
    /**
     * 处理日志
     *
     * @param operationLog 操作日志
     */
    void handleLog(LogDTO operationLog);

}
