package io.cordys.aspectj.handler;

import io.cordys.aspectj.dto.LogDTO;

public interface OperationLogHandler {
    /**
     * 处理日志
     *
     * @param operationLog 操作日志
     */
    void handleLog(LogDTO operationLog);

}
