package io.cordys.crm.system.service;

import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.JSON;
import io.cordys.common.util.JsonDifferenceUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.OperationLog;
import io.cordys.crm.system.domain.OperationLogBlob;
import io.cordys.crm.system.dto.request.OperationLogRequest;
import io.cordys.crm.system.dto.response.OperationLogResponse;
import io.cordys.crm.system.mapper.ExtOperationLogMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class SysOperationLogService {

    @Resource
    private ExtOperationLogMapper extOperationLogMapper;

    @Resource
    private ExtUserMapper extUserMapper;

    @Resource
    private BaseMapper<OperationLogBlob> operationLogBlobMapper;
    @Resource
    private BaseMapper<OperationLog> operationLogMapper;

    /**
     * 操作日志列表查询
     */
    public List<OperationLogResponse> list(OperationLogRequest request, String orgId) {
        checkTime(request.getStartTime(), request.getEndTime());
        List<OperationLogResponse> list = extOperationLogMapper.list(request, orgId);
        handleData(list);
        return list;
    }

    /**
     * 处理数据
     *
     * @param list 操作日志列表
     */
    private void handleData(List<OperationLogResponse> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            List<String> userIds = list.stream()
                    .map(OperationLogResponse::getOperator)
                    .collect(Collectors.toList());

            List<OptionDTO> userList = extUserMapper.selectUserOptionByIds(userIds);
            Map<String, String> userMap = userList.stream()
                    .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            list.forEach(item -> item.setOperator(userMap.getOrDefault(item.getOperator(), StringUtils.EMPTY)));
        }
    }

    /**
     * 时间校验
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    private void checkTime(Long startTime, Long endTime) {
        if (startTime > endTime) {
            throw new GenericException(Translator.get("startTime_must_be_less_than_endTime"));
        }
    }

    /**
     * 获取日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    public List<JsonDifferenceDTO> getLogDetail(String id, String orgId) {
        List<JsonDifferenceDTO> differenceDTOS = new ArrayList<>();
        Optional.ofNullable(operationLogBlobMapper.selectByPrimaryKey(id))
                .ifPresent(operationLog -> {
                    String oldString = new String(
                            Optional.ofNullable(operationLog.getOriginalValue()).orElse(new byte[0]),
                            StandardCharsets.UTF_8
                    );
                    String newString = new String(
                            Optional.ofNullable(operationLog.getModifiedValue()).orElse(new byte[0]),
                            StandardCharsets.UTF_8
                    );

                    try {
                        JsonDifferenceUtils.compareJson(oldString, newString, differenceDTOS);
                        if (CollectionUtils.isNotEmpty(differenceDTOS)) {
                            OperationLog log = operationLogMapper.selectByPrimaryKey(id);
                            BaseModuleLogService moduleLogService = ModuleLogServiceFactory.getModuleLogService(log.getModule());
                            if (moduleLogService != null) {
                                moduleLogService.handleLogField(differenceDTOS, orgId);
                            } else {
                                differenceDTOS.forEach(differ -> {
                                    differ.setColumnName(Translator.get("log_" + differ.getColumn()));
                                    differ.setOldValueName(JSON.parseObject(differ.getOldValue()));
                                    differ.setNewValueName(JSON.parseObject(differ.getNewValue()));
                                });
                            }

                        }
                    } catch (Exception e) {
                        throw new GenericException(Translator.get("data_parsing_exception"));
                    }
                });

        return differenceDTOS;
    }
}
