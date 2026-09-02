package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.dto.stage.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.opportunity.constants.OpportunityStageType;
import cn.cordys.crm.opportunity.domain.OpportunityStageConfig;
import cn.cordys.crm.opportunity.dto.request.OpportunityStageAddRequest;
import cn.cordys.crm.opportunity.dto.request.StageUpdateRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityStageResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.system.domain.StageAdvancedConfig;
import cn.cordys.crm.system.mapper.ExtStageAdvancedConfigMapper;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityStageService {

    public static final Long DEFAULT_POS = 1L;
    @Resource
    private BaseMapper<OpportunityStageConfig> opportunityStageConfigMapper;
    @Resource
    private ExtOpportunityStageConfigMapper extOpportunityStageConfigMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtStageAdvancedConfigMapper extStageAdvancedConfigMapper;
    @Resource
    private UserViewService userViewService;

    /**
     * 商机阶段配置列表
     *
     * @param orgId
     * @return
     */
    public StageConfigsResponse getStageConfigList(String orgId) {
        StageConfigsResponse stageConfigListResponse = new StageConfigsResponse();
        List<OpportunityStageResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        List<StageAdvancedConfig> advancedConfigs = extStageAdvancedConfigMapper.selectConfigByType(orgId, FormKey.OPPORTUNITY.getKey());
        buildList(stageConfigList, stageConfigListResponse, advancedConfigs, orgId);
        return stageConfigListResponse;
    }

    private void buildList(List<OpportunityStageResponse> stageConfigList, StageConfigsResponse response, List<StageAdvancedConfig> advancedConfigs, String orgId) {
        response.setStageConfigList(stageConfigList);
        if (CollectionUtils.isNotEmpty(stageConfigList)) {
            var first = stageConfigList.getFirst();
            response.setEndRollBack(first.getEndRollBack());
            response.setAfootRollBack(first.getAfootRollBack());
            stageConfigList.forEach(sc -> sc.setStageHasData(extOpportunityMapper.countByStage(sc.getId()) > 0));
            response.setCirculationType(first.getCirculationType());
        }
        if (CollectionUtils.isNotEmpty(advancedConfigs)) {
            Map<String, List<StageAdvancedConfig>> originIdMaps = advancedConfigs.stream().collect(Collectors.groupingBy(StageAdvancedConfig::getOriginId));
            List<CirculationSetting> configs = new ArrayList<>();
            List<CirculationFieldValue> allConfigs = new ArrayList<>();
            originIdMaps.forEach((key, value) -> {
                CirculationSetting configResponse = new CirculationSetting();
                List<Target> targetList = new ArrayList<>();
                configResponse.setOriginId(key);
                value.forEach(item -> {
                    Target target = new Target();
                    target.setTargetId(item.getTargetId());
                    target.setEnable(item.getEnable());
                    List<CirculationFieldValue> circulationFieldValues = JSON.parseObject(item.getFieldConfig(), new TypeReference<List<CirculationFieldValue>>() {
                    });
                    target.setCirculationFieldValues(circulationFieldValues);
                    targetList.add(target);
                    allConfigs.addAll(circulationFieldValues);
                });
                configResponse.setTargets(targetList);
                configResponse.setModuleType(value.getFirst().getModuleType());
                configs.add(configResponse);
            });

            Map<String, CirculationSetting> configMap = configs.stream()
                    .collect(Collectors.toMap(
                            CirculationSetting::getOriginId,
                            Function.identity()
                    ));

            List<CirculationSetting> sortedConfigs = stageConfigList.stream()
                    .map(stage -> configMap.get(stage.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            response.setAdvancedConfigs(sortedConfigs);

            List<FilterCondition> combinedConditions = allConfigs.stream()
                    .map(config -> {
                        FilterCondition condition = new FilterCondition();
                        condition.setName(config.getFieldId());
                        condition.setValue(config.getFieldValue());
                        return condition;
                    })
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(combinedConditions)) {
                response.setOptionMap(userViewService.buildOptionMap(orgId, FormKey.OPPORTUNITY.getKey(), combinedConditions));
            }
        }
    }

    /**
     * 添加商机配置
     *
     * @param request
     * @param userId
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
    public String addStageConfig(OpportunityStageAddRequest request, String userId, String orgId) {
        checkConfigCount(orgId);
        Long pos = DEFAULT_POS;
        Boolean afootRollBack = true;
        Boolean endRollBack = false;
        //源节点
        OpportunityStageConfig target = opportunityStageConfigMapper.selectByPrimaryKey(request.getTargetId());
        if (target != null) {
            pos = target.getPos();
            //target正常不会为空
            if (request.getDropPosition() == -1) {
                extOpportunityStageConfigMapper.moveUpStageConfig(pos, orgId, DEFAULT_POS);
            } else {
                extOpportunityStageConfigMapper.moveDownStageConfig(pos, orgId, DEFAULT_POS);
                pos = pos + 1;
            }
            afootRollBack = target.getAfootRollBack();
            endRollBack = target.getEndRollBack();
        }

        OpportunityStageConfig stageConfig = new OpportunityStageConfig();
        stageConfig.setId(IDGenerator.nextStr());
        stageConfig.setName(request.getName());
        stageConfig.setType(request.getType());
        stageConfig.setRate(request.getRate());
        stageConfig.setAfootRollBack(afootRollBack);
        stageConfig.setEndRollBack(endRollBack);
        stageConfig.setPos(pos);
        stageConfig.setOrganizationId(orgId);
        stageConfig.setCreateUser(userId);
        stageConfig.setUpdateUser(userId);
        stageConfig.setCreateTime(System.currentTimeMillis());
        stageConfig.setUpdateTime(System.currentTimeMillis());
        opportunityStageConfigMapper.insert(stageConfig);

        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(stageConfig)
                .resourceId(stageConfig.getId())
                .resourceName(Translator.get("opportunity_stage_setting").concat(":").concat(request.getName()))
                .build());

        return stageConfig.getId();
    }


    /**
     * 商机阶段配置校验
     *
     * @param orgId
     */
    private void checkConfigCount(String orgId) {
        if (extOpportunityStageConfigMapper.countStageConfig(orgId) >= 15) {
            throw new GenericException(Translator.get("opportunity_stage_config_list"));
        }
    }


    /**
     * 删除
     *
     * @param id
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String orgId) {
        OpportunityStageConfig stageConfig = deletePreCheck(id, orgId);
        opportunityStageConfigMapper.deleteByPrimaryKey(id);
        // 设置操作对象
        OperationLogContext.setResourceName(Translator.get("opportunity_stage_setting").concat(":").concat(stageConfig.getName()));
    }

    private OpportunityStageConfig deletePreCheck(String id, String orgId) {
        OpportunityStageConfig stageConfig = opportunityStageConfigMapper.selectByPrimaryKey(id);
        if (stageConfig == null) {
            throw new GenericException(Translator.get("opportunity_stage_delete"));
        }

        if (extOpportunityStageConfigMapper.countByType(OpportunityStageType.AFOOT.name(), orgId) <= 1) {
            throw new GenericException(Translator.get("opportunity_stage_at_least_one"));
        }
        return stageConfig;
    }


    /**
     * 更新商机阶段回退设置
     *
     * @param request
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void updateRollBack(StageRollBackRequest request, String orgId) {
        LambdaQueryWrapper<OpportunityStageConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityStageConfig::getOrganizationId, orgId);
        List<OpportunityStageConfig> stageConfigList = opportunityStageConfigMapper.selectListByLambda(wrapper);
        extOpportunityStageConfigMapper.updateRollBack(request, orgId);

        Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("afootRollBack", Translator.get("log.enable.".concat(stageConfigList.getFirst().getAfootRollBack().toString())));
        originalVal.put("endRollBack", Translator.get("log.enable.".concat(stageConfigList.getFirst().getEndRollBack().toString())));
        Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("afootRollBack", Translator.get("log.enable.".concat(request.getAfootRollBack().toString())));
        modifiedVal.put("endRollBack", Translator.get("log.enable.".concat(request.getEndRollBack().toString())));
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(originalVal)
                .resourceName(Translator.get("opportunity_stage_setting"))
                .modifiedValue(modifiedVal)
                .resourceId(orgId)
                .build());
    }


    /**
     * 更新配置
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void update(StageUpdateRequest request, String userId) {
        OpportunityStageConfig oldStageConfig = opportunityStageConfigMapper.selectByPrimaryKey(request.getId());
        if (oldStageConfig == null) {
            throw new GenericException(Translator.get("opportunity_stage_not_exist"));
        }
        extOpportunityStageConfigMapper.updateStageConfig(request, userId);

        Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("stage", oldStageConfig.getName());
        originalVal.put("rate", oldStageConfig.getRate());
        Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("stage", request.getName());
        modifiedVal.put("rate", request.getRate());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(request.getId())
                        .resourceName(Translator.get("opportunity_stage_setting"))
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }


    /**
     * 排序
     *
     * @param ids
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void sort(List<String> ids, String orgId) {
        List<OpportunityStageResponse> oldStageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        List<String> oldNames = oldStageConfigList.stream().map(OpportunityStageResponse::getName).toList();

        for (int i = 0; i < ids.size(); i++) {
            extOpportunityStageConfigMapper.updatePos(ids.get(i), (long) (i + 1));
        }

        List<OpportunityStageResponse> newStageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        List<String> newNames = newStageConfigList.stream().map(OpportunityStageResponse::getName).toList();

        Map<String, List<String>> originalVal = new HashMap<>(1);
        originalVal.put("stageSort", oldNames);
        Map<String, List<String>> modifiedVal = new HashMap<>(1);
        modifiedVal.put("stageSort", newNames);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(orgId)
                        .resourceName(Translator.get("opportunity_stage_setting"))
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }
}
