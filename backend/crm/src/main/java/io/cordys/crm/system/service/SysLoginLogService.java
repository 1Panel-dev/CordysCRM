package io.cordys.crm.system.service;


import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.dto.request.LoginLogRequest;
import io.cordys.crm.system.dto.response.LoginLogListResponse;
import io.cordys.crm.system.mapper.ExtLoginLogMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianxing
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SysLoginLogService {
    @Resource
    private ExtLoginLogMapper extLoginLogMapper;
    @Resource
    private BaseService baseService;

    /**
     * 处理数据
     *
     * @param list
     */
    private List<LoginLogListResponse> handleData(List<LoginLogListResponse> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            List<String> userIds = list.stream().map(LoginLogListResponse::getOperator).collect(Collectors.toList());
            Map<String, String> userNameMap = baseService.getUserNameMap(userIds);
            list.forEach(item -> item.setOperatorName(userNameMap.getOrDefault(item.getOperator(), StringUtils.EMPTY)));
        }
        return list;
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
    public List<LoginLogListResponse> list(LoginLogRequest request, String orgId) {
        checkTime(request.getStartTime(), request.getEndTime());
        List<LoginLogListResponse> list = extLoginLogMapper.list(request, orgId);
        return handleData(list);
    }
}