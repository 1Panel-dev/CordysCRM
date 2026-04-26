package cn.cordys.crm.opportunity.service;

import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.opportunity.domain.OpportunityStageHistory;
import cn.cordys.crm.opportunity.dto.response.OpportunityStageHistoryResponse;
import cn.cordys.crm.opportunity.dto.response.StageConfigResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.domain.DictConfig;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityStageHistoryService {
    @Resource
    private BaseMapper<OpportunityStageHistory> opportunityStageHistoryMapper;
    @Resource
    private BaseMapper<DictConfig> dictConfigMapper;
    @Resource
    private BaseMapper<Dict> dictMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ExtOpportunityStageConfigMapper extOpportunityStageConfigMapper;


    public List<OpportunityStageHistoryResponse> list(String opportunityId, String orgId) {
        LambdaQueryWrapper<OpportunityStageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityStageHistory::getOpportunityId, opportunityId);
        wrapper.orderByDesc(OpportunityStageHistory::getChangeTime);
        List<OpportunityStageHistory> historyList = opportunityStageHistoryMapper.selectListByLambda(wrapper);
        return buildListData(orgId, historyList);
    }

    private List<OpportunityStageHistoryResponse> buildListData(String orgId, List<OpportunityStageHistory> historyList) {
        if (CollectionUtils.isEmpty(historyList)) {
            return List.of();
        }

        Set<String> operatorIds = new HashSet<>();
        Set<String> stageIds = new HashSet<>();
        Set<String> reasonIds = new HashSet<>();

        for (OpportunityStageHistory history : historyList) {
            operatorIds.add(history.getOperator());
            stageIds.add(history.getFromStage());
            stageIds.add(history.getToStage());
            if (StringUtils.isNotBlank(history.getFailureReasonId())) {
                reasonIds.add(history.getFailureReasonId());
            }
        }

        Map<String, String> operatorNameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        operatorNameMap.putAll(baseService.getUserNameMap(operatorIds));

        List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        Map<String, String> stageNameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (StageConfigResponse config : stageConfigList) {
            stageNameMap.put(config.getId(), config.getName());
        }

        Map<String, String> reasonNameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (!reasonIds.isEmpty()) {
            List<Dict> dictList = dictMapper.selectByIds(new ArrayList<>(reasonIds));
            for (Dict dict : dictList) {
                reasonNameMap.put(dict.getId(), dict.getName());
            }
        }

        return historyList
                .stream()
                .map(item -> {
                    OpportunityStageHistoryResponse response =
                            BeanUtils.copyBean(new OpportunityStageHistoryResponse(), item);
                    response.setOperatorName(getOperatorNameOrDefault(operatorNameMap, item.getOperator()));
                    response.setFromStageName(getStageNameOrDefault(stageNameMap, item.getFromStage()));
                    response.setToStageName(getStageNameOrDefault(stageNameMap, item.getToStage()));
                    if (StringUtils.isNotBlank(item.getFailureReasonId())) {
                        response.setFailureReasonName(getNameIgnoreCase(reasonNameMap, item.getFailureReasonId()));
                    }
                    return response;
                }).toList();
    }

    private String getNameIgnoreCase(Map<String, String> nameMap, String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return nameMap.get(key);
    }

    private String getStageNameOrDefault(Map<String, String> stageNameMap, String stageId) {
        if (StringUtils.isBlank(stageId)) {
            return "未知阶段";
        }
        String name = stageNameMap.get(stageId);
        return StringUtils.isNotBlank(name) ? name : "未知阶段";
    }

    private String getOperatorNameOrDefault(Map<String, String> operatorNameMap, String operatorId) {
        if (StringUtils.isBlank(operatorId)) {
            return "未知用户";
        }
        String name = operatorNameMap.get(operatorId);
        return StringUtils.isNotBlank(name) ? name : "未知用户";
    }

    public void add(String opportunityId, String fromStage, String toStage, String operatorId, String failureReasonId) {
        OpportunityStageHistory history = new OpportunityStageHistory();
        history.setId(IDGenerator.nextStr());
        history.setOpportunityId(opportunityId);
        history.setFromStage(fromStage);
        history.setToStage(toStage);
        history.setChangeTime(System.currentTimeMillis());
        history.setOperator(operatorId);
        history.setFailureReasonId(failureReasonId);
        opportunityStageHistoryMapper.insert(history);
    }

    public void deleteByOpportunityId(String opportunityId) {
        LambdaQueryWrapper<OpportunityStageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityStageHistory::getOpportunityId, opportunityId);
        opportunityStageHistoryMapper.deleteByLambda(wrapper);
    }

    public void deleteByOpportunityIds(List<String> opportunityIds) {
        if (CollectionUtils.isEmpty(opportunityIds)) {
            return;
        }
        LambdaQueryWrapper<OpportunityStageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityStageHistory::getOpportunityId, opportunityIds);
        opportunityStageHistoryMapper.deleteByLambda(wrapper);
    }
}
