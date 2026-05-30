package cn.cordys.aspectj.handler;

import cn.cordys.aspectj.builder.OperationLog;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.util.ServletUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * 操作日志 ILogRecordService 实现类
 * <p>
 * 基于 {@link OperationLogHandler} 实现，记录操作日志
 */
@Service
public class OperationLogService {

    @Resource
    private OperationLogHandler operationLogHandler;

    public static void fillModuleFields(LogDTO reqDTO, OperationLog operationLog) {
        reqDTO.setCreateTime(System.currentTimeMillis());
        reqDTO.setType(operationLog.getType()); // 大模块类型，例如：CRM 客户
        reqDTO.setCreateUser(operationLog.getOperator());
        reqDTO.setModule(operationLog.getSubType());// 操作类型：CURD
        reqDTO.setResourceId(operationLog.getResourceId()); // 资源id
        reqDTO.setResourceName(operationLog.getResourceName()); // 资源名称
        reqDTO.setDetail(operationLog.getDetail()); // 资源名称
    }

    private static void fillRequestFields(LogDTO reqDTO) {
        // 获得 Request 对象
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        // 补全请求信息
        reqDTO.setMethod(request.getMethod());
        reqDTO.setPath(request.getRequestURI());
    }

    public void record(OperationLog operationLog) {
        // 1. 补全通用字段
        LogDTO reqDTO = new LogDTO();
        // 补全模块信息
        fillModuleFields(reqDTO, operationLog);
        // 补全请求信息
        fillRequestFields(reqDTO);

        // todo： 组织或项目信息

        // 2. 异步记录日志
        assert operationLogHandler != null;
        operationLogHandler.handleLog(reqDTO);
    }
}