package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.*;
import cn.cordys.crm.integration.dataease.DataEaseClient;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.integration.sync.dto.ThirdSwitchLogDTO;
import cn.cordys.crm.integration.tender.constant.TenderApiPaths;
import cn.cordys.crm.integration.tender.dto.TenderDetailDTO;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
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

    /**
     * 获取同步的组织配置
     *
     * @param organizationId 组织ID
     *
     * @return 第三方配置列表
     */
    public List<ThirdConfigBaseDTO<?>> getThirdConfig(String organizationId) {
        List<OrganizationConfigDetail> organizationConfigDetails = initConfig(organizationId, SessionUtils.getUserId());

        // 构建第三方配置列表
        List<ThirdConfigBaseDTO<?>> configDTOs = new ArrayList<>();
        buildDetailData(organizationConfigDetails, configDTOs);

        return configDTOs;
    }

    /**
     * 编辑配置
     *
     * @param configDTO      配置DTO
     * @param organizationId 组织ID
     * @param userId         用户ID
     */
    @OperationLog(module = LogModule.SYSTEM_BUSINESS_THIRD, type = LogType.UPDATE, operator = "{#userId}")
    public void editThirdConfig(ThirdConfigBaseDTO<Object> configDTO, String organizationId, String userId) {
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
        if (lastSyncType != null && !Strings.CI.equals(lastSyncType, configDTO.getType()) && getEnable(configDTO)) {
            // 关闭其他平台按钮
            List<String> detailTypes = getDetailTypes(lastSyncType);
            detailTypes.forEach(detailType -> extOrganizationConfigDetailMapper.updateStatus(
                    false, detailType, organizationConfig.getId()
            ));
        }

        if (CollectionUtils.isEmpty(existingDetails)) {
            // 没有配置详情，创建新的
            handleNewConfigDetails(configDTO, userId, token, types, organizationConfig, typeEnableMap);
        } else {
            // 更新已有配置
            handleExistingConfigDetails(configDTO, userId, token, types, organizationConfig, existingDetails, typeEnableMap);
        }
    }

    /**
     * 测试连接
     *
     * @param configDTO 配置DTO
     *
     * @return 是否连接成功
     */
    public boolean testConnection(ThirdConfigBaseDTO<?> configDTO) {
        String token = getToken(configDTO);
        if (ThirdConfigTypeConstants.WECOM.name().equals(configDTO.getType()) && StringUtils.isNotBlank(token)) {
            WecomThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
            boolean weComAgent = tokenService.checkWeComAgentAvailable(token, config.getAgentId());
            if (!weComAgent) {
                token = null;
            }
        }
        return StringUtils.isNotBlank(token);
    }

    /**
     * 获取同步状态
     *
     * @param orgId        组织ID
     * @param type         类型
     * @param syncResource 同步资源
     *
     * @return 是否同步
     */
    public boolean getSyncStatus(String orgId, String type, String syncResource) {
        OrganizationConfig syncStatus = extOrganizationConfigMapper.getSyncStatus(orgId, type, syncResource);
        return syncStatus != null && BooleanUtils.isTrue(syncStatus.isSync());
    }

    /**
     * 根据类型获取第三方配置
     *
     * @param type  类型
     * @param orgId 组织ID
     *
     * @return 配置DTO
     */
    public ThirdConfigBaseDTO<?> getThirdConfigForPublic(String type, String orgId) {
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
        if (type.contains(ThirdConfigTypeConstants.WECOM.name())) {
            type = ThirdConstants.ThirdDetailType.WECOM_SYNC.toString();
        }
        if (type.contains(ThirdConfigTypeConstants.DINGTALK.name())) {
            type = ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString();
        }
        if (type.contains(ThirdConfigTypeConstants.LARK.name())) {
            type = ThirdConstants.ThirdDetailType.LARK_SYNC.toString();
        }

        return getConfigurationByType(type, details);
    }

    /**
     * 获取第三方类型列表
     *
     * @param orgId 组织ID
     *
     * @return 选项列表
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
                ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(),
                ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(),
                ThirdConstants.ThirdDetailType.LARK_SYNC.toString()
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
     * 切换第三方设置
     *
     * @param type           类型
     * @param organizationId 组织ID
     */
    @OperationLog(module = LogModule.SYSTEM_BUSINESS_THIRD, type = LogType.UPDATE, operator = "{#userId}")
    public void switchThirdPartySetting(String type, String organizationId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
        if (organizationConfig == null) {
            return;
        }
        String oldType = organizationConfig.getSyncResource();

        if (type.equals(oldType)) {
            return;
        }
        ThirdSwitchLogDTO oldLog = new ThirdSwitchLogDTO();
        oldLog.setThirdType(oldType);
        ThirdSwitchLogDTO newLog = new ThirdSwitchLogDTO();
        newLog.setThirdType(type);
        //这里检查一下最近同步的来源是否和当前修改的一致，如果不一致，则关闭其他平台按钮
        // 关闭其他平台按钮
        List<String> detailTypes = getDetailTypes(oldType);
        detailTypes.forEach(detailType -> extOrganizationConfigDetailMapper.updateStatus(
                false, detailType, organizationConfig.getId()
        ));
        extOrganizationConfigMapper.updateSyncFlag(organizationId, type, OrganizationConfigConstants.ConfigType.THIRD.name(), false);
        OperationLogContext.setContext(LogContextInfo.builder()
                .resourceName(Translator.get("third.setting"))
                .resourceId(organizationConfig.getId())
                .originalValue(oldLog)
                .modifiedValue(newLog)
                .build());
    }

    /**
     * 获取最近同步资源
     *
     * @param organizationId 组织ID
     *
     * @return 组织配置
     */
    public OrganizationConfig getLatestSyncResource(String organizationId) {
        return extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
    }

    /**
     * 获取应用配置
     *
     * @param organizationId 组织ID
     * @param userId         用户ID
     * @param type           类型
     *
     * @return 配置DTO
     */
    public ThirdConfigBaseDTO<?> getApplicationConfig(String organizationId, String userId, String type) {
        List<OrganizationConfigDetail> organizationConfigDetails = initConfig(organizationId, userId);
        return getThirdConfigurationDTOByType(organizationConfigDetails, type);
    }

    /**
     * 构建详情数据
     *
     * @param details    配置详情列表
     * @param configDTOs 配置DTO列表
     */
    private void buildDetailData(List<OrganizationConfigDetail> details,
                                 List<ThirdConfigBaseDTO<?>> configDTOs) {

        ThirdConfigBaseDTO<SqlBotThirdConfigRequest> sqlBotConfig = new ThirdConfigBaseDTO<>();
        SqlBotThirdConfigRequest sqlBotDTO = new SqlBotThirdConfigRequest();

        for (OrganizationConfigDetail detail : details) {
            String type = detail.getType();
            String content = new String(detail.getContent());

            if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.WECOM_SYNC.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        WecomThirdConfigRequest.class,
                        cfg -> cfg.setStartEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.WECOM.name()
                );
                configDTOs.add(dto);

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.DINGTALK_SYNC.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        DingTalkThirdConfigRequest.class,
                        cfg -> cfg.setStartEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.DINGTALK.name()
                );
                configDTOs.add(dto);

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.LARK_SYNC.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        LarkThirdConfigRequest.class,
                        cfg -> cfg.setStartEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.LARK.name()
                );
                configDTOs.add(dto);

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.DE_BOARD.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        DeThirdConfigRequest.class,
                        cfg -> cfg.setDeBoardEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.DE.name()
                );
                configDTOs.add(dto);

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.SQLBOT_CHAT.name())
                    || Strings.CI.equals(type, ThirdConstants.ThirdDetailType.SQLBOT_BOARD.name())) {

                ThirdConfigBaseDTO<?> dto = JSON.parseObject(content, ThirdConfigBaseDTO.class);
                SqlBotThirdConfigRequest cfg = buildConfig(dto, content, SqlBotThirdConfigRequest.class);

                if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.SQLBOT_CHAT.name())) {
                    sqlBotDTO.setSqlBotChatEnable(detail.getEnable());
                } else {
                    sqlBotDTO.setSqlBotBoardEnable(detail.getEnable());
                }

                sqlBotDTO.setAppSecret(cfg.getAppSecret());
                sqlBotConfig.setType(ThirdConfigTypeConstants.SQLBOT.name());
                sqlBotConfig.setVerify(dto.getVerify());

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.MAXKB.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        MaxKBThirdConfigRequest.class,
                        cfg -> cfg.setMkEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.MAXKB.name()
                );
                configDTOs.add(dto);

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.TENDER.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        TenderThirdConfigRequest.class,
                        cfg -> cfg.setTenderEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.TENDER.name()
                );
                configDTOs.add(dto);

            } else if (Strings.CI.equals(type, ThirdConstants.ThirdDetailType.QCC.name())) {
                ThirdConfigBaseDTO<?> dto = buildDto(
                        content,
                        QccThirdConfigRequest.class,
                        cfg -> cfg.setQccEnable(detail.getEnable()),
                        ThirdConfigTypeConstants.QCC.name()
                );
                configDTOs.add(dto);
            }
        }

        if (sqlBotConfig.getType() != null) {
            sqlBotConfig.setConfig(sqlBotDTO);
            configDTOs.add(sqlBotConfig);
        }
    }

    /**
     * 构建DTO
     *
     * @param content      内容
     * @param configClass  配置类
     * @param enableSetter 启用设置器
     * @param type         类型
     * @param <T>          配置类型
     *
     * @return 配置DTO
     */
    private <T> ThirdConfigBaseDTO<?> buildDto(String content, Class<T> configClass, Consumer<T> enableSetter, String type) {

        ThirdConfigBaseDTO dto = JSON.parseObject(content, ThirdConfigBaseDTO.class);
        T config = buildConfig(dto, content, configClass);

        enableSetter.accept(config);
        dto.setType(type);
        dto.setConfig(config);
        return dto;
    }

    /**
     * 构建配置
     *
     * @param dto         DTO
     * @param content     内容
     * @param configClass 配置类
     * @param <T>         配置类型
     *
     * @return 配置对象
     */
    private <T> T buildConfig(ThirdConfigBaseDTO<?> dto, String content, Class<T> configClass) {

        return dto.getConfig() == null
                ? JSON.parseObject(content, configClass)
                : JSON.MAPPER.convertValue(dto.getConfig(), configClass);
    }

    /**
     * 初始化配置
     *
     * @param organizationId 组织ID
     * @param userId         用户ID
     *
     * @return 配置详情列表
     */
    private List<OrganizationConfigDetail> initConfig(String organizationId, String userId) {
        // 获取或创建组织配置
        OrganizationConfig organizationConfig = getOrCreateOrganizationConfig(organizationId, userId);

        // 检查当前类型下是否还有数据
        List<OrganizationConfigDetail> organizationConfigDetails = extOrganizationConfigDetailMapper
                .getOrganizationConfigDetails(organizationConfig.getId(), null);

        OrganizationConfigDetail tenderConfig = organizationConfigDetails.stream().filter(detail -> Strings.CI.contains(detail.getType(), ThirdConfigTypeConstants.TENDER.name()))
                .findFirst().orElse(null);
        if (tenderConfig == null) {
            initTender(userId, organizationConfig);
        }

        organizationConfigDetails = extOrganizationConfigDetailMapper
                .getOrganizationConfigDetails(organizationConfig.getId(), null);
        return organizationConfigDetails;
    }

    /**
     * 初始化招标配置
     *
     * @param userId             用户ID
     * @param organizationConfig 组织配置
     */
    private void initTender(String userId, OrganizationConfig organizationConfig) {
        TenderDetailDTO tenderConfig = new TenderDetailDTO();
        tenderConfig.setTenderAddress(TenderApiPaths.TENDER_API);
        tenderConfig.setVerify(true);
        OrganizationConfigDetail detail = createConfigDetail(userId, organizationConfig, JSON.toJSONString(tenderConfig));
        detail.setType(ThirdConfigTypeConstants.TENDER.name());
        detail.setEnable(true);
        detail.setName(Translator.get("third.setting"));
        organizationConfigDetailBaseMapper.insert(detail);
    }

    /**
     * 根据类型获取第三方配置DTO
     *
     * @param organizationConfigDetails 配置详情列表
     * @param type                      类型
     *
     * @return 配置DTO
     */
    private ThirdConfigBaseDTO<?> getThirdConfigurationDTOByType(
            List<OrganizationConfigDetail> organizationConfigDetails, String type) {

        List<OrganizationConfigDetail> detailList = organizationConfigDetails.stream()
                .filter(t -> t.getType().contains(type))
                .toList();

        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        ThirdConfigBaseDTO dto = new ThirdConfigBaseDTO();

        for (OrganizationConfigDetail detail : detailList) {

            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.WECOM_SYNC.name())) {
                dto = buildDTO(detail, WecomThirdConfigRequest.class, ThirdConfigTypeConstants.WECOM.name(),
                        cfg -> cfg.setStartEnable(detail.getEnable()));
            }

            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.DINGTALK_SYNC.name())) {
                dto = buildDTO(detail, DingTalkThirdConfigRequest.class, ThirdConfigTypeConstants.DINGTALK.name(),
                        cfg -> cfg.setStartEnable(detail.getEnable()));
            }

            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.LARK_SYNC.name())) {
                dto = buildDTO(detail, LarkThirdConfigRequest.class, ThirdConfigTypeConstants.LARK.name(),
                        cfg -> cfg.setStartEnable(detail.getEnable()));
            }

            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.MAXKB.name())) {
                dto = buildDTO(detail, MaxKBThirdConfigRequest.class, ThirdConfigTypeConstants.MAXKB.name(),
                        cfg -> cfg.setMkEnable(detail.getEnable()));
            }

            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.TENDER.name())) {
                dto = buildDTO(detail, TenderThirdConfigRequest.class, ThirdConfigTypeConstants.TENDER.name(),
                        cfg -> cfg.setTenderEnable(detail.getEnable()));
            }

            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.DE_BOARD.name())) {
                dto = buildDTO(detail, DeThirdConfigRequest.class, ThirdConfigTypeConstants.DE.name(),
                        cfg -> cfg.setDeBoardEnable(detail.getEnable()));
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.SQLBOT_CHAT.name())) {
                dto = buildDTO(detail, SqlBotThirdConfigRequest.class, ThirdConfigTypeConstants.SQLBOT.name(),
                        cfg -> cfg.setSqlBotChatEnable(detail.getEnable()));
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.SQLBOT_BOARD.name())) {
                dto = buildDTO(detail, SqlBotThirdConfigRequest.class, ThirdConfigTypeConstants.SQLBOT.name(),
                        cfg -> cfg.setSqlBotBoardEnable(detail.getEnable()));
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.QCC.name())) {
                dto = buildDTO(detail, QccThirdConfigRequest.class, ThirdConfigTypeConstants.QCC.name(),
                        cfg -> cfg.setQccEnable(detail.getEnable()));
            }
        }

        return dto;
    }

    /**
     * 构建DTO
     *
     * @param detail       配置详情
     * @param configClass  配置类
     * @param type         类型
     * @param enableSetter 启用设置器
     * @param <T>          配置类型
     *
     * @return 配置DTO
     */
    private <T> ThirdConfigBaseDTO<?> buildDTO(
            OrganizationConfigDetail detail,
            Class<T> configClass,
            String type,
            Consumer<T> enableSetter) {

        ThirdConfigBaseDTO dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);

        T config;
        if (dto.getConfig() == null) {
            config = JSON.parseObject(new String(detail.getContent()), configClass);
        } else {
            config = JSON.MAPPER.convertValue(dto.getConfig(), configClass);
        }

        enableSetter.accept(config);

        dto.setType(type);
        dto.setConfig(config);
        return dto;
    }

    /**
     * 获取或创建组织配置
     *
     * @param organizationId 组织ID
     * @param userId         用户ID
     *
     * @return 组织配置
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
     *
     * @param configDTO          配置DTO
     * @param userId             用户ID
     * @param token              Token
     * @param types              类型列表
     * @param organizationConfig 组织配置
     * @param typeEnableMap      类型启用映射
     */
    private void handleNewConfigDetails(
            ThirdConfigBaseDTO<Object> configDTO,
            String userId,
            String token,
            List<String> types,
            OrganizationConfig organizationConfig,
            Map<String, Boolean> typeEnableMap) {

        addIntegrationDetail(configDTO, userId, token, types, organizationConfig, typeEnableMap);
    }

    /**
     * 添加集成详情
     *
     * @param configDTO          配置DTO
     * @param userId             用户ID
     * @param token              Token
     * @param types              类型列表
     * @param organizationConfig 组织配置
     * @param typeEnableMap      类型启用映射
     */
    private void addIntegrationDetail(ThirdConfigBaseDTO<Object> configDTO, String userId, String token, List<String> types, OrganizationConfig organizationConfig, Map<String, Boolean> typeEnableMap) {
        String jsonContent = null;
        Boolean verify = false;
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        if (typeConstants == null) {
            throw new GenericException("unsupported.third.type");
        }
        switch (typeConstants) {
            case WECOM -> {
                WecomThirdConfigRequest wecomConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                if (wecomConfig.getStartEnable()) {
                    verifyWeCom(wecomConfig.getAgentId(), token, configDTO);
                }
                configDTO.setConfig(wecomConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(wecomConfig)));
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                if (dingTalkConfig.getStartEnable()) {
                    verifyDingTalk(token, configDTO);
                }
                configDTO.setConfig(dingTalkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(dingTalkConfig)));
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                if (larkConfig.getStartEnable()) {
                    verifyLark(token, configDTO);
                }
                configDTO.setConfig(larkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(larkConfig)));
            }
            case DE -> {
                DeThirdConfigRequest deConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                if (BooleanUtils.isTrue(deConfig.getDeBoardEnable())) {
                    verifyDe(token, configDTO);
                }
                configDTO.setConfig(deConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(deConfig)));
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest sqlBotConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                if (sqlBotConfig.getSqlBotBoardEnable() || sqlBotConfig.getSqlBotChatEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(sqlBotConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(sqlBotConfig)));
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest mkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                if (mkConfig.getMkEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(mkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(mkConfig)));
            }
            case TENDER -> {
                TenderThirdConfigRequest tenderConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), TenderThirdConfigRequest.class);
                tenderConfig.setTenderAddress(TenderApiPaths.TENDER_API);
                if (tenderConfig.getTenderEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(tenderConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(tenderConfig)));
            }
            case QCC -> {
                QccThirdConfigRequest qccConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                if (qccConfig.getQccEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(qccConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(qccConfig)));
            }
        }

        saveDetail(userId, organizationConfig, types, typeEnableMap, jsonContent, verify);
    }

    /**
     * 处理已存在的配置详情
     *
     * @param configDTO          配置DTO
     * @param userId             用户ID
     * @param token              Token
     * @param types              类型列表
     * @param organizationConfig 组织配置
     * @param existingDetails    现有详情列表
     * @param typeEnableMap      类型启用映射
     */
    private void handleExistingConfigDetails(
            ThirdConfigBaseDTO<Object> configDTO,
            String userId,
            String token,
            List<String> types,
            OrganizationConfig organizationConfig,
            List<OrganizationConfigDetail> existingDetails,
            Map<String, Boolean> typeEnableMap) {

        // 已存在类型的映射
        Map<String, OrganizationConfigDetail> existDetailTypeMap = existingDetails.stream()
                .collect(Collectors.toMap(OrganizationConfigDetail::getType, t -> t));
        //ThirdConfigTypeConstants type = ThirdConfigTypeConstants.fromString(configDTO.getType());
        for (String type : types) {
            if (!existDetailTypeMap.containsKey(type)) {
                // 不存在的类型，需要新建
                addIntegrationDetail(configDTO, userId, token, List.of(type), organizationConfig, typeEnableMap);
            } else {
                // 存在的类型，需要更新
                OrganizationConfigDetail detail = existDetailTypeMap.get(type);
                //如果更改的企业id和之前不一致，则如果之前的同步状态未true，则改为false
                if (BooleanUtils.isTrue(organizationConfig.isSync()) && organizationConfig.getSyncResource() != null && Strings.CI.equals(organizationConfig.getSyncResource(), configDTO.getType())
                        && syncCorpId(existingDetails.getFirst().getContent(), configDTO)) {
                    extOrganizationConfigMapper.updateSyncFlag(organizationConfig.getOrganizationId(), organizationConfig.getSyncResource(), organizationConfig.getType(), false);
                }
                updateExistingDetail(configDTO, userId, token, detail, typeEnableMap.get(type), organizationConfig.getId());
            }
        }
    }

    /**
     * 更新已存在的配置详情
     *
     * @param configDTO 配置DTO
     * @param userId    用户ID
     * @param token     Token
     * @param detail    配置详情
     * @param enable    是否启用
     * @param id        ID
     */
    private void updateExistingDetail(
            ThirdConfigBaseDTO<Object> configDTO,
            String userId,
            String token,
            OrganizationConfigDetail detail,
            Boolean enable,
            String id) {

        updateIntegrationDetail(configDTO, userId, token, detail, enable, id);
    }

    /**
     * 更新集成详情
     *
     * @param configDTO 配置DTO
     * @param userId    用户ID
     * @param token     Token
     * @param detail    配置详情
     * @param enable    是否启用
     * @param id        ID
     */
    private void updateIntegrationDetail(
            ThirdConfigBaseDTO<Object> configDTO,
            String userId,
            String token,
            OrganizationConfigDetail detail,
            Boolean enable,
            String id) {

        String jsonContent = null;
        boolean openEnable = false;

        ThirdConfigTypeConstants typeConstants =
                ThirdConfigTypeConstants.fromString(configDTO.getType());

        if (typeConstants == null) {
            throw new GenericException("unsupported.third.type");
        }

        switch (typeConstants) {
            case WECOM -> {
                WecomThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                if (config.getStartEnable()) {
                    verifyWeCom(config.getAgentId(), token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);

                boolean isVerified = Boolean.TRUE.equals(configDTO.getVerify());
                openEnable = isVerified && enable;

                WecomThirdConfigRequest oldConfig = parseOldConfig(detail, WecomThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case DINGTALK -> {
                DingTalkThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);

                if (config.getStartEnable()) {
                    verifyDingTalk(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);

                boolean isVerified = Boolean.TRUE.equals(configDTO.getVerify());
                openEnable = isVerified && enable;

                DingTalkThirdConfigRequest oldConfig = parseOldConfig(detail, DingTalkThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case LARK -> {
                LarkThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);

                if (config.getStartEnable()) {
                    verifyLark(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);

                boolean isVerified = Boolean.TRUE.equals(configDTO.getVerify());
                openEnable = isVerified && enable;

                LarkThirdConfigRequest oldConfig = parseOldConfig(detail, LarkThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case DE -> {
                DeThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);

                if (BooleanUtils.isTrue(config.getDeBoardEnable())) {
                    verifyDe(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                DeThirdConfigRequest oldConfig = parseOldConfig(detail, DeThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case SQLBOT -> {
                SqlBotThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);

                if (config.getSqlBotBoardEnable() || config.getSqlBotChatEnable()) {
                    verifyToken(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);

                boolean isVerified = Boolean.TRUE.equals(configDTO.getVerify());
                openEnable = isVerified && enable;

                SqlBotThirdConfigRequest oldConfig = parseOldConfig(detail, SqlBotThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case MAXKB -> {
                MaxKBThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);

                if (config.getMkEnable()) {
                    verifyToken(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                MaxKBThirdConfigRequest oldConfig = parseOldConfig(detail, MaxKBThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case TENDER -> {
                TenderThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), TenderThirdConfigRequest.class);

                config.setTenderAddress(TenderApiPaths.TENDER_API);

                if (config.getTenderEnable()) {
                    verifyToken(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                TenderThirdConfigRequest oldConfig = parseOldConfig(detail, TenderThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }

            case QCC -> {
                QccThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                if (config.getQccEnable()) {
                    verifyToken(token, configDTO);
                }

                configDTO.setConfig(config);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                QccThirdConfigRequest oldConfig = parseOldConfig(detail, QccThirdConfigRequest.class);

                addConfigLog(oldConfig, configDTO, id, config);
            }
        }

        if (StringUtils.isNotBlank(jsonContent)) {
            updateOrganizationConfigDetail(jsonContent, userId, detail, openEnable);
        }
    }

    /**
     * 解析旧配置
     *
     * @param detail 配置详情
     * @param clazz  类
     * @param <T>    类型
     *
     * @return 配置对象
     */
    private <T> T parseOldConfig(OrganizationConfigDetail detail, Class<T> clazz) {
        ThirdConfigBaseDTO<?> oldDto =
                JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);

        if (oldDto.getConfig() == null) {
            return JSON.parseObject(new String(detail.getContent()), clazz);
        }
        return JSON.MAPPER.convertValue(oldDto.getConfig(), clazz);
    }

    /**
     * 添加配置日志
     *
     * @param oldConfig 旧配置
     * @param newDto    新DTO
     * @param id        ID
     * @param newConfig 新配置
     */
    private void addConfigLog(Object oldConfig,
                              ThirdConfigBaseDTO<?> newDto,
                              String id,
                              Object newConfig) {
        addLog(
                JSON.parseToMap(JSON.toJSONString(oldConfig)),
                newDto,
                id,
                JSON.parseToMap(JSON.toJSONString(newConfig))
        );
    }

    /**
     * 添加日志
     *
     * @param oldMap    旧映射
     * @param configDTO 配置DTO
     * @param id        ID
     * @param newMap    新映射
     */
    private void addLog(Map<String, Object> oldMap, ThirdConfigBaseDTO<?> configDTO, String id, Map<String, Object> newMap) {
        oldMap.put("type", configDTO.getType());
        oldMap.put("verify", configDTO.getVerify());
        newMap.put("type", configDTO.getType());
        newMap.put("verify", configDTO.getVerify());
        OperationLogContext.setContext(LogContextInfo.builder()
                .resourceName(Translator.get("third.setting"))
                .resourceId(id)
                .originalValue(oldMap)
                .modifiedValue(newMap)
                .build());
    }

    /**
     * 验证De配置
     *
     * @param token    Token
     * @param deConfig 配置DTO
     */
    private void verifyDe(String token, ThirdConfigBaseDTO<?> deConfig) {
        deConfig.setVerify(StringUtils.isNotBlank(token) && Strings.CI.equals(token, "true"));
    }

    /**
     * 保存详情
     *
     * @param userId             用户ID
     * @param organizationConfig 组织配置
     * @param types              类型列表
     * @param typeEnableMap      类型启用映射
     * @param jsonString         JSON字符串
     * @param verify             是否验证
     */
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
     * 创建配置详情
     *
     * @param userId             用户ID
     * @param organizationConfig 组织配置
     * @param jsonString         JSON字符串
     *
     * @return 配置详情
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

    /**
     * 验证WeCom
     *
     * @param agentId     代理ID
     * @param token       Token
     * @param weComConfig 配置DTO
     */
    private void verifyWeCom(String agentId, String token, ThirdConfigBaseDTO<?> weComConfig) {
        if (StringUtils.isNotBlank(token)) {
            // 验证应用ID
            Boolean weComAgent = tokenService.checkWeComAgentAvailable(token, agentId);
            weComConfig.setVerify(weComAgent);
        } else {
            weComConfig.setVerify(false);
        }
    }

    /**
     * 验证DingTalk
     *
     * @param token                   Token
     * @param dingTalkConfigDetailDTO 配置DTO
     */
    private void verifyDingTalk(String token, ThirdConfigBaseDTO<?> dingTalkConfigDetailDTO) {
        dingTalkConfigDetailDTO.setVerify(StringUtils.isNotBlank(token));
    }

    /**
     * 验证Lark
     *
     * @param token               Token
     * @param larkConfigDetailDTO 配置DTO
     */
    private void verifyLark(String token, ThirdConfigBaseDTO<?> larkConfigDetailDTO) {
        larkConfigDetailDTO.setVerify(StringUtils.isNotBlank(token));
    }

    /**
     * 验证Token
     *
     * @param token  Token
     * @param config 配置DTO
     */
    private void verifyToken(String token, ThirdConfigBaseDTO<?> config) {
        config.setVerify(StringUtils.isNotBlank(token) && Strings.CI.equals(token, "true"));
    }

    /**
     * 根据配置类型获取详情类型列表
     *
     * @param type 类型
     *
     * @return 类型列表
     */
    private List<String> getDetailTypes(String type) {
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(type);
        if (typeConstants == null) {
            return Collections.emptyList();
        }
        return switch (typeConstants) {
            case WECOM -> List.of(ThirdConstants.ThirdDetailType.WECOM_SYNC.toString());
            case DINGTALK -> List.of(ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString());
            case LARK -> List.of(ThirdConstants.ThirdDetailType.LARK_SYNC.toString());
            case DE -> List.of(ThirdConstants.ThirdDetailType.DE_BOARD.toString());
            case SQLBOT -> List.of(
                    ThirdConstants.ThirdDetailType.SQLBOT_CHAT.toString(),
                    ThirdConstants.ThirdDetailType.SQLBOT_BOARD.toString()
            );
            case MAXKB -> List.of(ThirdConstants.ThirdDetailType.MAXKB.toString());
            case TENDER -> List.of(ThirdConstants.ThirdDetailType.TENDER.toString());
            case QCC -> List.of(ThirdConstants.ThirdDetailType.QCC.toString());
            default -> Collections.emptyList();
        };
    }

    /**
     * 获取类型启用状态映射
     *
     * @param configDTO 配置DTO
     *
     * @return 类型启用映射
     */
    private Map<String, Boolean> getTypeEnableMap(ThirdConfigBaseDTO<?> configDTO) {
        Map<String, Boolean> map = new HashMap<>();
        ThirdConfigTypeConstants type = ThirdConfigTypeConstants.fromString(configDTO.getType());
        if (type == null) {
            return Map.of();
        }
        switch (type) {
            case WECOM -> {
                WecomThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(), config.getStartEnable());
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(), config.getStartEnable());
            }
            case LARK -> {
                LarkThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.LARK_SYNC.toString(), config.getStartEnable());
            }
            case DE -> {
                DeThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.DE_BOARD.toString(), config.getDeBoardEnable());
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.SQLBOT_CHAT.toString(), config.getSqlBotChatEnable());
                map.put(ThirdConstants.ThirdDetailType.SQLBOT_BOARD.toString(), config.getSqlBotBoardEnable());
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.MAXKB.toString(), config.getMkEnable());
            }
            case TENDER -> {
                TenderThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), TenderThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.TENDER.toString(), config.getTenderEnable());
            }
            case QCC -> {
                QccThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.QCC.toString(), config.getQccEnable());
            }
        }

        return map;
    }

    /**
     * 获取Token
     *
     * @param configDTO 配置DTO
     *
     * @return Token
     */
    private String getToken(ThirdConfigBaseDTO<?> configDTO) {
        ThirdConfigTypeConstants type = ThirdConfigTypeConstants.fromString(configDTO.getType());
        if (type == null) {
            return null;
        }

        switch (type) {
            case WECOM -> {
                WecomThirdConfigRequest weComConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                return tokenService.getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                return tokenService.getDingTalkToken(dingTalkConfig.getAgentId(), dingTalkConfig.getAppSecret());
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                return tokenService.getLarkToken(larkConfig.getAgentId(), larkConfig.getAppSecret());
            }
            case DE -> {
                DeThirdConfigRequest deConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                boolean verify = validDeConfig(deConfig);
                return verify ? "true" : null;
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest sqlBotConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                return tokenService.getSqlBotSrc(sqlBotConfig.getAppSecret()) ? "true" : null;
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest mkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                return tokenService.getMaxKBToken(mkConfig.getMkAddress(), mkConfig.getAppSecret()) ? "true" : null;
            }
            case TENDER -> {
                return tokenService.getTender() ? "true" : null;
            }
            case QCC -> {
                QccThirdConfigRequest qccConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                return tokenService.getQcc(qccConfig.getQccAddress(), qccConfig.getQccAccessKey(), qccConfig.getQccSecretKey()) ? "true" : null;
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 验证De配置
     *
     * @param configDTO 配置请求
     *
     * @return 是否有效
     */
    private boolean validDeConfig(DeThirdConfigRequest configDTO) {
        // 校验url
        boolean verify = tokenService.pingDeUrl(configDTO.getRedirectUrl());
        DataEaseClient dataEaseClient = new DataEaseClient(configDTO);
        if (StringUtils.isNotBlank(configDTO.getDeAccessKey())
                && StringUtils.isNotBlank(configDTO.getDeSecretKey())
                && StringUtils.isNotBlank(configDTO.getRedirectUrl())) {
            // 校验 ak，sk
            verify = verify && dataEaseClient.validate();
        }
        return verify;
    }

    /**
     * 创建新的组织配置
     *
     * @param organizationId 组织ID
     * @param userId         用户ID
     *
     * @return 组织配置
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
     *
     * @param jsonString JSON字符串
     * @param userId     用户ID
     * @param detail     配置详情
     * @param enable     是否启用
     */
    private void updateOrganizationConfigDetail(String jsonString, String userId, OrganizationConfigDetail detail, Boolean enable) {
        detail.setContent(jsonString.getBytes());
        detail.setUpdateTime(System.currentTimeMillis());
        detail.setUpdateUser(userId);
        detail.setEnable(enable);
        organizationConfigDetailBaseMapper.update(detail);
    }

    /**
     * 根据类型获取配置
     *
     * @param type    类型
     * @param details 配置详情列表
     *
     * @return 配置DTO
     */
    private ThirdConfigBaseDTO<?> getConfigurationByType(String type, List<OrganizationConfigDetail> details) {
        return getNormalConfiguration(type, details);
    }

    /**
     * 获取普通配置
     *
     * @param type    类型
     * @param details 配置详情列表
     *
     * @return 配置DTO
     */
    private ThirdConfigBaseDTO<?> getNormalConfiguration(String type, List<OrganizationConfigDetail> details) {
        ThirdConfigBaseDTO<?> configDTO = getThirdConfigurationDTOByType(details, type);
        if (configDTO == null) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }

        return configDTO;
    }

    /**
     * 获取选项DTO
     *
     * @param detail 配置详情
     *
     * @return 选项DTO
     */
    private OptionDTO getOptionDTO(OrganizationConfigDetail detail) {
        OptionDTO option = new OptionDTO();
        String type = detail.getType();

        if (type.contains(ThirdConfigTypeConstants.WECOM.name())) {
            option.setId(ThirdConfigTypeConstants.WECOM.name());
        } else if (type.contains(ThirdConfigTypeConstants.DINGTALK.name())) {
            option.setId(ThirdConfigTypeConstants.DINGTALK.name());
        } else if (type.contains(ThirdConfigTypeConstants.LARK.name())) {
            option.setId(ThirdConfigTypeConstants.LARK.name());
        }

        option.setName(detail.getEnable().toString());
        return option;
    }

    /**
     * 获取启用状态
     *
     * @param configDTO 配置DTO
     *
     * @return 是否启用
     */
    private boolean getEnable(ThirdConfigBaseDTO<?> configDTO) {
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        if (typeConstants == null) {
            throw new GenericException("unsupported.third.type");
        }
        switch (typeConstants) {
            case WECOM -> {
                WecomThirdConfigRequest weComConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                return weComConfig.getStartEnable();
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                return dingTalkConfig.getStartEnable();
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = JSON.MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                return larkConfig.getStartEnable();
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * 获取最近同步类型
     *
     * @param id ID
     *
     * @return 同步类型
     */
    private String getLastSyncType(String id) {
        OrganizationConfig organizationConfig = organizationConfigBaseMapper.selectByPrimaryKey(id);
        if (organizationConfig != null && organizationConfig.isSync() && StringUtils.isNotBlank(organizationConfig.getSyncResource())) {
            return organizationConfig.getSyncResource();
        } else {
            return null;
        }
    }

    /**
     * 同步CorpId
     *
     * @param content   内容
     * @param configDTO 配置DTO
     *
     * @return 是否同步
     */
    private boolean syncCorpId(byte[] content, ThirdConfigBaseDTO<?> configDTO) {
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        if (typeConstants == null) {
            throw new GenericException("unsupported.third.type");
        }
        switch (typeConstants) {
            case WECOM, DINGTALK, LARK -> {
                WecomThirdConfigRequest oldConfig = JSON.parseObject(new String(content), WecomThirdConfigRequest.class);
                WecomThirdConfigRequest config = JSON.MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                return !Strings.CI.equals(oldConfig.getCorpId(), config.getCorpId());
            }
            default -> {
                return false;
            }
        }
    }
}