package cn.cordys.crm.system.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.aspectj.handler.OperationLogHandler;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.OperationLog;
import cn.cordys.crm.system.domain.OperationLogBlob;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志服务类
 * 提供单条和批量操作日志的存储方法。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LogService implements OperationLogHandler {

    @Resource
    private BaseMapper<OperationLog> operationLogMapper;

    @Resource
    private BaseMapper<OperationLogBlob> operationLogBlobMapper;

    /**
     * 根据 LogDTO 创建一个 OperationLogBlob 实体对象。
     *
     * @param log 日志数据传输对象
     *
     * @return OperationLogBlob
     */
    private OperationLogBlob getBlob(LogDTO log) {
        OperationLogBlob blob = new OperationLogBlob();
        blob.setId(log.getId());

        LogContextInfo extra = OperationLogContext.getContext();

        if (extra != null) {
            if (ObjectUtils.isNotEmpty(extra.getOriginalValue())) {
                blob.setOriginalValue(JSON.toJSONBytes(extra.getOriginalValue()));
            }
            if (ObjectUtils.isNotEmpty(extra.getModifiedValue())) {
                blob.setModifiedValue(JSON.toJSONBytes(extra.getModifiedValue()));
            }
        }

        if (ObjectUtils.isNotEmpty(log.getOriginalValue())) {
            blob.setOriginalValue(JSON.toJSONBytes(log.getOriginalValue()));
        }
        if (ObjectUtils.isNotEmpty(log.getModifiedValue())) {
            blob.setModifiedValue(JSON.toJSONBytes(log.getModifiedValue()));
        }

        return blob;
    }

    /**
     * 截断日志内容，确保其不超过500个字符。
     *
     * @param content 日志内容
     *
     * @return 截取后的日志内容
     */
    private String subStrContent(String content) {
        if (StringUtils.isNotBlank(content) && content.length() > 500) {
            return content.substring(0, 499);
        }
        return content;
    }

    private String subStrResourceName(String name) {
        if (StringUtils.isNotBlank(name) && name.length() > 255) {
            return name.substring(0, 255);
        }
        return name;
    }

    /**
     * 添加单条操作日志
     *
     * @param log 日志数据传输对象
     */
    @Async
    public void add(LogDTO log) {
        if (StringUtils.isBlank(log.getOrganizationId())) {
            log.setOrganizationId(OrganizationContext.getOrganizationId());
        }

        // 如果创建用户为空，设置为“admin”
        if (StringUtils.isBlank(log.getCreateUser())) {
            if (StringUtils.isBlank(SessionUtils.getUserId())) {
                log.setCreateUser(InternalUser.ADMIN.getValue());
            } else {
                log.setCreateUser(SessionUtils.getUserId());
            }
        }

        // 插入操作日志和日志Blob数据
        log.setId(IDGenerator.nextStr());

        LogContextInfo extra = OperationLogContext.getContext();

        if (extra != null) {
            // 如果注解中没有设置 sourceId ，则使用 extra 中的 resourceId
            if (StringUtils.isBlank(log.getResourceId()) && ObjectUtils.isNotEmpty(extra.getResourceId())) {
                log.setResourceId(extra.getResourceId());
            }

            // 如果注解中没有设置 resourceName ，则使用 extra 中的 resourceName
            if (StringUtils.isBlank(log.getResourceName()) && ObjectUtils.isNotEmpty(extra.getResourceName())) {
                log.setResourceName(subStrResourceName(extra.getResourceName()));
            }
        }

        // 截断日志内容
        log.setDetail(subStrContent(log.getDetail()));
        log.setMethod(StringUtils.defaultIfBlank(log.getMethod(), "GET"));
        OperationLog operationLog = BeanUtils.copyBean(new OperationLog(), log);
        operationLog.setResourceName(subStrResourceName(log.getResourceName()));
        operationLog.setDetail(log.getDetail());
        operationLogMapper.insert(operationLog);

        OperationLogBlob blob = getBlob(log);
        if (blob.getOriginalValue() != null || blob.getModifiedValue() != null) {
            operationLogBlobMapper.insert(blob);
        }
    }

    /**
     * 批量添加操作日志
     *
     * @param logs 日志数据传输对象列表
     */
    @Async
    public void batchAdd(List<LogDTO> logs) {
        // 如果日志列表为空，直接返回
        if (CollectionUtils.isEmpty(logs)) {
            return;
        }

        var currentTimeMillis = System.currentTimeMillis();
        List<OperationLog> items = new ArrayList<>();
        // 使用流处理，构建操作日志和Blob列表
        var blobs = logs.stream()
                .peek(log -> {
                    log.setId(IDGenerator.nextStr());
                    log.setResourceName(subStrResourceName(log.getResourceName()));
                    log.setDetail(subStrContent(log.getDetail()));
                    log.setCreateTime(currentTimeMillis);
                    OperationLog item = new OperationLog();
                    BeanUtils.copyBean(item, log);
                    item.setMethod(StringUtils.defaultIfBlank(log.getMethod(), "GET"));
                    items.add(item);
                })
                .map(this::getBlob)
                .filter(blob -> blob.getOriginalValue() != null || blob.getModifiedValue() != null)
                .toList();
        // 批量插入操作日志和日志Blob数据
        operationLogMapper.batchInsert(items);
        operationLogBlobMapper.batchInsert(blobs);
    }

    @Override
    public void handleLog(LogDTO operationLog) {
        add(operationLog);
    }
}
