package io.cordys.crm.system.notice;


import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.notice.message.MessageDetail;
import io.cordys.crm.system.notice.message.MessageDetailService;
import io.cordys.crm.system.notice.message.MessageTemplateUtils;
import io.cordys.crm.system.notice.sender.AbstractNoticeSender;
import io.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import io.cordys.crm.system.notice.sender.mail.MailNoticeSender;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class NoticeSendService {
    @Resource
    private MailNoticeSender mailNoticeSender;
    @Resource
    private InSiteNoticeSender inSiteNoticeSender;
    @Resource
    private MessageDetailService messageDetailService;


    private AbstractNoticeSender getNoticeSender(MessageDetail messageDetail) {
        AbstractNoticeSender noticeSender;
        switch (messageDetail.getReceiveType()) {
            case NotificationConstants.SendType.MAIL -> noticeSender = mailNoticeSender;
            default -> noticeSender = inSiteNoticeSender;
        }
        return noticeSender;
    }

    /**
     * 在线操作发送通知
     */
    @Async("threadPoolTaskExecutor")
    public void send(String module, NoticeModel noticeModel) {
        setLanguage(noticeModel);
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetail> messageDetails = messageDetailService.searchMessageByTypeAndOrgId(module, organizationId);
            // 异步发送通知
            messageDetails.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> {
                        MessageDetail m = SerializationUtils.clone(messageDetail);
                        NoticeModel n = SerializationUtils.clone(noticeModel);
                        try {
                            this.getNoticeSender(m).send(m, n);
                        } catch (Exception e) {
                            LogUtils.error(e);
                        }
                    });

        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    private static void setLanguage(NoticeModel noticeModel) {
        String language = (String) noticeModel.getParamMap().get("Language");
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (StringUtils.containsIgnoreCase(language,"US")) {
            locale = Locale.US;
        } else if (StringUtils.containsIgnoreCase(language,"TW")){
            locale = Locale.TAIWAN;
        }
        LocaleContextHolder.setLocale(locale);
    }

    public void setLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (StringUtils.containsIgnoreCase(language,"US")) {
            locale = Locale.US;
        } else if (StringUtils.containsIgnoreCase(language,"TW")){
            locale = Locale.TAIWAN;
        }
        LocaleContextHolder.setLocale(locale);
    }

    /**
     * jenkins 和定时任务触发的发送
     */
    @Async("threadPoolTaskExecutor")
    public void sendJenkins(String triggerMode, NoticeModel noticeModel) {
        // api和定时任务调用不排除自己
        noticeModel.setExcludeSelf(false);
        try {
            List<MessageDetail> messageDetails;

            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            messageDetails = messageDetailService.searchMessageByTypeAndOrgId(triggerMode, organizationId);

            // 异步发送通知
            messageDetails.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> {
                        MessageDetail m = SerializationUtils.clone(messageDetail);
                        NoticeModel n = SerializationUtils.clone(noticeModel);
                        try {
                            this.getNoticeSender(m).send(m, n);
                        } catch (Exception e) {
                            LogUtils.error(e);
                        }
                    });

        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    /**
     * 后台触发的发送，没有session
     */
    @Async("threadPoolTaskExecutor")
    public void send(String organizationId, String module, NoticeModel noticeModel) {
        setLanguage(noticeModel);
        try {
            List<MessageDetail> messageDetails = messageDetailService.searchMessageByTypeAndOrgId(module, organizationId);
            // 异步发送通知
            messageDetails.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> {
                        MessageDetail m = SerializationUtils.clone(messageDetail);
                        NoticeModel n = SerializationUtils.clone(noticeModel);
                        try {
                            this.getNoticeSender(m).send(m, n);
                        } catch (Exception e) {
                            LogUtils.error(e);
                        }
                    });

        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    /**
     * 其他类型
     */
    @Async("threadPoolTaskExecutor")
    public void sendOther(String module, NoticeModel noticeModel, List<String> users, boolean excludeSelf) {
        //如果在线需要排除自己，也需要选定当前环境选择的语言
        if (excludeSelf) {
            setLanguage(noticeModel);
        }
        // 定时任务调用不排除自己
        noticeModel.setExcludeSelf(excludeSelf);
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetail> messageDetails = messageDetailService.searchMessageByTypeAndOrgId(module, organizationId)
                    .stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .toList();
            if (CollectionUtils.isEmpty(messageDetails)) {
                NoticeModel n = SerializationUtils.clone(noticeModel);
                MessageDetail m = buildMessageTails(module, noticeModel, users, organizationId);
                inSiteNoticeSender.send(m, n);
            } else {
                // 异步发送通知
                messageDetails.stream()
                        .forEach(messageDetail -> {
                            MessageDetail m = SerializationUtils.clone(messageDetail);
                            if (CollectionUtils.isNotEmpty(users)) {
                                m.getReceiverIds().addAll(users);
                            }
                            NoticeModel n = SerializationUtils.clone(noticeModel);
                            try {
                                this.getNoticeSender(m).send(m, n);
                            } catch (Exception e) {
                                LogUtils.error(e);
                            }
                        });
            }

        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    private static MessageDetail buildMessageTails(String module, NoticeModel noticeModel, List<String> users, String organizationId) {
        MessageDetail m = new MessageDetail();
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        String defaultTemplate = defaultTemplateMap.get(module + "_" + noticeModel.getEvent());
        m.setReceiverIds(users);
        m.setOrganizationId(organizationId);
        m.setEvent(noticeModel.getEvent());
        m.setTaskType(module);
        m.setTemplate(defaultTemplate);
        m.setReceiveType(NotificationConstants.SendType.IN_SITE);
        return m;
    }

}
