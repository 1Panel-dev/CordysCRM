package cn.cordys.crm.system.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.stage.CirculationSetting;
import cn.cordys.common.dto.stage.StageAdvancedConfigRequest;
import cn.cordys.common.dto.stage.Target;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.StageAdvancedConfig;
import cn.cordys.crm.system.mapper.ExtStageAdvancedConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

        circulationSettings.forEach(setting -> {
            List<Target> targets = setting.getTargets();
            targets.forEach(target -> {
                StageAdvancedConfig stageAdvanceConfig = new StageAdvancedConfig();
                stageAdvanceConfig.setId(IDGenerator.nextStr());
                stageAdvanceConfig.setOriginId(setting.getOriginId());
                stageAdvanceConfig.setTargetId(target.getTargetId());
                stageAdvanceConfig.setEnable(target.getEnable());
                stageAdvanceConfig.setFieldConfig(JSON.toJSONString(target.getCirculationFieldValues()));
                stageAdvanceConfig.setModuleType(moduleType);
                stageAdvanceConfig.setCreateTime(System.currentTimeMillis());
                stageAdvanceConfig.setCreateUser(userId);
                stageAdvanceConfig.setUpdateTime(System.currentTimeMillis());
                stageAdvanceConfig.setUpdateUser(userId);
                stageAdvanceConfigList.add(stageAdvanceConfig);
            });

        });

        if (CollectionUtils.isNotEmpty(oldConfigs)) {
            List<String> ids = oldConfigs.stream().map(StageAdvancedConfig::getId).toList();
            stageAdvanceConfigMapper.deleteByIds(ids);
        }

        if (CollectionUtils.isNotEmpty(stageAdvanceConfigList)) {
            stageAdvanceConfigMapper.batchInsert(stageAdvanceConfigList);
        }

        LogDTO logDTO = new LogDTO(orgId, IDGenerator.nextStr(), userId, LogType.UPDATE, LogModule.SYSTEM_MODULE, Translator.get(moduleType) + Translator.get("advanced_circulation_setting"));
        logDTO.setOriginalValue(oldConfigs);
        logDTO.setModifiedValue(stageAdvanceConfigList);
        logService.add(logDTO);
    }


    public void switchType(String type, String moduleType, String orgId) {
        String tableName = STAGE_CONFIG_TABLE.get(moduleType);
        if (StringUtils.isNotBlank(tableName)) {
            extStageAdvancedConfigMapper.update(tableName, type, orgId, null, null);
        }

    }
}
