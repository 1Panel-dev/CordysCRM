package io.cordys.aspectj.handler;

import io.cordys.aspectj.builder.OperationLog;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.aspectj.dto.LogExtraDTO;
import io.cordys.common.util.JSON;
import io.cordys.common.util.ServletUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
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


    public static void fillModuleFields(LogDTO reqDTO, OperationLog operationLog) {
        reqDTO.setCreateTime(System.currentTimeMillis());
        reqDTO.setType(operationLog.getType()); // 大模块类型，例如：CRM 客户
        reqDTO.setCreateUser(operationLog.getOperator());
        reqDTO.setModule(operationLog.getSubType());// 操作类型：CURD
        reqDTO.setSourceId(operationLog.getResourceId()); // 资源id
        reqDTO.setContent(operationLog.getAction());// 操作内容，例如：修改编号为 1 的用户信息，将性别从男改成女
        reqDTO.setLoginAddress(operationLog.getLoginAddress());// 登录地址
        reqDTO.setPlatform(operationLog.getPlatform());// 平台

        // 变更原始内容
        if (StringUtils.isNotBlank(operationLog.getExtra())) {
            reqDTO.setExtra(JSON.parseObject(operationLog.getExtra(), LogExtraDTO.class));
        }
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
}