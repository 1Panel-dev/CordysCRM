package io.cordys.crm.integration.auth.service;


import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.request.AuthSourceRequest;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.crm.integration.auth.request.AuthSourceEditRequest;
import io.cordys.crm.integration.auth.response.AuthSourceDTO;
import io.cordys.mybatis.BaseMapper;
import io.cordys.crm.integration.auth.response.AuthSourceLogDTO;
import io.cordys.crm.integration.wecom.dto.WeComConfigurationDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class AuthSourceService {
    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Resource
    private BaseMapper<OrganizationConfigDetail> organizationConfigDetailBaseMapper;

    @Resource
    private BaseMapper<OrganizationConfig> organizationConfigBaseMapper;

    public List<OrganizationConfigDetail> list(AuthSourceRequest request) {
        return extOrganizationConfigDetailMapper.getOrganizationConfigDetailList(request);
    }

    public AuthSourceDTO getAuthSource(String id) {
        OrganizationConfigDetail source = organizationConfigDetailBaseMapper.selectByPrimaryKey(id);
        if (source == null) {
            throw new GenericException(Translator.get("auth.source.blank"));
        }
        AuthSourceDTO authSourceDTO = new AuthSourceDTO();
        BeanUtils.copyBean(authSourceDTO, source);
        authSourceDTO.setConfiguration(new String(source.getContent(), StandardCharsets.UTF_8));
        return authSourceDTO;
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.ADD, operator = "{#userId}")
    public void addAuthSource(AuthSourceEditRequest authSource, String organizationId, String userId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (organizationConfig == null) {
            organizationConfig = new OrganizationConfig();
            organizationConfig.setId(IDGenerator.nextStr());
            organizationConfig.setOrganizationId(organizationId);
            organizationConfig.setType(OrganizationConfigConstants.ConfigType.AUTH.name());
            organizationConfig.setCreateTime(System.currentTimeMillis());
            organizationConfig.setUpdateTime(System.currentTimeMillis());
            organizationConfig.setCreateUser(userId);
            organizationConfig.setUpdateUser(userId);
            organizationConfigBaseMapper.insert(organizationConfig);
        } else {
            checkAuthSource(authSource);
        }

        OrganizationConfigDetail organizationConfigDetail;
        organizationConfigDetail = new OrganizationConfigDetail();
        organizationConfigDetail.setId(IDGenerator.nextStr());
        organizationConfigDetail.setName(authSource.getName());
        organizationConfigDetail.setDescription(authSource.getDescription());
        organizationConfigDetail.setContent(authSource.getConfiguration().getBytes(StandardCharsets.UTF_8));
        organizationConfigDetail.setCreateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setCreateUser(userId);
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetail.setConfigId(organizationConfig.getId());
        organizationConfigDetail.setType(authSource.getType());
        organizationConfigDetail.setEnable(authSource.getEnable());
        organizationConfigDetailBaseMapper.insert(organizationConfigDetail);
        // 添加日志上下文
        AuthSourceLogDTO authSourceLogDTO = getAuthSourceLogDTOByRequest(authSource);
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(null)
                .resourceName(authSource.getName())
                .resourceId(organizationConfigDetail.getId())
                .modifiedValue(authSourceLogDTO)
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
        OrganizationConfigDetail originCustomerPool = organizationConfigDetailBaseMapper.selectByPrimaryKey(id);

        organizationConfigDetailBaseMapper.deleteByPrimaryKey(id);
        // 设置日志操作对象
        OperationLogContext.setResourceName(originCustomerPool.getName());

    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.UPDATE, operator = "{#userId}")
    public void updateAuthSource(AuthSourceEditRequest authSource, String userId) {
        checkAuthSource(authSource);
        AuthSourceDTO authSourceOld = getAuthSource(authSource.getId());
        OrganizationConfigDetail organizationConfigDetail = new OrganizationConfigDetail();
        organizationConfigDetail.setId(authSource.getId());
        organizationConfigDetail.setDescription(authSource.getDescription());
        organizationConfigDetail.setName(authSource.getName());
        organizationConfigDetail.setEnable(authSource.getEnable());
        organizationConfigDetail.setContent(authSource.getConfiguration().getBytes(StandardCharsets.UTF_8));
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        // 添加日志上下文
        AuthSourceLogDTO authSourceDTO = getAuthSourceLogDTOByRequest(authSource);
        // 旧的认证源日志对象
        AuthSourceLogDTO authSourceOldDTO = getAuthSourceLogDTOAuthDTO(authSourceOld);
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(authSourceOldDTO)
                .resourceName(authSource.getName())
                .resourceId(authSource.getId())
                .modifiedValue(authSourceDTO)
                .build());

    }

    @NotNull
    private static AuthSourceLogDTO getAuthSourceLogDTOAuthDTO(AuthSourceDTO authSourceOld) {
        AuthSourceLogDTO authSourceOldDTO = new AuthSourceLogDTO();
        BeanUtils.copyBean(authSourceOldDTO, authSourceOld);
        authSourceOldDTO.setConfiguration(JSON.parseObject(authSourceOld.getConfiguration(), WeComConfigurationDTO.class));
        if (authSourceOld.getEnable()) {
            authSourceOldDTO.setEnable("开启");
        } else {
            authSourceOldDTO.setEnable("关闭");
        }
        return authSourceOldDTO;
    }

    @NotNull
    private static AuthSourceLogDTO getAuthSourceLogDTOByRequest(AuthSourceEditRequest authSource) {
        AuthSourceLogDTO authSourceDTO = new AuthSourceLogDTO();
        BeanUtils.copyBean(authSourceDTO, authSource);
        authSourceDTO.setConfiguration(JSON.parseObject(authSource.getConfiguration(), WeComConfigurationDTO.class));
        if (BooleanUtils.isTrue(authSource.getEnable())) {
            authSourceDTO.setEnable("开启");
        } else {
            authSourceDTO.setEnable("关闭");
        }
        return authSourceDTO;
    }

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.UPDATE, resourceId = "{#id}", operator = "{#userId}")
    public void updateStatus(String id, Boolean status) {
        AuthSourceDTO authSourceOld = getAuthSource(id);
        OrganizationConfigDetail source = organizationConfigDetailBaseMapper.selectByPrimaryKey(id);
        if (BooleanUtils.isTrue(status)) {
            extOrganizationConfigDetailMapper.updateStatus(id, false, source.getType(), source.getConfigId());
        }
        OrganizationConfigDetail record = new OrganizationConfigDetail();
        record.setId(id);
        record.setEnable(BooleanUtils.toBooleanDefaultIfNull(status, false));
        record.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetailBaseMapper.update(record);
        // 添加日志上下文
        AuthSourceLogDTO authSourceOldDTO = getAuthSourceLogDTOAuthDTO(authSourceOld);
        AuthSourceLogDTO authSourceDTO = new AuthSourceLogDTO();
        BeanUtils.copyBean(authSourceDTO, authSourceOldDTO);
        if (BooleanUtils.isTrue(status)) {
            authSourceDTO.setEnable("开启");
        } else {
            authSourceDTO.setEnable("关闭");
        }
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(authSourceOldDTO)
                .resourceName(source.getName())
                .resourceId(source.getId())
                .modifiedValue(authSourceDTO)
                .build());

    }


    @OperationLog(module = LogModule.SYSTEM_BUSINESS_AUTH, type = LogType.UPDATE, resourceId = "{#id}", operator = "{#userId}")
    public void updateName(String id, String name, String userId) {
        int repeatDetails = extOrganizationConfigDetailMapper.getRepeatDetails(id, name);
        if (repeatDetails > 0) {
            throw new GenericException(Translator.get("auth.source.name.exists"));
        }
        AuthSourceDTO authSourceOld = getAuthSource(id);
        OrganizationConfigDetail organizationConfigDetail = new OrganizationConfigDetail();
        organizationConfigDetail.setId(id);
        organizationConfigDetail.setName(name);
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        // 添加日志上下文
        // 旧的认证源日志对象
        AuthSourceLogDTO authSourceOldDTO = getAuthSourceLogDTOAuthDTO(authSourceOld);
        AuthSourceLogDTO authSourceDTO = new AuthSourceLogDTO();
        BeanUtils.copyBean(authSourceDTO, authSourceOldDTO);
        authSourceDTO.setName(name);
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(authSourceOldDTO)
                .resourceName(name)
                .resourceId(id)
                .modifiedValue(authSourceDTO)
                .build());

    }

    public List<String> getEnableAuthSourceTypeList(String organizationId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (organizationConfig == null) {
            throw new GenericException(Translator.get("auth.setting.no.exists"));
        }
        List<OrganizationConfigDetail> enableOrganizationConfigDetails = extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(organizationConfig.getId(), null);
        return enableOrganizationConfigDetails.stream().map(OrganizationConfigDetail::getType).toList();
    }

    public AuthSourceDTO getAuthSourceByType(String organizationId, String type) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (organizationConfig == null) {
            throw new GenericException(Translator.get("auth.setting.no.exists"));
        }
        List<OrganizationConfigDetail> enableOrganizationConfigDetails = extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(organizationConfig.getId(), type);
        if (CollectionUtils.isEmpty(enableOrganizationConfigDetails)) {
            throw new GenericException(Translator.get("auth.source.blank"));
        }
        OrganizationConfigDetail organizationConfigDetail = enableOrganizationConfigDetails.getFirst();
        AuthSourceDTO authSourceDTO = new AuthSourceDTO();
        BeanUtils.copyBean(authSourceDTO, organizationConfigDetail);
        authSourceDTO.setConfiguration(new String(organizationConfigDetail.getContent(), StandardCharsets.UTF_8));
        return authSourceDTO;
    }
}
