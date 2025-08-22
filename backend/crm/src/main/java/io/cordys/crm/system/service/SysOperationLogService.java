package io.cordys.crm.system.service;

import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JsonDifferenceUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.OperationLog;
import io.cordys.crm.system.domain.OperationLogBlob;
import io.cordys.crm.system.dto.request.OperationLogRequest;
import io.cordys.crm.system.dto.response.OperationLogDetailResponse;
import io.cordys.crm.system.dto.response.OperationLogResponse;
import io.cordys.crm.system.mapper.ExtOperationLogMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
    @Resource
    private BaseService baseService;

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

            list.forEach(item -> item.setOperatorName(userMap.getOrDefault(item.getOperator(), StringUtils.EMPTY)));
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
    public OperationLogDetailResponse getLogDetail(String id, String orgId) {
        OperationLog operationLog = operationLogMapper.selectByPrimaryKey(id);
        OperationLogDetailResponse logResponse = BeanUtils.copyBean(new OperationLogDetailResponse(), operationLog);
        logResponse.setOperator(operationLog.getCreateUser());
        logResponse.setOperatorName(baseService.getUserName(logResponse.getOperator()));

        OperationLogBlob operationLogBlob = operationLogBlobMapper.selectByPrimaryKey(id);
        if (operationLogBlob == null) {
            return logResponse;
        }

        String oldString = new String(
                Optional.ofNullable(operationLogBlob.getOriginalValue()).orElse(new byte[0]),
                StandardCharsets.UTF_8
        );
        String newString = new String(
                Optional.ofNullable(operationLogBlob.getModifiedValue()).orElse(new byte[0]),
                StandardCharsets.UTF_8
        );

        try {
            List<JsonDifferenceDTO> differenceDTOS = new ArrayList<>();
            JsonDifferenceUtils.compareJson(oldString, newString, differenceDTOS);
            // 过滤掉组织ID等字段
            differenceDTOS = filterIgnoreFields(differenceDTOS);

            if (CollectionUtils.isNotEmpty(differenceDTOS)) {
                OperationLog log = operationLogMapper.selectByPrimaryKey(id);
                BaseModuleLogService moduleLogService = ModuleLogServiceFactory.getModuleLogService(log.getModule());
                if (moduleLogService != null) {
                    moduleLogService.handleLogField(differenceDTOS, orgId);
                } else {
                    differenceDTOS.forEach(BaseModuleLogService::translatorDifferInfo);
                }

            }
            logResponse.setDiffs(differenceDTOS);
        } catch (Exception e) {
            throw new GenericException(Translator.get("data_parsing_exception"));
        }

        return logResponse;
    }

    /**
     * 过滤掉日志对比无需显示的字段
     * 例如：organizationId
     *
     * @param differenceDTOS
     * @return
     */
    private List<JsonDifferenceDTO> filterIgnoreFields(List<JsonDifferenceDTO> differenceDTOS) {
        differenceDTOS = differenceDTOS.stream()
                .filter(differ -> {
                    return !Strings.CS.equalsAny(differ.getColumn(),
                            "organizationId", "createUser", "updateUser", "createTime", "updateTime", "departmentName", "supervisorName", "lastStage");
                }).toList();
        return differenceDTOS;
    }
}
