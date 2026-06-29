package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.stage.CirculationSetting;
import cn.cordys.common.dto.stage.StageAdvancedConfigRequest;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.dto.stage.Target;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.opportunity.constants.OpportunityStageType;
import cn.cordys.crm.system.constants.CirculationTypeEnum;
import cn.cordys.crm.system.domain.StageAdvancedConfig;
import cn.cordys.crm.system.mapper.ExtStageAdvancedConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class StageAdvancedConfigService {

    @Resource
    private ExtStageAdvancedConfigMapper extStageAdvancedConfigMapper;
    @Resource
    private BaseMapper<StageAdvancedConfig> stageAdvanceConfigMapper;
    @Resource
    private LogService logService;

    public static final Map<String, String> STAGE_CONFIG_TABLE = new HashMap<>(2);

    static {
        STAGE_CONFIG_TABLE.put(FormKey.ORDER.getKey(), "sales_order_stage_config");
        STAGE_CONFIG_TABLE.put(FormKey.CONTRACT.getKey(), "contract_stage_config");
    }


    public void saveAdvancedConfig(StageAdvancedConfigRequest request, String moduleType, String orgId, String userId) {
        String tableName = STAGE_CONFIG_TABLE.get(moduleType);
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        extStageAdvancedConfigMapper.update(tableName, request.getCirculationType(), orgId, userId, System.currentTimeMillis());

        List<StageAdvancedConfig> oldConfigs = extStageAdvancedConfigMapper.selectConfigByType(orgId, moduleType);

        List<CirculationSetting> circulationSettings = request.getCirculationSettings();
        List<StageAdvancedConfig> stageAdvanceConfigList = new ArrayList<>();
        //List<StageAdvancedConfig> updateStageAdvanceConfigList = new ArrayList<>();

        circulationSettings.forEach(setting -> {
            List<Target> targets = setting.getTargets();
            targets.forEach(target -> {
                StageAdvancedConfig stageAdvancedConfig = oldConfigs.stream().filter(oldConfig -> Strings.CI.equals(oldConfig.getOriginId(), setting.getOriginId())
                        && Strings.CI.equals(oldConfig.getTargetId(), target.getTargetId())
                        && Strings.CI.equals(oldConfig.getModuleType(), moduleType)).findFirst().orElse(null);
                if (stageAdvancedConfig == null) {
                    StageAdvancedConfig stageAdvanceConfig = new StageAdvancedConfig();
                    stageAdvanceConfig.setId(IDGenerator.nextStr());
                    stageAdvanceConfig.setOriginId(setting.getOriginId());
                    stageAdvanceConfig.setTargetId(target.getTargetId());
                    stageAdvanceConfig.setEnable(target.getEnable());
                    stageAdvanceConfig.setFieldConfig(JSON.toJSONString(target.getCirculationFieldValues()));
                    stageAdvanceConfig.setModuleType(moduleType);
                    stageAdvanceConfig.setOrganizationId(orgId);
                    stageAdvanceConfig.setCreateTime(System.currentTimeMillis());
                    stageAdvanceConfig.setCreateUser(userId);
                    stageAdvanceConfig.setUpdateTime(System.currentTimeMillis());
                    stageAdvanceConfig.setUpdateUser(userId);
                    stageAdvanceConfigList.add(stageAdvanceConfig);
                } else {
                    StageAdvancedConfig updateConfig = new StageAdvancedConfig();
                    BeanUtils.copyBean(updateConfig, stageAdvancedConfig);
                    updateConfig.setEnable(target.getEnable());
                    updateConfig.setFieldConfig(JSON.toJSONString(target.getCirculationFieldValues()));
                    updateConfig.setOrganizationId(orgId);
                    updateConfig.setCreateTime(System.currentTimeMillis());
                    updateConfig.setCreateUser(userId);
                    updateConfig.setUpdateTime(System.currentTimeMillis());
                    updateConfig.setUpdateUser(userId);
                    stageAdvanceConfigList.add(updateConfig);
                }
            });

        });

        if (CollectionUtils.isNotEmpty(oldConfigs)) {
            List<String> ids = oldConfigs.stream().map(StageAdvancedConfig::getId).toList();
            stageAdvanceConfigMapper.deleteByIds(ids);

        }

        if (CollectionUtils.isNotEmpty(stageAdvanceConfigList)) {
            stageAdvanceConfigMapper.batchInsert(stageAdvanceConfigList);
        }

        List<StageAdvancedConfig> newConfigs = extStageAdvancedConfigMapper.selectConfigByType(orgId, moduleType);
        final Map<String, Object> originalVal = new HashMap<>(1);
        originalVal.put(moduleType + "Setting", oldConfigs);
        final Map<String, Object> modifiedVal = new HashMap<>(1);
        modifiedVal.put(moduleType + "Setting", newConfigs);

        LogDTO logDTO = new LogDTO(orgId, IDGenerator.nextStr(), userId, CollectionUtils.isNotEmpty(oldConfigs) ? LogType.UPDATE : LogType.ADD, LogModule.SYSTEM_MODULE, Translator.get(moduleType) + Translator.get("advanced_circulation_setting"));
        logDTO.setOriginalValue(originalVal);
        logDTO.setModifiedValue(modifiedVal);
        logService.add(logDTO);
    }


    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void switchType(String type, String moduleType, String orgId) {
        String tableName = STAGE_CONFIG_TABLE.get(moduleType);
        if (StringUtils.isNotBlank(tableName)) {
            extStageAdvancedConfigMapper.update(tableName, type, orgId, null, null);
        }
        final Map<String, Object> originalVal = new HashMap<>(1);
        originalVal.put("circulationType", Strings.CI.equals(CirculationTypeEnum.NORMAL.name(), type) ? Translator.get(CirculationTypeEnum.ADVANCED.name()) : Translator.get(CirculationTypeEnum.NORMAL.name()));
        final Map<String, Object> modifiedVal = new HashMap<>(1);
        modifiedVal.put("circulationType", Translator.get(type));
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(IDGenerator.nextStr())
                        .resourceName(Translator.get(moduleType) + Translator.get("advanced_circulation_setting"))
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }


    /**
     * stage校验
     *
     * @param originStage
     * @param targetStage
     * @param moduleType
     */
    public boolean checkStage(String originStage, String targetStage, String moduleType) {
        if (Strings.CI.equals(originStage, targetStage)) {
            return true;
        }

        String tableName = STAGE_CONFIG_TABLE.get(moduleType);
        if (StringUtils.isBlank(tableName)) {
            return false;
        }

        StageConfigResponse originConfig = extStageAdvancedConfigMapper.getStageConfig(tableName, originStage);
        StageConfigResponse targetConfig = extStageAdvancedConfigMapper.getStageConfig(tableName, targetStage);

        if (originConfig == null || targetConfig == null) {
            return false;
        }

        if (Strings.CI.equals(originConfig.getCirculationType(), CirculationTypeEnum.NORMAL.name())) {
            return handleNormal(originConfig, targetConfig);
        }

        if (Strings.CI.equals(originConfig.getCirculationType(), CirculationTypeEnum.ADVANCED.name())) {
            return checkAdvanced(originConfig, targetConfig, moduleType);
        }

        return false;

    }

    /**
     * 高级流转校验（不抛异常，返回false）
     */
    private boolean checkAdvanced(StageConfigResponse originConfig, StageConfigResponse targetConfig, String moduleType) {
        StageAdvancedConfig config = extStageAdvancedConfigMapper.getConfigByOriginAndTarget(originConfig.getId(), targetConfig.getId(), moduleType);
        return config != null && config.getEnable();
    }

    private boolean handleAdvanced(StageConfigResponse originConfig, StageConfigResponse targetConfig, String moduleType) {
        StageAdvancedConfig config = extStageAdvancedConfigMapper.getConfigByOriginAndTarget(originConfig.getId(), targetConfig.getId(), moduleType);
        if (config == null || !config.getEnable()) {
            throw new GenericException("[" + originConfig.getName() + "] 不允许流转至 [" + targetConfig.getName() + "]");
        }
        return config.getEnable();
    }


    private boolean handleNormal(StageConfigResponse originConfig, StageConfigResponse targetConfig) {
        // 基础流转
        if (originConfig.getEndRollBack() && originConfig.getAfootRollBack()) {
            // 进行中&完结 同时开启 任意流转
            return true;
        }

        if (!originConfig.getEndRollBack() && !originConfig.getAfootRollBack()) {
            // 进行中&完结 同时关闭
            if (targetConfig.getPos() > originConfig.getPos()) {
                return true;
            }
            throw new GenericException("[" + originConfig.getName() + "] 不允许流转至 [" + targetConfig.getName() + "]");
        }

        if (originConfig.getEndRollBack()) {
            // 只开启完结回退配置
            // 按pos顺序（目标pos > 源pos）允许任何类型切换
            if (targetConfig.getPos() > originConfig.getPos()) {
                return true;
            }
            // 特定的类型组合，允许pos不递增的情况
            String originType = originConfig.getType();
            String targetType = targetConfig.getType();

            boolean bothAfoot = Strings.CI.equals(OpportunityStageType.AFOOT.name(), originType) && Strings.CI.equals(OpportunityStageType.AFOOT.name(), targetType);
            if (!bothAfoot) {
                return true;
            }
            throw new GenericException("[" + originConfig.getName() + "] 不允许流转至 [" + targetConfig.getName() + "]");
        }

        if (originConfig.getAfootRollBack()) {
            // 只开启进行中回退
            // 源阶段为 END 时，不允许任何切换
            if (Strings.CI.equals(OpportunityStageType.END.name(), originConfig.getType())) {
                throw new GenericException("[" + originConfig.getName() + "] 不允许流转至 [" + targetConfig.getName() + "]");
            }

            // 源阶段为 AFOOT 时，允许目标为 AFOOT 或 END（不限制 pos）
            String targetType = targetConfig.getType();
            return Strings.CI.equals(OpportunityStageType.END.name(), targetType) || Strings.CI.equals(OpportunityStageType.AFOOT.name(), targetType);
        }

        throw new GenericException("[" + originConfig.getName() + "] 不允许流转至 [" + targetConfig.getName() + "]");
    }
}
