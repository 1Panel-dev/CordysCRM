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
import cn.cordys.crm.integration.sso.service.AgentService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private AgentService agentService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 获取同步的组织配置
     */
    public List<ThirdConfigBaseDTO> getThirdConfig(String organizationId) {
        List<OrganizationConfigDetail> organizationConfigDetails = initConfig(organizationId, SessionUtils.getUserId());


        // 构建第三方配置列表
        List<ThirdConfigBaseDTO> configDTOs = new ArrayList<>();
        buildDetailData(organizationConfigDetails, configDTOs);

        return configDTOs;
    }

    private void buildDetailData(List<OrganizationConfigDetail> details, List<ThirdConfigBaseDTO> configDTOs) {
        ThirdConfigBaseDTO sqlBotConfig = new ThirdConfigBaseDTO();
        SqlBotThirdConfigRequest sqlBotDTO = new SqlBotThirdConfigRequest();
        for (OrganizationConfigDetail detail : details) {
            ThirdConfigBaseDTO dto = new ThirdConfigBaseDTO();
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.WECOM_SYNC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                WecomThirdConfigRequest wecomThirdConfigRequest = new WecomThirdConfigRequest();
                if (dto.getConfig() == null) {
                    wecomThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), WecomThirdConfigRequest.class);
                } else {
                    wecomThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), WecomThirdConfigRequest.class);
                }
                wecomThirdConfigRequest.setStartEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.WECOM.name());
                dto.setConfig(wecomThirdConfigRequest);
                configDTOs.add(dto);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.DINGTALK_SYNC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                DingTalkThirdConfigRequest dingTalkThirdConfigRequest = new DingTalkThirdConfigRequest();
                if (dto.getConfig() == null) {
                    dingTalkThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), DingTalkThirdConfigRequest.class);
                } else {
                    dingTalkThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), DingTalkThirdConfigRequest.class);
                }
                dingTalkThirdConfigRequest.setStartEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.DINGTALK.name());
                dto.setConfig(dingTalkThirdConfigRequest);
                configDTOs.add(dto);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.LARK_SYNC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                LarkThirdConfigRequest larkThirdConfigRequest = new LarkThirdConfigRequest();
                if (dto.getConfig() == null) {
                    larkThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), LarkThirdConfigRequest.class);
                } else {
                    larkThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), LarkThirdConfigRequest.class);
                }
                larkThirdConfigRequest.setStartEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.LARK.name());
                dto.setConfig(larkThirdConfigRequest);
                configDTOs.add(dto);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.DE_BOARD.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                DeThirdConfigRequest deThirdConfigRequest = new DeThirdConfigRequest();
                if (dto.getConfig() == null) {
                    deThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), DeThirdConfigRequest.class);
                } else {
                    deThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), DeThirdConfigRequest.class);
                }
                deThirdConfigRequest.setDeBoardEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.DE.name());
                dto.setConfig(deThirdConfigRequest);
                configDTOs.add(dto);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.SQLBOT_CHAT.name())
                    || Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.SQLBOT_BOARD.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                SqlBotThirdConfigRequest sqlBotThirdConfigRequest = new SqlBotThirdConfigRequest();
                if (dto.getConfig() == null) {
                    sqlBotThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), SqlBotThirdConfigRequest.class);
                } else {
                    sqlBotThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), SqlBotThirdConfigRequest.class);
                }
                if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.SQLBOT_CHAT.name())) {
                    sqlBotDTO.setSqlBotChatEnable(detail.getEnable());
                } else {
                    sqlBotDTO.setSqlBotBoardEnable(detail.getEnable());
                }
                sqlBotDTO.setAppSecret(sqlBotThirdConfigRequest.getAppSecret());
                //dto.setType(ThirdConfigTypeConstants.SQLBOT.name());
                sqlBotConfig.setType(ThirdConfigTypeConstants.SQLBOT.name());
                sqlBotConfig.setVerify(dto.getVerify());
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.MAXKB.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                MaxKBThirdConfigRequest maxKBThirdConfigRequest = new MaxKBThirdConfigRequest();
                if (dto.getConfig() == null) {
                    maxKBThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), MaxKBThirdConfigRequest.class);
                } else {
                    maxKBThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), MaxKBThirdConfigRequest.class);
                }
                maxKBThirdConfigRequest.setMkEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.MAXKB.name());
                dto.setConfig(maxKBThirdConfigRequest);
                configDTOs.add(dto);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.TENDER.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                TenderThirdConfigRequest tenderThirdConfigRequest = new TenderThirdConfigRequest();
                if (dto.getConfig() == null) {
                    tenderThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), TenderThirdConfigRequest.class);
                } else {
                    tenderThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), TenderThirdConfigRequest.class);
                }
                tenderThirdConfigRequest.setTenderEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.TENDER.name());
                dto.setConfig(tenderThirdConfigRequest);
                configDTOs.add(dto);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.QCC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                QccThirdConfigRequest qccThirdConfigRequest = new QccThirdConfigRequest();
                if (dto.getConfig() == null) {
                    qccThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), QccThirdConfigRequest.class);
                } else {
                    qccThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), QccThirdConfigRequest.class);
                }
                qccThirdConfigRequest.setQccEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.QCC.name());
                dto.setConfig(qccThirdConfigRequest);
                configDTOs.add(dto);
            }

        }

        if (sqlBotConfig.getType() != null) {
            sqlBotConfig.setConfig(sqlBotDTO);
            configDTOs.add(sqlBotConfig);
        }
    }

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
     * 如果配置不为空，添加到列表中
     */
    private void addConfigIfExists(List<ThirdConfigBaseDTO> configs, ThirdConfigBaseDTO config) {
        if (config != null) {
            configs.add(config);
        }
    }

    /**
     * 判断已查出的数据类型，不符合类型直接返回null
     *
     * @param organizationConfigDetails 已查出的数据
     * @param type                      类型
     * @return ThirdConfigBaseDTO
     */
    private ThirdConfigBaseDTO getThirdConfigurationDTOByType(List<OrganizationConfigDetail> organizationConfigDetails, String type) {
        List<OrganizationConfigDetail> detailList = organizationConfigDetails.stream()
                .filter(t -> t.getType().contains(type))
                .toList();

        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        ThirdConfigBaseDTO dto = new ThirdConfigBaseDTO();
        for (OrganizationConfigDetail detail : detailList) {
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.WECOM_SYNC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                WecomThirdConfigRequest wecomThirdConfigRequest = new WecomThirdConfigRequest();
                if (dto.getConfig() == null) {
                    wecomThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), WecomThirdConfigRequest.class);
                } else {
                    wecomThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), WecomThirdConfigRequest.class);
                }
                wecomThirdConfigRequest.setStartEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.WECOM.name());
                dto.setConfig(wecomThirdConfigRequest);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.DINGTALK_SYNC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                DingTalkThirdConfigRequest dingTalkThirdConfigRequest = new DingTalkThirdConfigRequest();
                if (dto.getConfig() == null) {
                    dingTalkThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), DingTalkThirdConfigRequest.class);
                } else {
                    dingTalkThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), DingTalkThirdConfigRequest.class);
                }
                dingTalkThirdConfigRequest.setStartEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.DINGTALK.name());
                dto.setConfig(dingTalkThirdConfigRequest);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.LARK_SYNC.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                LarkThirdConfigRequest larkThirdConfigRequest = new LarkThirdConfigRequest();
                if (dto.getConfig() == null) {
                    larkThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), LarkThirdConfigRequest.class);
                } else {
                    larkThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), LarkThirdConfigRequest.class);
                }
                larkThirdConfigRequest.setStartEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.LARK.name());
                dto.setConfig(larkThirdConfigRequest);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.MAXKB.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                MaxKBThirdConfigRequest maxKBThirdConfigRequest = new MaxKBThirdConfigRequest();
                if (dto.getConfig() == null) {
                    maxKBThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), MaxKBThirdConfigRequest.class);
                } else {
                    maxKBThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), MaxKBThirdConfigRequest.class);
                }
                maxKBThirdConfigRequest.setMkEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.MAXKB.name());
                dto.setConfig(maxKBThirdConfigRequest);
            }
            if (Strings.CI.equals(detail.getType(), ThirdConstants.ThirdDetailType.TENDER.name())) {
                dto = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                TenderThirdConfigRequest tenderThirdConfigRequest = new TenderThirdConfigRequest();
                if (dto.getConfig() == null) {
                    tenderThirdConfigRequest = JSON.parseObject(new String(detail.getContent()), TenderThirdConfigRequest.class);
                } else {
                    tenderThirdConfigRequest = MAPPER.convertValue(dto.getConfig(), TenderThirdConfigRequest.class);
                }
                tenderThirdConfigRequest.setTenderEnable(detail.getEnable());
                dto.setType(ThirdConfigTypeConstants.TENDER.name());
                dto.setConfig(tenderThirdConfigRequest);
            }
        }


        return dto;

    }

    /**
     * 编辑配置
     */
    @OperationLog(module = LogModule.SYSTEM_BUSINESS_THIRD, type = LogType.UPDATE, operator = "{#userId}")
    public void editThirdConfig(ThirdConfigBaseDTO configDTO, String organizationId, String userId) {
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

    private boolean getEnable(ThirdConfigBaseDTO configDTO) {
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        switch (typeConstants) {
            case WECOM -> {
                WecomThirdConfigRequest weComConfig = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                return weComConfig.getStartEnable();
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                return dingTalkConfig.getStartEnable();
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                return larkConfig.getStartEnable();
            }
            default -> {
                return false;
            }
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
            ThirdConfigBaseDTO configDTO,
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
    private void addIntegrationDetail(ThirdConfigBaseDTO configDTO, String userId, String token, List<String> types, OrganizationConfig organizationConfig, Map<String, Boolean> typeEnableMap) {
        String jsonContent = null;
        Boolean verify = false;
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        switch (typeConstants) {
            case WECOM -> {
                WecomThirdConfigRequest wecomConfig = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                if (wecomConfig.getStartEnable()) {
                    verifyWeCom(wecomConfig.getAgentId(), token, configDTO);
                }
                configDTO.setConfig(wecomConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(wecomConfig)));
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                if (dingTalkConfig.getStartEnable()) {
                    verifyDingTalk(token, configDTO);
                }
                configDTO.setConfig(dingTalkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(dingTalkConfig)));
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                if (larkConfig.getStartEnable()) {
                    verifyLark(token, configDTO);
                }
                configDTO.setConfig(larkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(larkConfig)));
            }
            case DE -> {
                DeThirdConfigRequest deConfig = MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                if (BooleanUtils.isTrue(deConfig.getDeBoardEnable())) {
                    verifyDe(token, configDTO);
                }
                configDTO.setConfig(deConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(deConfig)));
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest sqlBotConfig = MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                if (sqlBotConfig.getSqlBotBoardEnable() || sqlBotConfig.getSqlBotChatEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(sqlBotConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(sqlBotConfig)));
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest mkConfig = MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                if (mkConfig.getMkEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(mkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                verify = configDTO.getVerify();
                addLog(new HashMap<>(), configDTO, null, JSON.parseToMap(JSON.toJSONString(mkConfig)));
            }
            case TENDER -> {
                TenderThirdConfigRequest tenderConfig = MAPPER.convertValue(configDTO.getConfig(), TenderThirdConfigRequest.class);
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
                QccThirdConfigRequest qccConfig = MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
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
     */
    private void handleExistingConfigDetails(
            ThirdConfigBaseDTO configDTO,
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

    private boolean syncCorpId(byte[] content, ThirdConfigBaseDTO configDTO) {
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        switch (typeConstants) {
            case WECOM, DINGTALK, LARK -> {
                WecomThirdConfigRequest oldConfig = JSON.parseObject(new String(content), WecomThirdConfigRequest.class);
                WecomThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                return !Strings.CI.equals(oldConfig.getCorpId(), config.getCorpId());
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * 更新已存在的配置详情
     */
    private void updateExistingDetail(
            ThirdConfigBaseDTO configDTO,
            String userId,
            String token,
            OrganizationConfigDetail detail,
            Boolean enable,
            String id) {

        updateIntegrationDetail(configDTO, userId, token, detail, enable, id);
    }

    /**
     * 封装了各种 `update...` 方法的统一入口
     */
    private void updateIntegrationDetail(
            ThirdConfigBaseDTO configDTO,
            String userId,
            String token,
            OrganizationConfigDetail detail,
            Boolean enable,
            String id) {

        String type = configDTO.getType();
        String jsonContent = null;
        boolean isVerified;
        String detailType = detail.getType();
        boolean openEnable = false;

        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(configDTO.getType());
        switch (typeConstants) {
            case WECOM -> {
                WecomThirdConfigRequest wecomConfig = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                if (wecomConfig.getStartEnable()) {
                    verifyWeCom(wecomConfig.getAgentId(), token, configDTO);
                }
                configDTO.setConfig(wecomConfig);
                jsonContent = JSON.toJSONString(configDTO);
                isVerified = configDTO.getVerify() != null && configDTO.getVerify();
                openEnable = isVerified && enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                WecomThirdConfigRequest oldConfig = new WecomThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), WecomThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), WecomThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(wecomConfig)));
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                if (dingTalkConfig.getStartEnable()) {
                    verifyDingTalk(token, configDTO);
                }
                configDTO.setConfig(dingTalkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                isVerified = configDTO.getVerify() != null && configDTO.getVerify();
                openEnable = isVerified && enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                DingTalkThirdConfigRequest oldConfig = new DingTalkThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), DingTalkThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), DingTalkThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(dingTalkConfig)));
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                if (larkConfig.getStartEnable()) {
                    verifyLark(token, configDTO);
                }
                configDTO.setConfig(larkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                isVerified = configDTO.getVerify() != null && configDTO.getVerify();
                openEnable = isVerified && enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                LarkThirdConfigRequest oldConfig = new LarkThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), LarkThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), LarkThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(larkConfig)));
            }
            case DE -> {
                DeThirdConfigRequest deConfig = MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                if (BooleanUtils.isTrue(deConfig.getDeBoardEnable())) {
                    verifyDe(token, configDTO);
                }
                configDTO.setConfig(deConfig);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                DeThirdConfigRequest oldConfig = new DeThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), DeThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), DeThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(deConfig)));
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest sqlBotConfig = MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                if (sqlBotConfig.getSqlBotBoardEnable() || sqlBotConfig.getSqlBotChatEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(sqlBotConfig);
                jsonContent = JSON.toJSONString(configDTO);
                isVerified = configDTO.getVerify() != null && configDTO.getVerify();
                openEnable = isVerified && enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                SqlBotThirdConfigRequest oldConfig = new SqlBotThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), SqlBotThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), SqlBotThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(sqlBotConfig)));
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest mkConfig = MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                if (mkConfig.getMkEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(mkConfig);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                MaxKBThirdConfigRequest oldConfig = new MaxKBThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), MaxKBThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), MaxKBThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(mkConfig)));
            }
            case TENDER -> {
                TenderThirdConfigRequest tenderConfig = MAPPER.convertValue(configDTO.getConfig(), TenderThirdConfigRequest.class);
                tenderConfig.setTenderAddress(TenderApiPaths.TENDER_API);
                if (tenderConfig.getTenderEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(tenderConfig);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                TenderThirdConfigRequest oldConfig = new TenderThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), TenderThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), TenderThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(tenderConfig)));
            }
            case QCC -> {
                QccThirdConfigRequest qccConfig = MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                if (qccConfig.getQccEnable()) {
                    verifyToken(token, configDTO);
                }
                configDTO.setConfig(qccConfig);
                jsonContent = JSON.toJSONString(configDTO);
                openEnable = enable;

                ThirdConfigBaseDTO config = JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class);
                QccThirdConfigRequest oldConfig = new QccThirdConfigRequest();
                if (config.getConfig() == null) {
                    oldConfig = JSON.parseObject(new String(detail.getContent()), QccThirdConfigRequest.class);
                } else {
                    oldConfig = MAPPER.convertValue(config.getConfig(), QccThirdConfigRequest.class);
                }
                addLog(JSON.parseToMap(JSON.toJSONString(oldConfig)), configDTO, id, JSON.parseToMap(JSON.toJSONString(qccConfig)));
            }
        }

        updateOrganizationConfigDetail(jsonContent, userId, detail, openEnable);

    }

    private void addLog(Map<String, Object> oldMap, ThirdConfigBaseDTO configDTO, String id, Map<String, Object> newMap) {
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


    private void verifyDe(String token, ThirdConfigBaseDTO deConfig) {
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

    private void verifyWeCom(String agentId, String token, ThirdConfigBaseDTO weComConfig) {
        if (StringUtils.isNotBlank(token)) {
            // 验证应用ID
            Boolean weComAgent = agentService.getWeComAgent(token, agentId);
            weComConfig.setVerify(weComAgent != null && weComAgent);
        } else {
            weComConfig.setVerify(false);
        }
    }

    private void verifyDingTalk(String token, ThirdConfigBaseDTO dingTalkConfigDetailDTO) {
        dingTalkConfigDetailDTO.setVerify(StringUtils.isNotBlank(token));
    }

    private void verifyLark(String token, ThirdConfigBaseDTO larkConfigDetailDTO) {
        larkConfigDetailDTO.setVerify(StringUtils.isNotBlank(token));
    }

    private void verifyToken(String token, ThirdConfigBaseDTO config) {
        config.setVerify(StringUtils.isNotBlank(token) && Strings.CI.equals(token, "true"));
    }

    /**
     * 根据配置类型获取详情类型列表
     */
    private List<String> getDetailTypes(String type) {
        ThirdConfigTypeConstants typeConstants = ThirdConfigTypeConstants.fromString(type);
        List<String> result = switch (typeConstants) {
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
        return result;
    }

    /**
     * 获取类型启用状态映射
     */
    private Map<String, Boolean> getTypeEnableMap(ThirdConfigBaseDTO configDTO) {
        Map<String, Boolean> map = new HashMap<>();
        ThirdConfigTypeConstants type = ThirdConfigTypeConstants.fromString(configDTO.getType());
        switch (type) {
            case WECOM -> {
                WecomThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.WECOM_SYNC.toString(), config.getStartEnable());
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString(), config.getStartEnable());
            }
            case LARK -> {
                LarkThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.LARK_SYNC.toString(), config.getStartEnable());
            }
            case DE -> {
                DeThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.DE_BOARD.toString(), config.getDeBoardEnable());
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.SQLBOT_CHAT.toString(), config.getSqlBotChatEnable());
                map.put(ThirdConstants.ThirdDetailType.SQLBOT_BOARD.toString(), config.getSqlBotBoardEnable());
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.MAXKB.toString(), config.getMkEnable());
            }
            case TENDER -> {
                TenderThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), TenderThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.TENDER.toString(), config.getTenderEnable());
            }
            case QCC -> {
                QccThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                map.put(ThirdConstants.ThirdDetailType.QCC.toString(), config.getQccEnable());
            }
        }

        return map;
    }

    /**
     * 获取验证所需的token
     */
    private String getToken(ThirdConfigBaseDTO configDTO) {
        ThirdConfigTypeConstants type = ThirdConfigTypeConstants.fromString(configDTO.getType());
        switch (type) {
            case WECOM -> {
                WecomThirdConfigRequest weComConfig = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
                return tokenService.getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = MAPPER.convertValue(configDTO.getConfig(), DingTalkThirdConfigRequest.class);
                return tokenService.getDingTalkToken(dingTalkConfig.getAgentId(), dingTalkConfig.getAppSecret());
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = MAPPER.convertValue(configDTO.getConfig(), LarkThirdConfigRequest.class);
                return tokenService.getLarkToken(larkConfig.getAgentId(), larkConfig.getAppSecret());
            }
            case DE -> {
                DeThirdConfigRequest deConfig = MAPPER.convertValue(configDTO.getConfig(), DeThirdConfigRequest.class);
                boolean verify = validDeConfig(deConfig);
                return verify ? "true" : null;
            }
            case SQLBOT -> {
                SqlBotThirdConfigRequest sqlBotConfig = MAPPER.convertValue(configDTO.getConfig(), SqlBotThirdConfigRequest.class);
                return tokenService.getSqlBotSrc(sqlBotConfig.getAppSecret()) ? "true" : null;
            }
            case MAXKB -> {
                MaxKBThirdConfigRequest mkConfig = MAPPER.convertValue(configDTO.getConfig(), MaxKBThirdConfigRequest.class);
                return tokenService.getMaxKBToken(mkConfig.getMkAddress(), mkConfig.getAppSecret()) ? "true" : null;
            }
            case TENDER -> {
                return tokenService.getTender() ? "true" : null;
            }
            case QCC -> {
                QccThirdConfigRequest qccConfig = MAPPER.convertValue(configDTO.getConfig(), QccThirdConfigRequest.class);
                return tokenService.getQcc(qccConfig.getQccAddress(), qccConfig.getQccAccessKey(), qccConfig.getQccSecretKey()) ? "true" : null;
            }
        }
        return null;
    }

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
     * 测试连接
     */
    public boolean testConnection(ThirdConfigBaseDTO configDTO) {
        String token = getToken(configDTO);
        if (ThirdConfigTypeConstants.WECOM.name().equals(configDTO.getType()) && StringUtils.isNotBlank(token)) {
            WecomThirdConfigRequest config = MAPPER.convertValue(configDTO.getConfig(), WecomThirdConfigRequest.class);
            Boolean weComAgent = agentService.getWeComAgent(token, config.getAgentId());
            if (weComAgent == null || !weComAgent) {
                token = null;
            }
        }
        return StringUtils.isNotBlank(token);
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
    public ThirdConfigBaseDTO getThirdConfigForPublic(String type, String orgId) {
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
        ThirdConfigBaseDTO configDTO = getConfigurationByType(type, details);


        return configDTO;
    }

    /**
     * 根据类型获取配置
     */
    private ThirdConfigBaseDTO getConfigurationByType(String type, List<OrganizationConfigDetail> details) {
        return getNormalConfiguration(type, details);
    }

    /**
     * 获取普通配置
     */
    private ThirdConfigBaseDTO getNormalConfiguration(String type, List<OrganizationConfigDetail> details) {
        ThirdConfigBaseDTO configDTO = getThirdConfigurationDTOByType(details, type);
        if (configDTO == null) {
            throw new GenericException(Translator.get("third.config.not.exist"));
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
     * 配置详情转换为选项
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

    public OrganizationConfig getLatestSyncResource(String organizationId) {
        return extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
    }

    public ThirdConfigBaseDTO getApplicationConfig(String organizationId, String userId, String type) {
        List<OrganizationConfigDetail> organizationConfigDetails = initConfig(organizationId, userId);
        return getThirdConfigurationDTOByType(organizationConfigDetails, type);

    }
}