package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.mail.sender.MailSender;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private MailSender mailSender;


    public EmailDTO getEmail(String organizationId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.EMAIL.name());
        if (organizationConfig == null) {
            return new EmailDTO();
        }
        OrganizationConfigDetail organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        if (organizationConfigDetail == null) {
            return new EmailDTO();
        }
        return JSON.parseObject(new String(organizationConfigDetail.getContent()), EmailDTO.class);
    }

    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, operator = "{#userId}")
    public void editEmail(EmailDTO emailDTO, String organizationId, String userId) {
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
        EmailDTO emailDTOOld = new EmailDTO();
        OrganizationConfigDetail organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        if (organizationConfigDetail == null) {
            organizationConfigDetail = getOrganizationConfigDetail(userId, organizationConfig, JSON.toJSONString(emailDTO));
            organizationConfigDetail.setType(OrganizationConfigConstants.ConfigType.EMAIL.name());
            organizationConfigDetail.setName(Translator.get("email.setting"));
            organizationConfigDetail.setEnable(true);
            organizationConfigDetailBaseMapper.insert(organizationConfigDetail);
        } else {
            emailDTOOld = JSON.parseObject(new String(organizationConfigDetail.getContent()), EmailDTO.class);
            organizationConfigDetail.setContent(JSON.toJSONBytes(emailDTO));
            organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
            organizationConfigDetail.setUpdateUser(userId);
            organizationConfigDetailBaseMapper.update(organizationConfigDetail);
        }

        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(emailDTOOld)
                .resourceName(Translator.get("email.setting"))
                .modifiedValue(emailDTO)
                .build());

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

    public void testEmailConnection(EmailDTO emailDTO) {
        JavaMailSenderImpl javaMailSender = null;
        try {
            javaMailSender = mailSender.getMailSender(emailDTO);
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

                LogUtils.info("收件人地址: {}", List.of(recipient));
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

