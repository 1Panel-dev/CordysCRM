package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.response.EmailDTO;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.utils.MailSender;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    @OperationLog(module = LogModule.SYSTEM_BUSINESS_MAIL, type = LogType.UPDATE, operator = "{#userId}")
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
                .resourceId(organizationConfigDetail.getId())
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

    public void verifyEmailConnection(EmailDTO emailDTO) {
        try {
            JavaMailSenderImpl javaMailSender = mailSender.getMailSender(emailDTO);
            javaMailSender.testConnection();

            String recipient = emailDTO.getRecipient();
            if (StringUtils.isBlank(recipient)) {
                return; // Early exit if recipient is blank
            }

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            String username = javaMailSender.getUsername();
            String email = StringUtils.isNotBlank(username) && username.contains("@")
                    ? username
                    : username + "@" + Objects.requireNonNull(javaMailSender.getHost()).substring(javaMailSender.getHost().indexOf(".") + 1);

            InternetAddress from = new InternetAddress();
            String smtpFrom = emailDTO.getFrom();
            if (StringUtils.isBlank(smtpFrom)) {
                from.setAddress(email);
                from.setPersonal(username);
            } else {
                from.setAddress(smtpFrom.contains("@") ? smtpFrom : email);
                from.setPersonal(smtpFrom);
            }
            helper.setFrom(from);
            helper.setSubject("Cordys CRM 测试邮件");

            LogUtils.info("收件人地址: {}", recipient);
            helper.setText("这是一封测试邮件，邮件发送成功", true);
            helper.setTo(recipient);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            LogUtils.error("邮件发送或连接测试失败: ", e);
            throw new GenericException(Translator.get("email.connection.failed"));
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
        extOrganizationConfigMapper.updateSyncFlag(orgId, syncResource, OrganizationConfigConstants.ConfigType.THIRD.name());
    }
}

