package io.cordys.crm.system.service;


import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.JsonDifferenceUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.OperationLogBlob;
import io.cordys.crm.system.dto.request.OperationLogRequest;
import io.cordys.crm.system.dto.response.OperationLogResponse;
import io.cordys.crm.system.mapper.ExtOperationLogMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
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


    /**
     * 操作日志列表查询
     */
    public List<OperationLogResponse> list(OperationLogRequest request) {
        checkTime(request.getStartTime(), request.getEndTime());
        List<OperationLogResponse> list = extOperationLogMapper.list(request);
        handleData(list);
        return list;
    }

    /**
     * 处理数据
     *
     * @param list
     */
    private void handleData(List<OperationLogResponse> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            List<String> userIds = list.stream().map(OperationLogResponse::getOperator).collect(Collectors.toList());
            List<OptionDTO> userList = extUserMapper.selectUserOptionByIds(userIds);
            Map<String, String> userMap = userList.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            list.forEach(item -> {
                item.setOperator(userMap.getOrDefault(item.getOperator(), StringUtils.EMPTY));
            });
        }
    }

    /**
     * 时间校验
     *
     * @param startTime
     * @param endTime
     */
    private void checkTime(Long startTime, Long endTime) {
        int compare = Long.compare(startTime, endTime);
        if (compare > 0) {
            throw new GenericException(Translator.get("startTime_must_be_less_than_endTime"));
        }
    }


    /**
     * 登录日志列表查询
     *
     * @param request
     * @return
     */
    public List<OperationLogResponse> loginList(OperationLogRequest request) {
        checkTime(request.getStartTime(), request.getEndTime());
        List<OperationLogResponse> list = extOperationLogMapper.loginList(request);
        handleData(list);
        return list;
    }

    public List<JsonDifferenceDTO> getLogDetail(String id) {
        List<JsonDifferenceDTO> differenceDTOS = new ArrayList<>();
        OperationLogBlob operationLogBlob = operationLogBlobMapper.selectByPrimaryKey(id);
        Optional.ofNullable(operationLogBlob).ifPresent(operationLog -> {
            String oldString = new String(operationLog.getOriginalValue() == null ? new byte[0] : operationLog.getOriginalValue(), StandardCharsets.UTF_8);
            String newString = new String(operationLog.getModifiedValue() == null ? new byte[0] : operationLog.getModifiedValue(), StandardCharsets.UTF_8);
            try {
                JsonDifferenceUtils.compareJson(oldString, newString, differenceDTOS);
            } catch (Exception e) {
                throw new GenericException(Translator.get("data_parsing_exception"));
            }
        });
        return differenceDTOS;
    }
}