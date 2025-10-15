package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.DepartmentConstants;
import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.response.EmailDTO;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.utils.MailSender;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * @param organizationId 组织ID
     *
     * @return true 是第三方同步的 false 不是第三方同步的
     */
    public boolean syncCheck(String organizationId) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
        String type;
        if (organizationConfig == null || StringUtils.isBlank(organizationConfig.getSyncResource())) {
            return false;
        }
        if (organizationConfig.getSyncResource().equals(DepartmentConstants.WECOM.name())) {
            type = ThirdConstants.ThirdDetailType.WECOM_SYNC.toString();
        } else if (organizationConfig.getSyncResource().equals(DepartmentConstants.DINGTALK.name())) {
            type = ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString();
        } else if (organizationConfig.getSyncResource().equals(DepartmentConstants.LARK.name())) {
            type = ThirdConstants.ThirdDetailType.LARK_SYNC.toString();
        } else {
            return false;
        }
        return extOrganizationConfigMapper.getSyncFlag(organizationId, type) > 0;
    }


    /**
     * 更新三方同步标识
     *
     * @param orgId        组织ID
     * @param syncResource 同步来源
     */
    public void updateSyncFlag(String orgId, String syncResource, Boolean syncStatus) {
        extOrganizationConfigMapper.updateSyncFlag(orgId, syncResource, OrganizationConfigConstants.ConfigType.THIRD.name(), syncStatus);
    }

    /**
     * 数据升级
     * 1. 给组织配置表添加同步回掉地址字段
     * 2. 给组织配置详情表添加启用字段
     */
    public void upgradeData() {
        // 构建查询条件，查找第三方配置的数据
        LambdaQueryWrapper<OrganizationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrganizationConfig::getType, OrganizationConfigConstants.ConfigType.THIRD.name())
                .eq(OrganizationConfig::getSyncResource, DepartmentConstants.WECOM.name());

        // 获取符合条件的所有组织配置
        List<OrganizationConfig> organizationConfigs = organizationConfigBaseMapper.selectListByLambda(wrapper);

        // 如果没有找到符合条件的配置，直接返回
        if (organizationConfigs.isEmpty()) {
            return;
        }

        // 获取所有匹配的组织配置的ID
        List<String> ids = organizationConfigs.stream()
                .map(OrganizationConfig::getId)
                .collect(Collectors.toList());

        // 构建查询条件，查找对应的组织配置详情
        LambdaQueryWrapper<OrganizationConfigDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(OrganizationConfigDetail::getId, ids);

        // 获取所有符合条件的配置详情
        List<OrganizationConfigDetail> organizationConfigDetails = organizationConfigDetailBaseMapper.selectListByLambda(detailWrapper);

        // 如果有找到组织配置详情
        if (!organizationConfigDetails.isEmpty()) {
            // 获取第一个配置详情（配置是唯一的）
            OrganizationConfigDetail organizationConfigDetail = organizationConfigDetails.getFirst();

            // 解析配置详情中的内容为 ThirdConfigurationDTO 对象
            ThirdConfigurationDTO thirdConfigurationDTO = JSON.parseObject(
                    new String(organizationConfigDetail.getContent()), ThirdConfigurationDTO.class);

            // 构建查询条件，查找授权配置
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrganizationConfig::getType, OrganizationConfigConstants.ConfigType.AUTH.name());

            // 获取所有授权配置
            List<OrganizationConfig> authConfigs = organizationConfigBaseMapper.selectListByLambda(wrapper);

            // 如果有找到授权配置
            if (!authConfigs.isEmpty()) {
                // 查找对应的授权配置详情
                detailWrapper = new LambdaQueryWrapper<>();
                detailWrapper.in(OrganizationConfigDetail::getConfigId,
                        authConfigs.stream().map(OrganizationConfig::getId).collect(Collectors.toList()));
                List<OrganizationConfigDetail> authConfigDetails = organizationConfigDetailBaseMapper.selectListByLambda(detailWrapper);
                OrganizationConfigDetail oauthDetail = authConfigDetails.getFirst();
                // 如果有找到授权配置详情
                if (!authConfigDetails.isEmpty() && oauthDetail.getEnable()) {
                    // 解析授权配置详情中的内容为 ThirdConfigurationDTO 对象
                    ThirdConfigurationDTO authConfig = JSON.parseObject(
                            new String(oauthDetail.getContent()), ThirdConfigurationDTO.class);

                    // 如果第三方配置和授权配置的 corpId 一致，更新第三方配置的 redirectUrl
                    if ( Strings.CI.equals(thirdConfigurationDTO.getCorpId(), authConfig.getCorpId())) {
                        thirdConfigurationDTO.setRedirectUrl(authConfig.getRedirectUrl());
                        // 更新组织配置详情的内容
                        organizationConfigDetail.setContent(JSON.toJSONBytes(thirdConfigurationDTO));
                        organizationConfigDetailBaseMapper.update(organizationConfigDetail);
                    }
                }
            }
        }
    }

}

