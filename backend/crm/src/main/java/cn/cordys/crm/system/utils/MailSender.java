package cn.cordys.crm.system.utils;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.response.EmailDTO;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

@Component
@Slf4j
public class MailSender {

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    public void send(String subject,
                     String context,
                     String[] users,
                     String[] cc,
                     String organizationId) {

        OrganizationConfig config = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId,
                OrganizationConfigConstants.ConfigType.EMAIL.name()
        );
        if (config == null) {
            log.error("邮件配置不存在");
            return;
        }

        OrganizationConfigDetail detail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(config.getId());
        if (detail == null) {
            log.error("邮件配置内容为空");
            return;
        }

        EmailDTO emailDTO = JSON.parseObject(
                new String(detail.getContent(), StandardCharsets.UTF_8),
                EmailDTO.class
        );

        JavaMailSenderImpl mailSender = buildMailSender(emailDTO);
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(buildFromAddress(mailSender, emailDTO));
            helper.setSubject(buildSubject(subject));
            helper.setText(context, true);

            log.info("收件人: {}", Arrays.toString(users));

            if (cc != null && cc.length > 0) {
                helper.setTo(users);
                helper.setCc(cc);
                mailSender.send(message);
            } else {
                for (String user : users) {
                    log.info("正在发送邮件给: {}", user);
                    //检查user 是否符合邮箱格式
                    if (StringUtils.isBlank(user) || !user.contains("@")) {
                        log.warn("用户 {} 不符合邮箱格式，或者用户为空，将跳过", user);
                        continue;
                    }
                    helper.setTo(user);
                    mailSender.send(message);
                }
            }
        } catch (Exception e) {
            log.error("发送邮件失败", e);
        }
    }

    public JavaMailSenderImpl buildMailSender(EmailDTO emailDTO) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        sender.setProtocol("smtp");
        sender.setHost(emailDTO.getHost());
        sender.setPort(Integer.parseInt(emailDTO.getPort()));
        sender.setUsername(emailDTO.getAccount());
        sender.setPassword(emailDTO.getPassword());

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.ssl.trust", sender.getHost());

        if (BooleanUtils.toBoolean(emailDTO.getSsl())) {
            sender.setProtocol("smtps");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        if (BooleanUtils.toBoolean(emailDTO.getTsl())) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        sender.setJavaMailProperties(props);
        return sender;
    }

    private InternetAddress buildFromAddress(JavaMailSenderImpl sender, EmailDTO emailDTO) throws Exception {
        String username = sender.getUsername();
        String email = Strings.CS.contains(username, "@")
                ? username
                : username + "@" + Objects.requireNonNull(sender.getHost()).substring(sender.getHost().indexOf('.') + 1);

        InternetAddress from = new InternetAddress();
        String smtpFrom = emailDTO.getFrom();

        if (StringUtils.isBlank(smtpFrom)) {
            from.setAddress(email);
            from.setPersonal(username);
        } else {
            from.setAddress(smtpFrom.contains("@") ? smtpFrom : email);
            from.setPersonal(smtpFrom);
        }
        return from;
    }

    private String buildSubject(String subject) {
        if (StringUtils.length(subject) > 60) {
            subject = subject.substring(0, 59);
        }
        return "Cordys CRM " + subject;
    }

}
