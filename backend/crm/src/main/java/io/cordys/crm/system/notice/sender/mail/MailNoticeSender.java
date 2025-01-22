package io.cordys.crm.system.notice.sender.mail;


import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.crm.system.notice.message.MessageDetail;
import io.cordys.crm.system.notice.NoticeModel;
import io.cordys.crm.system.notice.Receiver;
import io.cordys.crm.system.notice.sender.AbstractNoticeSender;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import io.cordys.crm.system.domain.User;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MailNoticeSender extends AbstractNoticeSender {

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    public void sendMail(String context, NoticeModel noticeModel, String organizationId, String subjectText) throws Exception {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        List<String> userIds = receivers.stream()
                .map(Receiver::getUserId)
                .distinct()
                .collect(Collectors.toList());
        String[] users = super.getUsers(userIds, organizationId).stream()
                .map(User::getEmail)
                .distinct()
                .toArray(String[]::new);

        send(subjectText, context, users, new String[0], organizationId);
    }

    private void send(String subject, String context, String[] users, String[] cc, String organizationId) throws Exception {
        LogUtils.debug("发送邮件开始 ");
        //
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.EMAIL.name());
        if (organizationConfig == null) {
            LogUtils.error("邮件未设置 ");
            return;
        }
        OrganizationConfigDetail organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        if (organizationConfigDetail == null) {
            LogUtils.error("邮件内容为空 ");
            return;
        }
        EmailDTO emailDTO = JSON.parseObject(new String(organizationConfigDetail.getContent()), EmailDTO.class);
        JavaMailSenderImpl javaMailSender = getMailSender(emailDTO);
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
        if (subject.length() > 60) {
            subject = subject.substring(0, 59);
        }
        helper.setSubject("MeterSphere " + subject);

        LogUtils.info("收件人地址: {}", Arrays.asList(users));
        helper.setText(context, true);
        // 有抄送
        if (cc != null && cc.length > 0) {
            //设置抄送人 CC（Carbon Copy）
            helper.setCc(cc);
            // to 参数表示收件人
            helper.setTo(users);
            javaMailSender.send(mimeMessage);
        }
        // 无抄送
        else {
            for (String u : users) {
                helper.setTo(u);
                try {
                    javaMailSender.send(mimeMessage);
                } catch (Exception e) {
                    LogUtils.error("发送邮件失败: ", e);
                }
            }
        }
    }

    @Override
    public void send(MessageDetail messageDetail, NoticeModel noticeModel) {
        String context = super.getContext(messageDetail, noticeModel);
        String subjectText = super.getSubjectText(messageDetail, noticeModel);
        try {
            sendMail(context, noticeModel, messageDetail.getOrganizationId(), subjectText);
            LogUtils.debug("发送邮件结束");
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    public JavaMailSenderImpl getMailSender(EmailDTO emailDTO) {
        Properties props = new Properties();
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        javaMailSender.setProtocol("smtp");
        javaMailSender.setHost(emailDTO.getHost());
        javaMailSender.setPort(Integer.parseInt(emailDTO.getPort()));
        javaMailSender.setUsername(emailDTO.getAccount());
        javaMailSender.setPassword(emailDTO.getPassword());

        if (BooleanUtils.toBoolean(emailDTO.getSsl())) {
            javaMailSender.setProtocol("smtps");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        if (BooleanUtils.toBoolean(emailDTO.getTsl())) {
            String result = BooleanUtils.toString(BooleanUtils.toBoolean(emailDTO.getTsl()), "true", "false");
            props.put("mail.smtp.starttls.enable", result);
            props.put("mail.smtp.starttls.required", result);
        }

        props.put("mail.smtp.ssl.trust", javaMailSender.getHost());
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.connectiontimeout", "5000");
        javaMailSender.setJavaMailProperties(props);
        return javaMailSender;
    }

}
