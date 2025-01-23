package io.cordys.crm.system.service;

import java.util.ArrayList;
import java.util.List;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.dto.response.SyncOrganizationDTO;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;

@Service
public class OrganizationConfigService {

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Resource
    private BaseMapper<OrganizationConfigDetail> organizationConfigDetailBaseMapper;

    @Resource
    private BaseMapper<OrganizationConfig> organizationConfigBaseMapper;


    public EmailDTO getEmail(String organizationId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.EMAIL.name());
        if (organizationConfig == null) {
            return new EmailDTO();
        }
        OrganizationConfigDetail organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        if (organizationConfigDetail == null) {
            return new EmailDTO();
        }
        EmailDTO emailDTO = JSON.parseObject(new String(organizationConfigDetail.getContent()), EmailDTO.class);
        emailDTO.setId(organizationConfigDetail.getId());
        return emailDTO;
    }

    public List<SyncOrganizationDTO> getSynOrganization(String organizationId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.SYNCHRONIZATION.name());
        if (organizationConfig == null) {
            return new ArrayList<>();
        }
        List<SyncOrganizationDTO> syncOrganizationDTOS = new ArrayList<>();
        List<OrganizationConfigDetail> organizationConfigDetails = extOrganizationConfigDetailMapper.getOrganizationConfigDetails(organizationConfig.getId());
        for (OrganizationConfigDetail organizationConfigDetail : organizationConfigDetails) {
            SyncOrganizationDTO syncOrganizationDTO = JSON.parseObject(new String(organizationConfigDetail.getContent()), SyncOrganizationDTO.class);
            syncOrganizationDTO.setId(organizationConfigDetail.getId());
            syncOrganizationDTOS.add(syncOrganizationDTO);
        }
        return syncOrganizationDTOS;
    }

    /**
     * 删除组织设置(根据ID,认证的删除)
     *
     * @param id
     */
    public void delete(String id) {
        organizationConfigDetailBaseMapper.deleteByPrimaryKey(id);
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD, operator = "{#userId}")
    public void addEmail(EmailDTO emailDTO, String organizationId, String userId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.EMAIL.name());
        if (organizationConfig == null) {
            organizationConfig = new OrganizationConfig();
            organizationConfig.setId(IDGenerator.nextStr());
            organizationConfig.setOrganizationId(organizationId);
            organizationConfig.setType(OrganizationConfigConstants.ConfigType.EMAIL.name());
            organizationConfig.setCreateTime(System.currentTimeMillis());
            organizationConfig.setUpdateTime(System.currentTimeMillis());
            organizationConfig.setCreateUser(userId);
            organizationConfig.setUpdateUser(userId);
            organizationConfigBaseMapper.insert(organizationConfig);
        }
        OrganizationConfigDetail organizationConfigDetail = getOrganizationConfigDetail(userId, organizationConfig, JSON.toJSONString(emailDTO));
        organizationConfigDetail.setType(OrganizationConfigConstants.ConfigType.EMAIL.name());
        organizationConfigDetailBaseMapper.insert(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(organizationConfig.getId())
                        .resourceName("邮件设置")
                        .modifiedValue(emailDTO)
                        .build()
        );

    }

    @NotNull
    private OrganizationConfigDetail getOrganizationConfigDetail(String userId, OrganizationConfig organizationConfig, String jsonString) {
        OrganizationConfigDetail organizationConfigDetail;
        organizationConfigDetail = new OrganizationConfigDetail();
        organizationConfigDetail.setId(IDGenerator.nextStr());
        organizationConfigDetail.setContent(jsonString.getBytes());
        organizationConfigDetail.setCreateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setCreateUser(userId);
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetail.setConfigId(organizationConfig.getId());
        return organizationConfigDetail;
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD, operator = "{#userId}")
    public void addSynchronization(SyncOrganizationDTO syncOrganizationDTO, String organizationId, String userId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.SYNCHRONIZATION.name());
        if (organizationConfig == null) {
            organizationConfig = new OrganizationConfig();
            organizationConfig.setId(IDGenerator.nextStr());
            organizationConfig.setOrganizationId(organizationId);
            organizationConfig.setType(OrganizationConfigConstants.ConfigType.SYNCHRONIZATION.name());
            organizationConfig.setCreateTime(System.currentTimeMillis());
            organizationConfig.setUpdateTime(System.currentTimeMillis());
            organizationConfig.setCreateUser(userId);
            organizationConfig.setUpdateUser(userId);
            organizationConfigBaseMapper.insert(organizationConfig);
        }
        OrganizationConfigDetail organizationConfigDetail = getOrganizationConfigDetail(userId, organizationConfig, JSON.toJSONString(syncOrganizationDTO));
        organizationConfigDetail.setType(syncOrganizationDTO.getType());
        organizationConfigDetailBaseMapper.insert(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(null)
                .resourceName("同步组织设置")
                .modifiedValue(syncOrganizationDTO)
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{#emailDTO.id}", operator = "{#userId}")
    public void updateEmail(EmailDTO emailDTO, String userId) {
        OrganizationConfigDetail organizationConfigDetail = organizationConfigDetailBaseMapper.selectByPrimaryKey(emailDTO.getId());
        EmailDTO emailDTOOld = JSON.parseObject(new String(organizationConfigDetail.getContent()), EmailDTO.class);
        organizationConfigDetail.setContent(JSON.toJSONBytes(emailDTO));
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(emailDTOOld)
                .resourceName("邮件设置")
                .modifiedValue(emailDTO)
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{#syncOrganizationDTO.id}", operator = "{#userId}")
    public void updateSynchronization(SyncOrganizationDTO syncOrganizationDTO, String userId) {
        OrganizationConfigDetail organizationConfigDetail = organizationConfigDetailBaseMapper.selectByPrimaryKey(syncOrganizationDTO.getId());
        SyncOrganizationDTO syncOrganizationDTOOld = JSON.parseObject(new String(organizationConfigDetail.getContent()), SyncOrganizationDTO.class);
        organizationConfigDetail.setContent(JSON.toJSONBytes(syncOrganizationDTO));
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .resourceName("同步组织设置")
                .originalValue(syncOrganizationDTOOld)
                .modifiedValue(syncOrganizationDTO)
                .build());
    }
}

