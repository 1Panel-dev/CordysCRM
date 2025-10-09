package cn.cordys.crm.integration.auth.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.auth.request.AuthSourceEditRequest;
import cn.cordys.crm.integration.auth.response.AuthSourceDTO;
import cn.cordys.crm.integration.auth.response.AuthSourceLogDTO;
import cn.cordys.crm.integration.wecom.dto.WeComConfigurationDTO;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.request.AuthSourceRequest;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class AuthSourceService {

    private static final String ENABLED_LABEL = "开启";
    private static final String DISABLED_LABEL = "关闭";

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;
    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;
    @Resource
    private BaseMapper<OrganizationConfigDetail> organizationConfigDetailBaseMapper;
    @Resource
    private BaseMapper<OrganizationConfig> organizationConfigBaseMapper;

    private static AuthSourceLogDTO getAuthSourceLogDTOAuthDTO(AuthSourceDTO authSourceOld) {
        AuthSourceLogDTO dto = new AuthSourceLogDTO();
        BeanUtils.copyBean(dto, authSourceOld);
        dto.setConfiguration(JSON.parseObject(authSourceOld.getConfiguration(), WeComConfigurationDTO.class));
        dto.setEnable(Boolean.TRUE.equals(authSourceOld.getEnable()) ? ENABLED_LABEL : DISABLED_LABEL);
        return dto;
    }

    private static AuthSourceLogDTO getAuthSourceLogDTOByRequest(AuthSourceEditRequest authSource) {
        AuthSourceLogDTO dto = new AuthSourceLogDTO();
        BeanUtils.copyBean(dto, authSource);
        dto.setConfiguration(JSON.parseObject(authSource.getConfiguration(), WeComConfigurationDTO.class));
        dto.setEnable(BooleanUtils.isTrue(authSource.getEnable()) ? ENABLED_LABEL : DISABLED_LABEL);
        return dto;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public List<OrganizationConfigDetail> list(AuthSourceRequest request) {
        return extOrganizationConfigDetailMapper.getOrganizationConfigDetailList(request);
    }

    public AuthSourceDTO getAuthSource(String id) {
        OrganizationConfigDetail detail = organizationConfigDetailBaseMapper.selectByPrimaryKey(id);
        if (detail == null) {
            throw new GenericException(Translator.get("auth.source.blank"));
        }
        AuthSourceDTO dto = new AuthSourceDTO();
        BeanUtils.copyBean(dto, detail);
        dto.setConfiguration(new String(detail.getContent(), StandardCharsets.UTF_8));
        return dto;
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.ADD, operator = "{#userId}")
    public void addAuthSource(AuthSourceEditRequest authSource, String organizationId, String userId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());

        if (organizationConfig == null) {
            organizationConfig = createOrganizationConfig(organizationId, userId);
        } else {
            checkAuthSource(authSource);
        }

        OrganizationConfigDetail detail = buildDetailForInsert(authSource, organizationConfig.getId(), userId);
        organizationConfigDetailBaseMapper.insert(detail);

        AuthSourceLogDTO logDTO = getAuthSourceLogDTOByRequest(authSource);
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(null)
                .resourceName(authSource.getName())
                .resourceId(detail.getId())
                .modifiedValue(logDTO)
                .build());
    }

    public void checkAuthSource(AuthSourceEditRequest authSource) {
        if (StringUtils.isBlank(authSource.getId())) {
            authSource.setId(null);
        }
        int repeatDetails = extOrganizationConfigDetailMapper.getRepeatDetails(authSource.getId(), authSource.getName());
        if (repeatDetails > 0) {
            throw new GenericException(Translator.get("auth.source.name.exists"));
        }
        if (StringUtils.isBlank(authSource.getConfiguration())) {
            throw new GenericException(Translator.get("auth.source.configuration.blank"));
        }
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.DELETE, resourceId = "{#id}")
    public void deleteAuthSource(String id) {
        OrganizationConfigDetail origin = organizationConfigDetailBaseMapper.selectByPrimaryKey(id);
        organizationConfigDetailBaseMapper.deleteByPrimaryKey(id);
        // 保持原逻辑（可能为 null 时抛出 NPE）
        OperationLogContext.setResourceName(origin.getName());
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.UPDATE, operator = "{#userId}")
    public void updateAuthSource(AuthSourceEditRequest authSource, String userId) {
        checkAuthSource(authSource);
        AuthSourceDTO old = getAuthSource(authSource.getId());

        OrganizationConfigDetail record = new OrganizationConfigDetail();
        record.setId(authSource.getId());
        record.setDescription(authSource.getDescription());
        record.setName(authSource.getName());
        record.setEnable(authSource.getEnable());
        record.setContent(authSource.getConfiguration().getBytes(StandardCharsets.UTF_8));
        record.setUpdateTime(now());
        record.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(record);

        AuthSourceLogDTO newLog = getAuthSourceLogDTOByRequest(authSource);
        AuthSourceLogDTO oldLog = getAuthSourceLogDTOAuthDTO(old);
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldLog)
                .resourceName(authSource.getName())
                .resourceId(authSource.getId())
                .modifiedValue(newLog)
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.UPDATE, resourceId = "{#id}", operator = "{#userId}")
    public void updateStatus(String id, Boolean status) {
        AuthSourceDTO old = getAuthSource(id);
        OrganizationConfigDetail source = organizationConfigDetailBaseMapper.selectByPrimaryKey(id);

        if (BooleanUtils.isTrue(status)) {
            extOrganizationConfigDetailMapper.updateStatus(id, false, source.getType(), source.getConfigId());
        }

        OrganizationConfigDetail record = new OrganizationConfigDetail();
        record.setId(id);
        record.setEnable(BooleanUtils.toBooleanDefaultIfNull(status, false));
        record.setUpdateTime(now());
        organizationConfigDetailBaseMapper.update(record);

        AuthSourceLogDTO oldLog = getAuthSourceLogDTOAuthDTO(old);
        AuthSourceLogDTO newLog = new AuthSourceLogDTO();
        BeanUtils.copyBean(newLog, oldLog);
        newLog.setEnable(BooleanUtils.isTrue(status) ? ENABLED_LABEL : DISABLED_LABEL);

        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldLog)
                .resourceName(source.getName())
                .resourceId(source.getId())
                .modifiedValue(newLog)
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.UPDATE, resourceId = "{#id}", operator = "{#userId}")
    public void updateName(String id, String name, String userId) {
        int repeatDetails = extOrganizationConfigDetailMapper.getRepeatDetails(id, name);
        if (repeatDetails > 0) {
            throw new GenericException(Translator.get("auth.source.name.exists"));
        }

        AuthSourceDTO old = getAuthSource(id);

        OrganizationConfigDetail record = new OrganizationConfigDetail();
        record.setId(id);
        record.setName(name);
        record.setUpdateTime(now());
        record.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(record);

        AuthSourceLogDTO oldLog = getAuthSourceLogDTOAuthDTO(old);
        AuthSourceLogDTO newLog = new AuthSourceLogDTO();
        BeanUtils.copyBean(newLog, oldLog);
        newLog.setName(name);

        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldLog)
                .resourceName(name)
                .resourceId(id)
                .modifiedValue(newLog)
                .build());
    }

    public List<String> getEnableAuthSourceTypeList(String organizationId) {
        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (config == null) {
            throw new GenericException(Translator.get("auth.setting.no.exists"));
        }
        List<OrganizationConfigDetail> details =
                extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(config.getId(), null);
        return details.stream().map(OrganizationConfigDetail::getType).toList();
    }

    // helpers

    public AuthSourceDTO getAuthSourceByType(String organizationId, String type) {
        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (config == null) {
            throw new GenericException(Translator.get("auth.setting.no.exists"));
        }
        List<OrganizationConfigDetail> details =
                extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(config.getId(), type);
        if (CollectionUtils.isEmpty(details)) {
            throw new GenericException(Translator.get("auth.source.blank"));
        }
        OrganizationConfigDetail detail = details.getFirst();
        AuthSourceDTO dto = new AuthSourceDTO();
        BeanUtils.copyBean(dto, detail);
        dto.setConfiguration(new String(detail.getContent(), StandardCharsets.UTF_8));
        return dto;
    }

    private OrganizationConfig createOrganizationConfig(String organizationId, String userId) {
        OrganizationConfig config = new OrganizationConfig();
        config.setId(IDGenerator.nextStr());
        config.setOrganizationId(organizationId);
        config.setType(OrganizationConfigConstants.ConfigType.AUTH.name());
        config.setCreateTime(now());
        config.setUpdateTime(now());
        config.setCreateUser(userId);
        config.setUpdateUser(userId);
        organizationConfigBaseMapper.insert(config);
        return config;
    }

    private OrganizationConfigDetail buildDetailForInsert(AuthSourceEditRequest authSource, String configId, String userId) {
        OrganizationConfigDetail detail = new OrganizationConfigDetail();
        detail.setId(IDGenerator.nextStr());
        detail.setName(authSource.getName());
        detail.setDescription(authSource.getDescription());
        detail.setContent(authSource.getConfiguration().getBytes(StandardCharsets.UTF_8));
        detail.setCreateTime(now());
        detail.setUpdateTime(now());
        detail.setCreateUser(userId);
        detail.setUpdateUser(userId);
        detail.setConfigId(configId);
        detail.setType(authSource.getType());
        detail.setEnable(authSource.getEnable());
        return detail;
    }
}