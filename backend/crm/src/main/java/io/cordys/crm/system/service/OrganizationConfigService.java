package io.cordys.crm.system.service;

import java.util.*;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.client.QrCodeClient;
import io.cordys.crm.system.constants.SyncSettingApiPaths;
import io.cordys.crm.system.dto.dingtalk.DingTalkBaseParamDTO;
import io.cordys.crm.system.dto.lark.LarkBaseParamDTO;
import io.cordys.crm.system.notice.sender.mail.MailNoticeSender;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    @Resource
    private MailNoticeSender mailNoticeSender;

    @Resource
    private QrCodeClient qrCodeClient;


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
        List<OrganizationConfigDetail> organizationConfigDetails = extOrganizationConfigDetailMapper.getOrganizationConfigDetails(organizationConfig.getId(), null);
        for (OrganizationConfigDetail organizationConfigDetail : organizationConfigDetails) {
            SyncOrganizationDTO syncOrganizationDTO = JSON.parseObject(new String(organizationConfigDetail.getContent()), SyncOrganizationDTO.class);
            syncOrganizationDTO.setType(organizationConfigDetail.getType());
            syncOrganizationDTO.setId(organizationConfigDetail.getId());
            syncOrganizationDTO.setEnable(organizationConfigDetail.getEnable());
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
        organizationConfigDetail.setName(Translator.get("email.setting"));
        organizationConfigDetail.setEnable(true);
        organizationConfigDetailBaseMapper.insert(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(organizationConfig.getId())
                        .resourceName(organizationConfigDetail.getName())
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
        int organizationConfigDetailCount = extOrganizationConfigDetailMapper.getOrganizationConfigDetailCount(organizationConfig.getId(), null, syncOrganizationDTO.getType());
        if (organizationConfigDetailCount > 0) {
            throw new GenericException(Translator.get("sync.organization.type.exist"));
        }
        OrganizationConfigDetail organizationConfigDetail = getOrganizationConfigDetail(userId, organizationConfig, JSON.toJSONString(syncOrganizationDTO));
        organizationConfigDetail.setType(syncOrganizationDTO.getType());
        organizationConfigDetail.setEnable(syncOrganizationDTO.getEnable());
        organizationConfigDetail.setName(Translator.get("sync.organization"));
        organizationConfigDetailBaseMapper.insert(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(null)
                .resourceName(organizationConfigDetail.getName())
                .modifiedValue(syncOrganizationDTO)
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{#emailDTO.id}", operator = "{#userId}")
    public void updateEmail(EmailDTO emailDTO, String userId) {
        OrganizationConfigDetail organizationConfigDetail = organizationConfigDetailBaseMapper.selectByPrimaryKey(emailDTO.getId());
        if (organizationConfigDetail == null) {
            throw new GenericException(Translator.get("email.update.error.id.wrong"));
        }
        EmailDTO emailDTOOld = JSON.parseObject(new String(organizationConfigDetail.getContent()), EmailDTO.class);
        organizationConfigDetail.setContent(JSON.toJSONBytes(emailDTO));
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(emailDTOOld)
                .resourceName(Translator.get("email.setting"))
                .modifiedValue(emailDTO)
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{#syncOrganizationDTO.id}", operator = "{#userId}")
    public void updateSynchronization(SyncOrganizationDTO syncOrganizationDTO, String userId) {
        OrganizationConfigDetail organizationConfigDetail = organizationConfigDetailBaseMapper.selectByPrimaryKey(syncOrganizationDTO.getId());
        if (organizationConfigDetail == null) {
            throw new GenericException(Translator.get("sync.organization.update.error.id.wrong"));
        }
        SyncOrganizationDTO syncOrganizationDTOOld = JSON.parseObject(new String(organizationConfigDetail.getContent()), SyncOrganizationDTO.class);
        organizationConfigDetail.setContent(JSON.toJSONBytes(syncOrganizationDTO));
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser(userId);
        organizationConfigDetail.setEnable(syncOrganizationDTO.getEnable());
        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .resourceName(Translator.get("sync.organization"))
                .originalValue(syncOrganizationDTOOld)
                .modifiedValue(syncOrganizationDTO)
                .build());
    }

    public void testEmailConnection(EmailDTO emailDTO) {
        JavaMailSenderImpl javaMailSender = null;
        try {
            javaMailSender = mailNoticeSender.getMailSender(emailDTO);
            javaMailSender.testConnection();
        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
            throw new GenericException(Translator.get("email.connection.failed"));
        }
        String recipient = emailDTO.getRecipient();
        if (!StringUtils.isBlank(recipient)) {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                String username = javaMailSender.getUsername();
                String email;
                if (username.contains("@")) {
                    email = username;
                } else {
                    String mailHost = javaMailSender.getHost();
                    String domainName = mailHost.substring(mailHost.indexOf(".") + 1);
                    email = username + "@" + domainName;
                }
                InternetAddress from = new InternetAddress();

                String smtpFrom = emailDTO.getFrom();
                if (StringUtils.isBlank(smtpFrom)) {
                    from.setAddress(email);
                    from.setPersonal(username);
                } else {
                    // 指定发件人后，address 应该是邮件服务器验证过的发件人
                    if (smtpFrom.contains("@")) {
                        from.setAddress(smtpFrom);
                    } else {
                        from.setAddress(email);
                    }
                    from.setPersonal(smtpFrom);
                }
                helper.setFrom(from);

                LogUtils.debug("发件人地址" + javaMailSender.getUsername());
                LogUtils.debug("helper" + helper);
                helper.setSubject("MeterSphere测试邮件");

                LogUtils.info("收件人地址: {}", Arrays.asList(recipient));
                helper.setText("这是一封测试邮件，邮件发送成功", true);
                helper.setTo(recipient);
                try {
                    javaMailSender.send(mimeMessage);
                } catch (Exception e) {
                    LogUtils.error("发送邮件失败: ", e);
                }
            } catch (Exception e) {
                LogUtils.error(e);
                throw new GenericException(Translator.get("email.connection.failed"));
            }
        }
    }

    public void testSyncConnection(SyncOrganizationDTO syncOrganizationDTO, String userId) {
        String accessToken = "";
        if (StringUtils.equalsIgnoreCase(syncOrganizationDTO.getType(), DepartmentConstants.WECOM.name())) {
            accessToken = generateWeComToken(syncOrganizationDTO);
        }
        if (StringUtils.equalsIgnoreCase(syncOrganizationDTO.getType(), DepartmentConstants.DINGTALK.name())) {
            accessToken = generateDingTalkToken(syncOrganizationDTO);
        }
        if (StringUtils.equalsIgnoreCase(syncOrganizationDTO.getType(), DepartmentConstants.LARK.name())) {
            accessToken = generateLarkToken(syncOrganizationDTO);
        }

        if (StringUtils.isNotBlank(accessToken)) {
            syncOrganizationDTO.setValid(true);
            OrganizationConfigDetail organizationConfigDetail = new OrganizationConfigDetail();
            organizationConfigDetail.setId(syncOrganizationDTO.getId());
            organizationConfigDetail.setContent(JSON.toJSONBytes(syncOrganizationDTO));
            organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
            organizationConfigDetail.setUpdateUser(userId);
            organizationConfigDetail.setEnable(syncOrganizationDTO.getEnable());
            organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        }

    }

    public String generateDingTalkToken(SyncOrganizationDTO syncOrganizationDTO) {
        if (StringUtils.isBlank(syncOrganizationDTO.getAppSecret())) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }
        String appKey = syncOrganizationDTO.getAppKey();
        String appSecret = syncOrganizationDTO.getAppSecret();
        DingTalkBaseParamDTO dingTalkTokenParamDTO = new DingTalkBaseParamDTO();
        dingTalkTokenParamDTO.setAppKey(appKey);
        dingTalkTokenParamDTO.setAppSecret(appSecret);
        String body = qrCodeClient.postExchange(SyncSettingApiPaths.DING_TALK_GET_TOKEN, null, null, dingTalkTokenParamDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
        Map bodyMap = JSON.parseMap(body);
        if (ObjectUtils.isNotEmpty(bodyMap.get("errcode")) && Integer.parseInt(bodyMap.get("errcode").toString()) != 0) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }
        return bodyMap.get("accessToken").toString();
    }

    private String generateWeComToken(SyncOrganizationDTO syncOrganizationDTO) {
        if (StringUtils.isBlank(syncOrganizationDTO.getAppSecret())) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }
        String corpid = syncOrganizationDTO.getCorpId();
        String appSecret = syncOrganizationDTO.getAppSecret();
        String url = String.format(SyncSettingApiPaths.WE_COM_GET_TOKEN, corpid, appSecret);
        String body = qrCodeClient.get(url);
        Map bodyMap = JSON.parseMap(body);
        if (ObjectUtils.isNotEmpty(bodyMap.get("errcode")) && Integer.parseInt(bodyMap.get("errcode").toString()) != 0) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }
        return bodyMap.get("access_token").toString();
    }

    private String generateLarkToken(SyncOrganizationDTO syncOrganizationDTO) {
        if (StringUtils.isBlank(syncOrganizationDTO.getAppSecret())) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }
        String agentId = syncOrganizationDTO.getAgentId();
        String appSecret = syncOrganizationDTO.getAppSecret();
        LarkBaseParamDTO larkBaseParamDTO = new LarkBaseParamDTO();
        larkBaseParamDTO.setApp_id(agentId);
        larkBaseParamDTO.setApp_secret(appSecret);
        String body = qrCodeClient.postExchange(SyncSettingApiPaths.LARK_GET_TOKEN, null, null, larkBaseParamDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
        Map bodyMap = JSON.parseMap(body);
        if (ObjectUtils.isNotEmpty(bodyMap.get("code")) && Integer.parseInt(bodyMap.get("code").toString()) > 0) {
            throw new GenericException(Translator.get("sync.organization.test.error"));
        }
        return bodyMap.get("tenant_access_token").toString();
    }


    /**
     * 当前组织的用户数据是否是第三方同步的
     *
     * @param organizationId
     */
    public boolean syncCheck(String organizationId) {
        return extOrganizationConfigMapper.getSyncFlag(organizationId) > 0;
    }


    /**
     * 更新三方同步标识
     *
     * @param orgId
     * @param syncResource
     */
    public void updateSyncFlag(String orgId, String syncResource) {
        extOrganizationConfigMapper.updateSyncFlag(orgId, syncResource, OrganizationConfigConstants.ConfigType.SYNCHRONIZATION.name());
    }
}

