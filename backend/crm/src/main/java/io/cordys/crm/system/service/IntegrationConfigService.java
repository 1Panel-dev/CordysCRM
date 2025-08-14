package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.constants.UserSource;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import io.cordys.crm.integration.auth.dto.ThirdEnableDTO;
import io.cordys.common.constants.ThirdConstants;
import io.cordys.crm.integration.dataease.dto.DeConfigDetailDTO;
import io.cordys.crm.integration.dataease.dto.DeConfigDetailLogDTO;
import io.cordys.crm.integration.sqlbot.dto.SqlBotConfigDetailDTO;
import io.cordys.crm.integration.sqlbot.dto.SqlBotConfigDetailLogDTO;
import io.cordys.crm.integration.sso.service.AgentService;
import io.cordys.crm.integration.sso.service.TokenService;
import io.cordys.crm.integration.wecom.dto.WeComConfigDetailDTO;
import io.cordys.crm.integration.wecom.dto.WeComConfigDetailLogDTO;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class IntegrationConfigService {

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Resource
    private BaseMapper<OrganizationConfigDetail> organizationConfigDetailBaseMapper;

    @Resource
    private BaseMapper<OrganizationConfig> organizationConfigBaseMapper;

    @Resource
    private TokenService tokenService;

    @Resource
    private AgentService agentService;

    private static final String DEFAULT_ORGANIZATION_ID = "100001";

    /**
     * 获取同步的组织配置
     */
    public List<ThirdConfigurationDTO> getThirdConfig(String organizationId) {
        // 检查当前类型是否有过配置
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.THIRD.name()
        );

        if (organizationConfig == null) {
            return new ArrayList<>();
        }

        // 检查当前类型下是否还有数据
        List<OrganizationConfigDetail> organizationConfigDetails = extOrganizationConfigDetailMapper
                .getOrganizationConfigDetails(organizationConfig.getId(), null);

        if (CollectionUtils.isEmpty(organizationConfigDetails)) {
            return new ArrayList<>();
        }

        // 构建第三方配置列表
        List<ThirdConfigurationDTO> configDTOs = new ArrayList<>();

        // 添加企业微信、飞书、钉钉配置
        addConfigIfExists(configDTOs, getThirdConfigurationDTOByType(organizationConfigDetails, DepartmentConstants.WECOM.name()));
        addConfigIfExists(configDTOs, getThirdConfigurationDTOByType(organizationConfigDetails, DepartmentConstants.LARK.name()));
        addConfigIfExists(configDTOs, getThirdConfigurationDTOByType(organizationConfigDetails, DepartmentConstants.DINGTALK.name()));

        // 添加数据看板配置
        ThirdConfigurationDTO deEmbeddedConfig = getThirdConfigurationDTOByType(
                organizationConfigDetails, ThirdConstants.ThirdDetailType.DE_BOARD.toString());
        if (deEmbeddedConfig != null) {
            deEmbeddedConfig.setType(DepartmentConstants.DE.name());
            configDTOs.add(deEmbeddedConfig);
        }

        // 添加SQL机器人配置
        addConfigIfExists(configDTOs, getThirdConfigurationDTOByType(organizationConfigDetails, DepartmentConstants.SQLBOT.name()));

        return configDTOs;
    }

    /**
     * 如果配置不为空，添加到列表中
     */
    private void addConfigIfExists(List<ThirdConfigurationDTO> configs, ThirdConfigurationDTO config) {
        if (config != null) {
            configs.add(config);
        }
    }

    /**
     * 判断已查出的数据类型，不符合类型直接返回null
     *
     * @param organizationConfigDetails 已查出的数据
     * @param type                      类型
     * @return ThirdConfigurationDTO
     */
    private ThirdConfigurationDTO getThirdConfigurationDTOByType(List<OrganizationConfigDetail> organizationConfigDetails, String type) {
        List<OrganizationConfigDetail> detailList = organizationConfigDetails.stream()
                .filter(t -> t.getType().contains(type))
                .toList();

        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }

        ThirdEnableDTO enableDTO = new ThirdEnableDTO();

        // 处理各种类型的启用状态
        for (OrganizationConfigDetail detail : detailList) {
            String detailType = detail.getType();
            Boolean isEnabled = detail.getEnable();

            if (detailType.contains("SYNC")) {
                enableDTO.setSyncEnable(isEnabled);
            } else if (detailType.contains("CODE")) {
                enableDTO.setCodeEnable(isEnabled);
            } else if (detailType.contains("NOTICE")) {
                enableDTO.setNoticeEnable(isEnabled);
            } else if (detailType.contains("BOARD")) {
                enableDTO.setBoardEnable(isEnabled);
            } else if (detailType.contains("CHAT")) {
                enableDTO.setChatEnable(isEnabled);
            }
        }

        return buildThirdConfigurationDTO(detailList.getFirst().getContent(), type, enableDTO);
    }

    /**
     * 构建需要展示的数据结构
     */
    private ThirdConfigurationDTO buildThirdConfigurationDTO(byte[] content, String type, ThirdEnableDTO thirdEnableDTO) {
        ThirdConfigurationDTO configDTO = JSON.parseObject(
                new String(content), ThirdConfigurationDTO.class
        );

        configDTO.setType(type);
        configDTO.setQrcodeEnable(thirdEnableDTO.isCodeEnable());
        configDTO.setSyncEnable(thirdEnableDTO.isSyncEnable());
        configDTO.setWeComEnable(thirdEnableDTO.isNoticeEnable());
        configDTO.setDeBoardEnable(thirdEnableDTO.isBoardEnable());
        configDTO.setSqlBotChatEnable(thirdEnableDTO.isChatEnable());
        configDTO.setSqlBotBoardEnable(thirdEnableDTO.isBoardEnable());

        return configDTO;
    }

    /**
     * 编辑配置
     */
    @OperationLog(module = LogModule.SYSTEM_BUSINESS_THIRD, type = LogType.UPDATE, operator = "{#userId}")
    public void editThirdConfig(ThirdConfigurationDTO configDTO, String organizationId, String userId) {
        // 获取或创建组织配置
        OrganizationConfig organizationConfig = getOrCreateOrganizationConfig(organizationId, userId);

        // 获取当前平台对应类型和启用状态
        List<String> types = getDetailTypes(configDTO);
        Map<String, Boolean> typeEnableMap = getTypeEnableMap(configDTO);

        // 获取当前类型下的配置详情
        List<OrganizationConfigDetail> existingDetails = extOrganizationConfigDetailMapper
                .getOrgConfigDetailByType(organizationConfig.getId(), null, types);

        // 获取验证所需的token
        String token = getToken(configDTO);

        if (CollectionUtils.isEmpty(existingDetails)) {
            // 没有配置详情，创建新的
            handleNewConfigDetails(configDTO, userId, token, types, organizationConfig, typeEnableMap);
        } else {
            // 更新已有配置
            handleExistingConfigDetails(configDTO, userId, token, types, organizationConfig, existingDetails, typeEnableMap);
        }
    }

    /**
     * 获取或创建组织配置
     */
    private OrganizationConfig getOrCreateOrganizationConfig(String organizationId, String userId) {
        OrganizationConfig config = extOrganizationConfigMapper
                .getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());

        if (config == null) {
            config = createNewOrganizationConfig(organizationId, userId);
        }

        return config;
    }

    /**
     * 处理新建配置详情
     */
    private void handleNewConfigDetails(
            ThirdConfigurationDTO configDTO,
            String userId,
            String token,
            List<String> types,
            OrganizationConfig organizationConfig,
            Map<String, Boolean> typeEnableMap) {

        String type = configDTO.getType();

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.WECOM.name())) {
            addWeComDetail(configDTO, userId, token, types, organizationConfig, typeEnableMap);
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DE.name())) {
            addDeDetail(configDTO, userId, token, organizationConfig, types, typeEnableMap);
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            addSqlBotDetail(configDTO, userId, token, types, organizationConfig, typeEnableMap);
        }
    }

    /**
     * 处理已存在的配置详情
     */
    private void handleExistingConfigDetails(
            ThirdConfigurationDTO configDTO,
            String userId,
            String token,
            List<String> types,
            OrganizationConfig organizationConfig,
            List<OrganizationConfigDetail> existingDetails,
            Map<String, Boolean> typeEnableMap) {

        // 原有的配置数据
        ThirdConfigurationDTO oldConfig = JSON.parseObject(
                new String(existingDetails.getFirst().getContent()), ThirdConfigurationDTO.class
        );
        oldConfig.setType(configDTO.getType());

        // 已存在类型的映射
        Map<String, OrganizationConfigDetail> existDetailTypeMap = existingDetails.stream()
                .collect(Collectors.toMap(OrganizationConfigDetail::getType, t -> t));

        // 遍历所有类型，处理更新或新建
        for (String type : types) {
            if (!existDetailTypeMap.containsKey(type)) {
                // 不存在的类型，需要新建
                if (StringUtils.equalsIgnoreCase(configDTO.getType(), DepartmentConstants.WECOM.name())) {
                    addWeComDetail(configDTO, userId, token, List.of(type), organizationConfig, typeEnableMap);
                } else if (StringUtils.equalsIgnoreCase(configDTO.getType(), DepartmentConstants.DE.name())) {
                    addDeDetail(configDTO, userId, token, organizationConfig, List.of(type), typeEnableMap);
                } else if (StringUtils.equalsIgnoreCase(configDTO.getType(), DepartmentConstants.SQLBOT.name())) {
                    addSqlBotDetail(configDTO, userId, token, List.of(type), organizationConfig, typeEnableMap);
                }
            } else {
                // 存在的类型，需要更新
                OrganizationConfigDetail detail = existDetailTypeMap.get(type);
                updateExistingDetail(configDTO, userId, token, oldConfig, detail, typeEnableMap.get(type));
            }
        }

        // 添加日志上下文
        logOperation(oldConfig, configDTO, organizationConfig.getId());
    }

    /**
     * 更新已存在的配置详情
     */
    private void updateExistingDetail(
            ThirdConfigurationDTO configDTO,
            String userId,
            String token,
            ThirdConfigurationDTO oldConfig,
            OrganizationConfigDetail detail,
            Boolean enable) {

        String type = configDTO.getType();

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.WECOM.name())) {
            updateWeCom(configDTO, userId, token, oldConfig, detail, enable);
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DE.name())) {
            updateDe(configDTO, userId, token, oldConfig, detail, enable);
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            updateSqlBot(configDTO, userId, token, oldConfig, detail, enable);
        }
    }

    private void updateDe(ThirdConfigurationDTO configDTO, String userId, String token, ThirdConfigurationDTO oldConfig, OrganizationConfigDetail detail, Boolean enable) {
        DeConfigDetailDTO deConfig = new DeConfigDetailDTO();
        BeanUtils.copyBean(deConfig, configDTO);

        if (Boolean.TRUE.equals(configDTO.getDeBoardEnable())) {
            verifyDe(token, deConfig);
            configDTO.setVerify(deConfig.getVerify());
        } else {
            deConfig.setVerify(configDTO.getVerify());
        }

        // 记录原有配置状态
        DeConfigDetailDTO old = new DeConfigDetailDTO();
        BeanUtils.copyBean(old, oldConfig);
        oldConfig.setDeBoardEnable(detail.getEnable());

        // 更新配置
        updateOrganizationConfigDetail(JSON.toJSONString(deConfig), userId, detail, enable);
    }

    private void updateSqlBot(ThirdConfigurationDTO configDTO, String userId, String token, ThirdConfigurationDTO oldConfig, OrganizationConfigDetail detail, Boolean enable) {
        SqlBotConfigDetailDTO sqlBotConfig = new SqlBotConfigDetailDTO();
        BeanUtils.copyBean(sqlBotConfig, configDTO);

        // 验证配置
        if (configDTO.getSqlBotBoardEnable() || configDTO.getSqlBotChatEnable()) {
            verifySqlBot(token, sqlBotConfig);
            configDTO.setVerify(sqlBotConfig.getVerify());
        } else {
            sqlBotConfig.setVerify(configDTO.getVerify());
        }

        // 记录原有配置状态
        SqlBotConfigDetailDTO old = new SqlBotConfigDetailDTO();
        BeanUtils.copyBean(old, oldConfig);

        if (detail.getType().contains("CHAT")) {
            oldConfig.setSqlBotChatEnable(detail.getEnable());
        }
        if (detail.getType().contains("BOARD")) {
            oldConfig.setSqlBotBoardEnable(detail.getEnable());
        }

        // 更新配置
        boolean isVerified = sqlBotConfig.getVerify() != null ? sqlBotConfig.getVerify() : false;
        updateOrganizationConfigDetail(JSON.toJSONString(sqlBotConfig), userId, detail, isVerified && enable);
    }

    private void updateWeCom(ThirdConfigurationDTO configDTO, String userId, String token, ThirdConfigurationDTO oldConfig, OrganizationConfigDetail detail, Boolean enable) {
        WeComConfigDetailDTO weComConfig = new WeComConfigDetailDTO();
        BeanUtils.copyBean(weComConfig, configDTO);

        // 验证配置
        if (configDTO.getSyncEnable() || configDTO.getQrcodeEnable() || configDTO.getWeComEnable()) {
            verifyWeCom(configDTO, token, weComConfig);
            configDTO.setVerify(weComConfig.getVerify());
        } else {
            weComConfig.setVerify(configDTO.getVerify());
        }

        // 记录原有配置状态
        WeComConfigDetailDTO old = new WeComConfigDetailDTO();
        BeanUtils.copyBean(old, oldConfig);

        // 根据详情类型设置原配置的启用状态
        String detailType = detail.getType();
        if (detailType.contains("SYNC")) {
            oldConfig.setSyncEnable(detail.getEnable());
        } else if (detailType.contains("CODE")) {
            oldConfig.setQrcodeEnable(detail.getEnable());
        } else if (detailType.contains("NOTICE")) {
            oldConfig.setWeComEnable(detail.getEnable());
        }

        // 更新配置
        boolean isVerified = weComConfig.getVerify() != null ? weComConfig.getVerify() : false;
        updateOrganizationConfigDetail(JSON.toJSONString(weComConfig), userId, detail, isVerified && enable);
    }

    private void addDeDetail(ThirdConfigurationDTO configDTO, String userId, String token, OrganizationConfig organizationConfig, List<String> types, Map<String, Boolean> typeEnableMap) {
        DeConfigDetailDTO deConfig = new DeConfigDetailDTO();
        BeanUtils.copyBean(deConfig, configDTO);

        // 验证配置
        if (Boolean.TRUE.equals(configDTO.getDeBoardEnable())) {
            verifyDe(token, deConfig);
        } else {
            deConfig.setVerify(configDTO.getVerify());
        }

        // 保存配置
        saveDetail(userId, organizationConfig, types, typeEnableMap, JSON.toJSONString(deConfig), deConfig.getVerify());
    }

    private void verifyDe(String token, DeConfigDetailDTO deConfig) {
        deConfig.setVerify(StringUtils.isNotBlank(token) && StringUtils.equalsIgnoreCase(token, "true"));
    }

    private void saveDetail(String userId, OrganizationConfig organizationConfig, List<String> types, Map<String, Boolean> typeEnableMap, String jsonString, Boolean verify) {
        for (String type : types) {
            OrganizationConfigDetail detail = createConfigDetail(userId, organizationConfig, jsonString);
            detail.setType(type);

            // 设置启用状态
            if (verify != null) {
                detail.setEnable(verify && typeEnableMap.get(type));
            } else {
                detail.setEnable(false);
            }

            detail.setName(Translator.get("third.setting"));
            organizationConfigDetailBaseMapper.insert(detail);
        }
    }

    /**
     * 创建配置详情对象
     */
    private OrganizationConfigDetail createConfigDetail(String userId, OrganizationConfig organizationConfig, String jsonString) {
        OrganizationConfigDetail detail = new OrganizationConfigDetail();
        detail.setId(IDGenerator.nextStr());
        detail.setContent(jsonString.getBytes());
        detail.setCreateTime(System.currentTimeMillis());
        detail.setUpdateTime(System.currentTimeMillis());
        detail.setCreateUser(userId);
        detail.setUpdateUser(userId);
        detail.setConfigId(organizationConfig.getId());
        return detail;
    }

    private void addWeComDetail(ThirdConfigurationDTO configDTO, String userId, String token, List<String> types, OrganizationConfig organizationConfig, Map<String, Boolean> typeEnableMap) {
        WeComConfigDetailDTO weComConfig = new WeComConfigDetailDTO();
        BeanUtils.copyBean(weComConfig, configDTO);

        // 验证配置
        if (configDTO.getSyncEnable() || configDTO.getQrcodeEnable() || configDTO.getWeComEnable()) {
            verifyWeCom(configDTO, token, weComConfig);
        } else {
            weComConfig.setVerify(configDTO.getVerify());
        }

        // 保存配置
        saveDetail(userId, organizationConfig, types, typeEnableMap, JSON.toJSONString(weComConfig), weComConfig.getVerify());
    }

    private void verifyWeCom(ThirdConfigurationDTO configDTO, String token, WeComConfigDetailDTO weComConfig) {
        if (StringUtils.isNotBlank(token)) {
            // 验证应用ID
            Boolean weComAgent = agentService.getWeComAgent(token, configDTO.getAgentId());
            weComConfig.setVerify(weComAgent != null && weComAgent);
        } else {
            weComConfig.setVerify(false);
        }
    }

    private void addSqlBotDetail(ThirdConfigurationDTO configDTO, String userId, String token, List<String> types, OrganizationConfig organizationConfig, Map<String, Boolean> typeEnableMap) {
        SqlBotConfigDetailDTO sqlBotConfig = new SqlBotConfigDetailDTO();
        BeanUtils.copyBean(sqlBotConfig, configDTO);

        // 验证配置
        if (configDTO.getSqlBotBoardEnable() || configDTO.getSqlBotChatEnable()) {
            verifySqlBot(token, sqlBotConfig);
        } else {
            sqlBotConfig.setVerify(configDTO.getVerify());
        }

        // 保存配置
        saveDetail(userId, organizationConfig, types, typeEnableMap, JSON.toJSONString(sqlBotConfig), sqlBotConfig.getVerify());
    }

    private void verifySqlBot(String token, SqlBotConfigDetailDTO sqlBotConfig) {
        sqlBotConfig.setVerify(StringUtils.isNotBlank(token) && StringUtils.equalsIgnoreCase(token, "true"));
    }

    /**
     * 根据配置类型获取详情类型列表
     */
    @NotNull
    private List<String> getDetailTypes(ThirdConfigurationDTO configDTO) {
        String type = configDTO.getType();

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.WECOM.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(),
                    ThirdConstants.ThirdDetailType.WECOM_CODE.toString(),
                    ThirdConstants.ThirdDetailType.WECOM_NOTICE.toString()
            );
        }

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DINGTALK.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(),
                    ThirdConstants.ThirdDetailType.DINGTALK_CODE.toString()
            );
        }

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.LARK.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.LARK_SYNC.toString(),
                    ThirdConstants.ThirdDetailType.LARK_CODE.toString()
            );
        }

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DE.name())) {
            return List.of(ThirdConstants.ThirdDetailType.DE_BOARD.toString());
        }

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.SQLBOT_CHAT.toString(),
                    ThirdConstants.ThirdDetailType.SQLBOT_BOARD.toString()
            );
        }

        return new ArrayList<>();
    }

    /**
     * 获取类型启用状态映射
     */
    private Map<String, Boolean> getTypeEnableMap(ThirdConfigurationDTO configDTO) {
        Map<String, Boolean> map = new HashMap<>();
        String type = configDTO.getType();

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.WECOM.name())) {
            map.put(ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(), configDTO.getSyncEnable());
            map.put(ThirdConstants.ThirdDetailType.WECOM_CODE.toString(), configDTO.getQrcodeEnable());
            map.put(ThirdConstants.ThirdDetailType.WECOM_NOTICE.toString(), configDTO.getWeComEnable());
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DINGTALK.name())) {
            map.put(ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(), configDTO.getSyncEnable());
            map.put(ThirdConstants.ThirdDetailType.DINGTALK_CODE.toString(), configDTO.getQrcodeEnable());
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.LARK.name())) {
            map.put(ThirdConstants.ThirdDetailType.LARK_SYNC.toString(), configDTO.getSyncEnable());
            map.put(ThirdConstants.ThirdDetailType.LARK_CODE.toString(), configDTO.getQrcodeEnable());
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DE.name())) {
            map.put(ThirdConstants.ThirdDetailType.DE_BOARD.toString(),
                    configDTO.getDeBoardEnable() != null && configDTO.getDeBoardEnable());
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            map.put(ThirdConstants.ThirdDetailType.SQLBOT_CHAT.toString(), configDTO.getSqlBotChatEnable());
            map.put(ThirdConstants.ThirdDetailType.SQLBOT_BOARD.toString(), configDTO.getSqlBotBoardEnable());
        }

        return map;
    }

    /**
     * 获取验证所需的token
     */
    private String getToken(ThirdConfigurationDTO configDTO) {
        String type = configDTO.getType();

        if (DepartmentConstants.WECOM.name().equals(type)) {
            return tokenService.getAssessToken(configDTO.getCorpId(), configDTO.getAppSecret());
        } else if (DepartmentConstants.DINGTALK.name().equals(type)) {
            return tokenService.getDingTalkToken(configDTO.getAppKey(), configDTO.getAppSecret());
        } else if (DepartmentConstants.LARK.name().equals(type)) {
            return tokenService.getLarkToken(configDTO.getAgentId(), configDTO.getAppSecret());
        } else if (DepartmentConstants.DE.name().equals(type)) {
            return tokenService.pingDeUrl(configDTO.getRedirectUrl()) ? "true" : null;
        } else if (DepartmentConstants.SQLBOT.name().equals(type)) {
            return tokenService.getSqlBotSrc(configDTO.getAppSecret()) ? "true" : null;
        }

        return null;
    }

    /**
     * 创建新的组织配置
     */
    private OrganizationConfig createNewOrganizationConfig(String organizationId, String userId) {
        OrganizationConfig config = new OrganizationConfig();
        config.setId(IDGenerator.nextStr());
        config.setOrganizationId(organizationId);
        config.setType(OrganizationConfigConstants.ConfigType.THIRD.name());
        config.setCreateTime(System.currentTimeMillis());
        config.setUpdateTime(System.currentTimeMillis());
        config.setCreateUser(userId);
        config.setUpdateUser(userId);
        organizationConfigBaseMapper.insert(config);
        return config;
    }

    /**
     * 更新组织配置详情
     */
    private void updateOrganizationConfigDetail(String jsonString, String userId, OrganizationConfigDetail detail, Boolean enable) {
        detail.setContent(jsonString.getBytes());
        detail.setUpdateTime(System.currentTimeMillis());
        detail.setUpdateUser(userId);
        detail.setEnable(enable);
        organizationConfigDetailBaseMapper.update(detail);
    }

    /**
     * 记录操作日志
     */
    private void logOperation(ThirdConfigurationDTO oldConfig, ThirdConfigurationDTO newConfig, String id) {
        Object oldLog = null;
        Object newLog = null;

        String type = newConfig.getType();

        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.WECOM.name())) {
            WeComConfigDetailLogDTO oldDTO = new WeComConfigDetailLogDTO();
            WeComConfigDetailLogDTO newDTO = new WeComConfigDetailLogDTO();
            BeanUtils.copyBean(oldDTO, oldConfig);
            BeanUtils.copyBean(newDTO, newConfig);
            oldLog = oldDTO;
            newLog = newDTO;
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.DE.name())) {
            DeConfigDetailLogDTO oldDTO = getDeConfigDetailLogDTO(oldConfig);
            DeConfigDetailLogDTO newDTO = getDeConfigDetailLogDTO(newConfig);
            oldLog = oldDTO;
            newLog = newDTO;
        } else if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            SqlBotConfigDetailLogDTO oldDTO = getSqlBotConfigDetailLogDTO(oldConfig);
            SqlBotConfigDetailLogDTO newDTO = getSqlBotConfigDetailLogDTO(newConfig);
            oldLog = oldDTO;
            newLog = newDTO;
        }

        if (oldLog != null) {
            OperationLogContext.setContext(LogContextInfo.builder()
                    .resourceName(Translator.get("third.setting"))
                    .resourceId(id)
                    .originalValue(oldLog)
                    .modifiedValue(newLog)
                    .build());
        }
    }

    @NotNull
    private DeConfigDetailLogDTO getDeConfigDetailLogDTO(ThirdConfigurationDTO config) {
        DeConfigDetailLogDTO dto = new DeConfigDetailLogDTO();
        dto.setDeAppId(config.getAgentId());
        dto.setDeAppSecret(config.getAppSecret());
        dto.setDeAccount(config.getDeAccount());
        dto.setDeBoardEnable(config.getDeBoardEnable());
        dto.setDeUrl(config.getRedirectUrl());
        return dto;
    }

    private SqlBotConfigDetailLogDTO getSqlBotConfigDetailLogDTO(ThirdConfigurationDTO config) {
        SqlBotConfigDetailLogDTO dto = new SqlBotConfigDetailLogDTO();
        dto.setSqlBotAppSecret(config.getAppSecret());
        dto.setSqlBotChatEnable(config.getSqlBotChatEnable());
        dto.setSqlBotBoardEnable(config.getSqlBotBoardEnable());
        return dto;
    }

    /**
     * 测试连接
     */
    public boolean testConnection(ThirdConfigurationDTO configDTO, String organizationId, String userId) {
        // 参数验证
        if (StringUtils.isBlank(configDTO.getAppSecret())) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }

        String type = configDTO.getType();
        String token = getToken(configDTO);
        List<String> types = getDetailTypes(configDTO);

        // 验证token
        if (DepartmentConstants.WECOM.name().equals(type) && StringUtils.isNotBlank(token)) {
            Boolean weComAgent = agentService.getWeComAgent(token, configDTO.getAgentId());
            if (weComAgent == null || !weComAgent) {
                token = null;
            }
        }

        boolean result = StringUtils.isNotBlank(token);

        // 更新配置
        updateExistingConfigIfNeeded(configDTO, organizationId, userId, token, types, result);

        return result;
    }

    /**
     * 如果存在配置，更新它
     */
    private void updateExistingConfigIfNeeded(ThirdConfigurationDTO configDTO, String organizationId, String userId, String token, List<String> types, boolean result) {
        OrganizationConfig config = extOrganizationConfigMapper
                .getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());

        if (config == null) {
            return;
        }

        List<OrganizationConfigDetail> details = extOrganizationConfigDetailMapper
                .getOrgConfigDetailByType(config.getId(), null, types);

        if (CollectionUtils.isEmpty(details)) {
            return;
        }

        // 获取原有配置
        ThirdConfigurationDTO oldConfig = JSON.parseObject(
                new String(details.getFirst().getContent()), ThirdConfigurationDTO.class
        );
        oldConfig.setType(configDTO.getType());
        configDTO.setVerify(result);

        // 更新所有详情
        for (OrganizationConfigDetail detail : details) {
            updateExistingDetail(configDTO, userId, token, oldConfig, detail, detail.getEnable());
        }

        // 添加日志上下文
        logOperation(oldConfig, configDTO, config.getId());
    }

    /**
     * 获取同步状态
     */
    public boolean getSyncStatus(String orgId, String type, String syncResource) {
        OrganizationConfig syncStatus = extOrganizationConfigMapper.getSyncStatus(orgId, type, syncResource);
        return syncStatus != null && BooleanUtils.isTrue(syncStatus.isSync());
    }

    /**
     * 根据类型获取第三方配置
     */
    public ThirdConfigurationDTO getThirdConfigByType(String type) {
        // 确定配置类型和组织ID
        String configType = StringUtils.equalsIgnoreCase(type, UserSource.WE_COM_OAUTH2.toString())
                ? OrganizationConfigConstants.ConfigType.AUTH.name()
                : OrganizationConfigConstants.ConfigType.THIRD.name();

        // 获取组织配置
        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                DEFAULT_ORGANIZATION_ID, configType
        );

        if (config == null) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }

        // 获取配置详情
        List<OrganizationConfigDetail> details = extOrganizationConfigDetailMapper
                .getOrganizationConfigDetails(config.getId(), null);

        if (CollectionUtils.isEmpty(details)) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }

        // 获取指定类型的配置
        ThirdConfigurationDTO configDTO = getConfigurationByType(type, config, details);

        // 隐藏敏感信息
        if (!StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            configDTO.setAppSecret(null);
        }

        return configDTO;
    }

    /**
     * 根据类型获取配置
     */
    private ThirdConfigurationDTO getConfigurationByType(String type, OrganizationConfig config, List<OrganizationConfigDetail> details) {
        if (StringUtils.equalsIgnoreCase(type, UserSource.WE_COM_OAUTH2.toString())) {
            return getOAuth2Configuration(type, config.getId());
        } else {
            return getNormalConfiguration(type, details);
        }
    }

    /**
     * 获取OAuth2配置
     */
    private ThirdConfigurationDTO getOAuth2Configuration(String type, String configId) {
        List<OrganizationConfigDetail> enableDetails = extOrganizationConfigDetailMapper
                .getEnableOrganizationConfigDetails(configId, type);

        if (CollectionUtils.isEmpty(enableDetails)) {
            throw new GenericException(Translator.get("third.config.un.enable"));
        }

        OrganizationConfigDetail detail = enableDetails.getFirst();
        if (detail == null) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }

        ThirdConfigurationDTO configDTO = JSON.parseObject(
                new String(detail.getContent()), ThirdConfigurationDTO.class
        );
        configDTO.setType(type);

        return configDTO;
    }

    /**
     * 获取普通配置
     */
    private ThirdConfigurationDTO getNormalConfiguration(String type, List<OrganizationConfigDetail> details) {
        ThirdConfigurationDTO configDTO = getThirdConfigurationDTOByType(details, type);

        if (configDTO == null) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }

        // 检查是否启用
        if (StringUtils.equalsIgnoreCase(type, DepartmentConstants.SQLBOT.name())) {
            if (configDTO.getSqlBotChatEnable() == null || !configDTO.getSqlBotChatEnable()) {
                throw new GenericException(Translator.get("third.config.un.enable"));
            }
        } else if (!configDTO.getQrcodeEnable()) {
            throw new GenericException(Translator.get("third.config.un.enable"));
        }

        return configDTO;
    }

    /**
     * 获取第三方类型列表
     */
    public List<OptionDTO> getThirdTypeList() {
        // 获取组织配置
        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                DEFAULT_ORGANIZATION_ID, OrganizationConfigConstants.ConfigType.THIRD.name()
        );

        if (config == null) {
            return new ArrayList<>();
        }

        // 获取CODE类型的配置详情
        List<String> codeTypes = List.of(
                ThirdConstants.ThirdDetailType.WECOM_CODE.toString(),
                ThirdConstants.ThirdDetailType.DINGTALK_CODE.toString(),
                ThirdConstants.ThirdDetailType.LARK_CODE.toString()
        );

        List<OrganizationConfigDetail> details = extOrganizationConfigDetailMapper
                .getOrgConfigDetailByType(config.getId(), null, codeTypes);

        if (CollectionUtils.isEmpty(details)) {
            return new ArrayList<>();
        }

        // 构建选项列表
        return details.stream()
                .map(this::getOptionDTO)
                .sorted(Comparator.comparing(OptionDTO::getId).reversed())
                .toList();
    }


    /**
     * 配置详情转换为选项
     */
    @NotNull
    private OptionDTO getOptionDTO(OrganizationConfigDetail detail) {
        OptionDTO option = new OptionDTO();
        String type = detail.getType();

        if (type.contains(DepartmentConstants.WECOM.name())) {
            option.setId(DepartmentConstants.WECOM.name());
        } else if (type.contains(DepartmentConstants.DINGTALK.name())) {
            option.setId(DepartmentConstants.DINGTALK.name());
        } else if (type.contains(DepartmentConstants.LARK.name())) {
            option.setId(DepartmentConstants.LARK.name());
        }

        option.setName(detail.getEnable().toString());
        return option;
    }
}