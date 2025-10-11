package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.DepartmentConstants;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.constants.UserSource;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.common.dto.ThirdEnableDTO;
import cn.cordys.crm.integration.dataease.DataEaseClient;
import cn.cordys.crm.integration.dataease.dto.DeAuthDTO;
import cn.cordys.crm.integration.dataease.dto.DeConfigDetailDTO;
import cn.cordys.crm.integration.dataease.dto.DeConfigDetailLogDTO;
import cn.cordys.crm.integration.dataease.service.DataEaseService;
import cn.cordys.crm.integration.sqlbot.dto.SqlBotConfigDetailDTO;
import cn.cordys.crm.integration.sqlbot.dto.SqlBotConfigDetailLogDTO;
import cn.cordys.crm.integration.sso.service.AgentService;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.integration.common.dto.ThirdConfigDetailDTO;
import cn.cordys.crm.integration.common.dto.ThirdConfigDetailLogDTO;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
    private DataEaseService dataEaseService;

    @Resource
    private AgentService agentService;

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

            if (detailType.contains("SYNC") || detailType.contains("CODE") || detailType.contains("NOTICE")) {
                enableDTO.setStartEnable(isEnabled);
            } else if (detailType.contains("OAUTH2")) {
                enableDTO.setOAuth2Enable(isEnabled);
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
        configDTO.setStartEnable(thirdEnableDTO.isStartEnable());
        configDTO.setOAuth2Enable(thirdEnableDTO.isOAuth2Enable());
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
        List<String> types = getDetailTypes(configDTO.getType());
        Map<String, Boolean> typeEnableMap = getTypeEnableMap(configDTO);

        // 获取当前类型下的配置详情
        List<OrganizationConfigDetail> existingDetails = extOrganizationConfigDetailMapper
                .getOrgConfigDetailByType(organizationConfig.getId(), null, types);

        // 获取验证所需的token
        String token = getToken(configDTO);

        //这里检查一下最近同步的来源是否和当前修改的一致，如果不一致，且当前平台开启同步按钮，则关闭其他平台按钮
        String lastSyncType = getLastSyncType(organizationConfig.getId());
        if (!Strings.CI.equals(lastSyncType, configDTO.getType()) && configDTO.getStartEnable()) {
            // 关闭其他平台按钮
            List<String> detailTypes = getDetailTypes(lastSyncType);
            detailTypes.forEach(detailType -> {
                extOrganizationConfigDetailMapper.updateStatus(
                        false, detailType, organizationConfig.getId()
                );
            });
        }

        if (CollectionUtils.isEmpty(existingDetails)) {
            // 没有配置详情，创建新的
            handleNewConfigDetails(configDTO, userId, token, types, organizationConfig, typeEnableMap);
        } else {
            // 更新已有配置
            handleExistingConfigDetails(configDTO, userId, token, types, organizationConfig, existingDetails, typeEnableMap);
        }
    }

    private String getLastSyncType(String id) {
        OrganizationConfig organizationConfig = organizationConfigBaseMapper.selectByPrimaryKey(id);
        if (organizationConfig != null && organizationConfig.isSync() && StringUtils.isNotBlank(organizationConfig.getSyncResource())) {
            return organizationConfig.getSyncResource();
        } else {
            return null;
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

        addIntegrationDetail(configDTO, userId, token, types, organizationConfig, typeEnableMap);
    }

    /**
     * 封装了各种 `add...Detail` 方法的统一入口
     */
    private void addIntegrationDetail(ThirdConfigurationDTO configDTO, String userId, String token, List<String> types, OrganizationConfig organizationConfig, Map<String, Boolean> typeEnableMap) {
        String type = configDTO.getType();
        String jsonContent;
        Boolean verify;

        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            ThirdConfigDetailDTO weComConfig = new ThirdConfigDetailDTO();
            BeanUtils.copyBean(weComConfig, configDTO);
            if (configDTO.getStartEnable()) {
                verifyWeCom(configDTO, token, weComConfig);
            } else {
                weComConfig.setVerify(configDTO.getVerify());
            }
            jsonContent = JSON.toJSONString(weComConfig);
            verify = weComConfig.getVerify();
        } else if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            ThirdConfigDetailDTO dingTalkConfigDetailDTO = new ThirdConfigDetailDTO();
            BeanUtils.copyBean(dingTalkConfigDetailDTO, configDTO);
            if (configDTO.getStartEnable()) {
                verifyDingTalk(token, dingTalkConfigDetailDTO);
            } else {
                dingTalkConfigDetailDTO.setVerify(configDTO.getVerify());
            }
            jsonContent = JSON.toJSONString(dingTalkConfigDetailDTO);
            verify = dingTalkConfigDetailDTO.getVerify();
        } else if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            ThirdConfigDetailDTO larkConfigDetailDTO = new ThirdConfigDetailDTO();
            BeanUtils.copyBean(larkConfigDetailDTO, configDTO);
            if (configDTO.getStartEnable()) {
                verifyLark(token, larkConfigDetailDTO);
            } else {
                larkConfigDetailDTO.setVerify(configDTO.getVerify());
            }
            jsonContent = JSON.toJSONString(larkConfigDetailDTO);
            verify = larkConfigDetailDTO.getVerify();
        } else if (Strings.CI.equals(type, DepartmentConstants.DE.name())) {
            DeConfigDetailDTO deConfig = new DeConfigDetailDTO();
            BeanUtils.copyBean(deConfig, configDTO);
            if (Boolean.TRUE.equals(configDTO.getDeBoardEnable())) {
                verifyDe(token, deConfig);
            } else {
                deConfig.setVerify(configDTO.getVerify());
            }
            jsonContent = JSON.toJSONString(deConfig);
            verify = deConfig.getVerify();
        } else if (Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
            SqlBotConfigDetailDTO sqlBotConfig = new SqlBotConfigDetailDTO();
            BeanUtils.copyBean(sqlBotConfig, configDTO);
            if (configDTO.getSqlBotBoardEnable() || configDTO.getSqlBotChatEnable()) {
                verifySqlBot(token, sqlBotConfig);
            } else {
                sqlBotConfig.setVerify(configDTO.getVerify());
            }
            jsonContent = JSON.toJSONString(sqlBotConfig);
            verify = sqlBotConfig.getVerify();
        }  else {
            return;
        }

        saveDetail(userId, organizationConfig, types, typeEnableMap, jsonContent, verify);
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
                addIntegrationDetail(configDTO, userId, token, List.of(type), organizationConfig, typeEnableMap);
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

        updateIntegrationDetail(configDTO, userId, token, oldConfig, detail, enable);
    }

    /**
     * 封装了各种 `update...` 方法的统一入口
     */
    private void updateIntegrationDetail(
            ThirdConfigurationDTO configDTO,
            String userId,
            String token,
            ThirdConfigurationDTO oldConfig,
            OrganizationConfigDetail detail,
            Boolean enable) {

        String type = configDTO.getType();
        String jsonContent;
        boolean isVerified;
        String detailType = detail.getType();
        boolean openEnable;

        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            ThirdConfigDetailDTO weComConfig = new ThirdConfigDetailDTO();
            BeanUtils.copyBean(weComConfig, configDTO);

            if (configDTO.getStartEnable()) {
                verifyWeCom(configDTO, token, weComConfig);
                configDTO.setVerify(weComConfig.getVerify());
            } else {
                weComConfig.setVerify(configDTO.getVerify());
            }

            updateOldConfigEnableState(oldConfig, detailType, detail.getEnable());

            isVerified = weComConfig.getVerify() != null && weComConfig.getVerify();
            jsonContent = JSON.toJSONString(weComConfig);
            openEnable = isVerified && enable;

        } else if (Strings.CI.equals(type, DepartmentConstants.DE.name())) {
            DeConfigDetailDTO deConfig = new DeConfigDetailDTO();
            BeanUtils.copyBean(deConfig, configDTO);

            if (Boolean.TRUE.equals(configDTO.getDeBoardEnable())) {
                verifyDe(token, deConfig);
                configDTO.setVerify(deConfig.getVerify());
            } else {
                deConfig.setVerify(configDTO.getVerify());
            }

            oldConfig.setDeBoardEnable(detail.getEnable());

            jsonContent = JSON.toJSONString(deConfig);
            openEnable = enable;

        } else if (Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
            SqlBotConfigDetailDTO sqlBotConfig = new SqlBotConfigDetailDTO();
            BeanUtils.copyBean(sqlBotConfig, configDTO);

            if (configDTO.getSqlBotBoardEnable() || configDTO.getSqlBotChatEnable()) {
                verifySqlBot(token, sqlBotConfig);
                configDTO.setVerify(sqlBotConfig.getVerify());
            } else {
                sqlBotConfig.setVerify(configDTO.getVerify());
            }

            if (detailType.contains("CHAT")) {
                oldConfig.setSqlBotChatEnable(detail.getEnable());
            }
            if (detailType.contains("BOARD")) {
                oldConfig.setSqlBotBoardEnable(detail.getEnable());
            }

            isVerified = sqlBotConfig.getVerify() != null && sqlBotConfig.getVerify();
            jsonContent = JSON.toJSONString(sqlBotConfig);
            openEnable = isVerified && enable;
        } else if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            ThirdConfigDetailDTO dingTalkConfigDetailDTO = new ThirdConfigDetailDTO();
            BeanUtils.copyBean(dingTalkConfigDetailDTO, configDTO);

            if (configDTO.getStartEnable()) {
                verifyDingTalk(token, dingTalkConfigDetailDTO);
                configDTO.setVerify(dingTalkConfigDetailDTO.getVerify());
            } else {
                dingTalkConfigDetailDTO.setVerify(configDTO.getVerify());
            }

            updateOldConfigEnableState(oldConfig, detailType, detail.getEnable());

            isVerified = dingTalkConfigDetailDTO.getVerify() != null && dingTalkConfigDetailDTO.getVerify();
            jsonContent = JSON.toJSONString(dingTalkConfigDetailDTO);

            openEnable = isVerified && enable;

        } else if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            ThirdConfigDetailDTO larkConfigDetailDTO = new ThirdConfigDetailDTO();
            BeanUtils.copyBean(larkConfigDetailDTO, configDTO);

            if (configDTO.getStartEnable()) {
                verifyLark(token, larkConfigDetailDTO);
                configDTO.setVerify(larkConfigDetailDTO.getVerify());
            } else {
                larkConfigDetailDTO.setVerify(configDTO.getVerify());
            }

            updateOldConfigEnableState(oldConfig, detailType, detail.getEnable());

            isVerified = larkConfigDetailDTO.getVerify() != null && larkConfigDetailDTO.getVerify();
            jsonContent = JSON.toJSONString(larkConfigDetailDTO);

            openEnable = isVerified && enable;
        } else {
            return;
        }

        updateOrganizationConfigDetail(jsonContent, userId, detail, openEnable);

    }

    private void updateOldConfigEnableState(ThirdConfigurationDTO oldConfig, String detailType, Boolean enable) {
        if (detailType.contains("SYNC") || detailType.contains("CODE") || detailType.contains("NOTICE")) {
            oldConfig.setStartEnable(enable);
        }
        if (detailType.contains("OAUTH2")) {
            oldConfig.setOAuth2Enable(enable);
        }
    }

    private void verifyDe(String token, DeConfigDetailDTO deConfig) {
        deConfig.setVerify(StringUtils.isNotBlank(token) && Strings.CI.equals(token, "true"));
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

    private void verifyWeCom(ThirdConfigurationDTO configDTO, String token, ThirdConfigDetailDTO weComConfig) {
        if (StringUtils.isNotBlank(token)) {
            // 验证应用ID
            Boolean weComAgent = agentService.getWeComAgent(token, configDTO.getAgentId());
            weComConfig.setVerify(weComAgent != null && weComAgent);
        } else {
            weComConfig.setVerify(false);
        }
    }

    private void verifyDingTalk(String token, ThirdConfigDetailDTO dingTalkConfigDetailDTO) {
        dingTalkConfigDetailDTO.setVerify(StringUtils.isNotBlank(token));
    }

    private void verifyLark(String token, ThirdConfigDetailDTO larkConfigDetailDTO) {
        larkConfigDetailDTO.setVerify(StringUtils.isNotBlank(token));
    }

    private void verifySqlBot(String token, SqlBotConfigDetailDTO sqlBotConfig) {
        sqlBotConfig.setVerify(StringUtils.isNotBlank(token) && Strings.CI.equals(token, "true"));
    }

    /**
     * 根据配置类型获取详情类型列表
     */
    private List<String> getDetailTypes(String type) {
        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(),
                    ThirdConstants.ThirdDetailType.WECOM_CODE.toString(),
                    ThirdConstants.ThirdDetailType.WECOM_NOTICE.toString(),
                    UserSource.WECOM_OAUTH2.toString()
            );
        }

        if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(),
                    ThirdConstants.ThirdDetailType.DINGTALK_CODE.toString(),
                    ThirdConstants.ThirdDetailType.DINGTALK_NOTICE.toString(),
                    UserSource.DINGTALK_OAUTH2.toString()
            );
        }

        if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            return List.of(
                    ThirdConstants.ThirdDetailType.LARK_SYNC.toString(),
                    ThirdConstants.ThirdDetailType.LARK_CODE.toString(),
                    ThirdConstants.ThirdDetailType.LARK_NOTICE.toString(),
                    UserSource.LARK_OAUTH2.toString()
            );
        }

        if (Strings.CI.equals(type, DepartmentConstants.DE.name())) {
            return List.of(ThirdConstants.ThirdDetailType.DE_BOARD.toString());
        }

        if (Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
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

        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            map.put(ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(), configDTO.getStartEnable());
            map.put(ThirdConstants.ThirdDetailType.WECOM_CODE.toString(), configDTO.getStartEnable());
            map.put(ThirdConstants.ThirdDetailType.WECOM_NOTICE.toString(), configDTO.getStartEnable());
            map.put(UserSource.WECOM_OAUTH2.toString(), configDTO.getOAuth2Enable());
        } else if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            map.put(ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(), configDTO.getStartEnable());
            map.put(ThirdConstants.ThirdDetailType.DINGTALK_CODE.toString(), configDTO.getStartEnable());
            map.put(ThirdConstants.ThirdDetailType.DINGTALK_NOTICE.toString(), configDTO.getStartEnable());
            map.put(UserSource.DINGTALK_OAUTH2.toString(), configDTO.getOAuth2Enable());
        } else if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            map.put(ThirdConstants.ThirdDetailType.LARK_SYNC.toString(), configDTO.getStartEnable());
            map.put(ThirdConstants.ThirdDetailType.LARK_CODE.toString(), configDTO.getStartEnable());
            map.put(ThirdConstants.ThirdDetailType.LARK_NOTICE.toString(), configDTO.getStartEnable());
            map.put(UserSource.LARK_OAUTH2.toString(), configDTO.getOAuth2Enable());
        } else if (Strings.CI.equals(type, DepartmentConstants.DE.name())) {
            map.put(ThirdConstants.ThirdDetailType.DE_BOARD.toString(),
                    configDTO.getDeBoardEnable() != null && configDTO.getDeBoardEnable());
        } else if (Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
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
            return tokenService.getDingTalkToken(configDTO.getAgentId(), configDTO.getAppSecret());
        } else if (DepartmentConstants.LARK.name().equals(type)) {
            return tokenService.getLarkToken(configDTO.getAgentId(), configDTO.getAppSecret());
        } else if (DepartmentConstants.DE.name().equals(type)) {
            boolean verify = validDeConfig(configDTO);
            return verify ? "true" : null;
        } else if (DepartmentConstants.SQLBOT.name().equals(type)) {
            return tokenService.getSqlBotSrc(configDTO.getAppSecret()) ? "true" : null;
        }

        return null;
    }

    private boolean validDeConfig(ThirdConfigurationDTO configDTO) {
        // 校验url
        boolean verify = tokenService.pingDeUrl(configDTO.getRedirectUrl());
        DataEaseClient dataEaseClient = new DataEaseClient(configDTO);
        if (BooleanUtils.isTrue(configDTO.getDeModuleEmbedding())
                && StringUtils.isNotBlank(configDTO.getDeAccessKey())
                && StringUtils.isNotBlank(configDTO.getDeSecretKey())
                && StringUtils.isNotBlank(configDTO.getRedirectUrl())) {
            // 校验 ak，sk
            verify = verify && dataEaseClient.validate();
        }
        String account = StringUtils.isBlank(configDTO.getDeAccount()) ? InternalUser.ADMIN.name() : configDTO.getDeAccount();
        DeAuthDTO embeddedDeToken = dataEaseService.getEmbeddedDeToken(account, configDTO);
        // 校验嵌入式token
        verify = verify && dataEaseClient.validateEmbeddedToken(embeddedDeToken.getToken());
        return verify;
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

        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name()) ||
                Strings.CI.equals(type, DepartmentConstants.DINGTALK.name()) ||
                Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            ThirdConfigDetailLogDTO oldDTO = new ThirdConfigDetailLogDTO();
            ThirdConfigDetailLogDTO newDTO = new ThirdConfigDetailLogDTO();
            BeanUtils.copyBean(oldDTO, oldConfig);
            BeanUtils.copyBean(newDTO, newConfig);
            oldLog = oldDTO;
            newLog = newDTO;
        }else if (Strings.CI.equals(type, DepartmentConstants.DE.name())) {
            DeConfigDetailLogDTO oldDTO = getDeConfigDetailLogDTO(oldConfig);
            DeConfigDetailLogDTO newDTO = getDeConfigDetailLogDTO(newConfig);
            oldLog = oldDTO;
            newLog = newDTO;
        } else if (Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
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

    private DeConfigDetailLogDTO getDeConfigDetailLogDTO(ThirdConfigurationDTO config) {
        DeConfigDetailLogDTO dto = new DeConfigDetailLogDTO();
        dto.setDeAppId(config.getAgentId());
        dto.setDeAppSecret(config.getAppSecret());
        dto.setDeAccount(config.getDeAccount());
        dto.setDeBoardEnable(config.getDeBoardEnable());
        dto.setDeUrl(config.getRedirectUrl());
        dto.setDeModuleEmbedding(config.getDeModuleEmbedding());
        dto.setDeLinkIntegration(config.getDeLinkIntegration());
        dto.setDeAutoSync(config.getDeAutoSync());
        dto.setDeAccessKey(config.getDeAccessKey());
        dto.setDeSecretKey(config.getDeSecretKey());
        dto.setDeOrgID(config.getDeOrgID());
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
        List<String> types = getDetailTypes(type);

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
    public ThirdConfigurationDTO getThirdConfigForPublic(String type, String orgId) {
        // 确定配置类型和组织ID
        String configType = OrganizationConfigConstants.ConfigType.THIRD.name();

        // 获取组织配置
        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                orgId, configType
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
        ThirdConfigurationDTO configDTO = getConfigurationByType(type, details);

        // 隐藏敏感信息
        if (!Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
            configDTO.setAppSecret(null);
            configDTO.setDeSecretKey(null);
            configDTO.setDeAccessKey(null);
        }

        return configDTO;
    }

    /**
     * 根据类型获取配置
     */
    private ThirdConfigurationDTO getConfigurationByType(String type, List<OrganizationConfigDetail> details) {
        return getNormalConfiguration(type, details);
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
        if (Strings.CI.equals(type, DepartmentConstants.SQLBOT.name())) {
            if (configDTO.getSqlBotChatEnable() == null || !configDTO.getSqlBotChatEnable()) {
                throw new GenericException(Translator.get("third.config.un.enable"));
            }
        } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.DE_BOARD.name())) {
            if (configDTO.getRedirectUrl() == null) {
                throw new GenericException(Translator.get("third.config.un.enable"));
            }
        } else if (!configDTO.getStartEnable()) {
            throw new GenericException(Translator.get("third.config.un.enable"));
        }

        return configDTO;
    }

    /**
     * 获取第三方类型列表
     */
    public List<OptionDTO> getThirdTypeList(String orgId) {
        // 获取组织配置
        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                orgId, OrganizationConfigConstants.ConfigType.THIRD.name()
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