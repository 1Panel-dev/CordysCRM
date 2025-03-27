package io.cordys.crm.system.notice;


import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.notice.message.MessageDetailDTOService;
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

@Component
public class NoticeSendService {
    @Resource
    private MailNoticeSender mailNoticeSender;
    @Resource
    private InSiteNoticeSender inSiteNoticeSender;
    @Resource
    private MessageDetailDTOService messageDetailDTOService;


    /**
     * 在线操作发送通知
     */
    @Async("threadPoolTaskExecutor")
    public void send(String module, NoticeModel noticeModel) {
        setLanguage(noticeModel);
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetailDTO> messageDetailDTOS = messageDetailDTOService.searchMessageByTypeAndOrgId(module, organizationId);
            // 异步发送通知
            messageDetailDTOS.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> {
                        MessageDetailDTO m = SerializationUtils.clone(messageDetail);
                        NoticeModel n = SerializationUtils.clone(noticeModel);
                        try {
                            if (m.isSysEnable()) {
                                inSiteNoticeSender.send(m, n);
                            }
                            if (m.isEmailEnable()) {
                                mailNoticeSender.send(m, n);
                            }
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
     * 后台触发的发送，没有session
     */
    @Async("threadPoolTaskExecutor")
    public void send(String organizationId, String module, NoticeModel noticeModel) {
        setLanguage(noticeModel);
        try {
            List<MessageDetailDTO> messageDetailDTOS = messageDetailDTOService.searchMessageByTypeAndOrgId(module, organizationId);
            // 异步发送通知
            messageDetailDTOS.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> {
                        MessageDetailDTO m = SerializationUtils.clone(messageDetail);
                        NoticeModel n = SerializationUtils.clone(noticeModel);
                        try {
                            if (m.isSysEnable()) {
                                inSiteNoticeSender.send(m, n);
                            }
                            if (m.isEmailEnable()) {
                                mailNoticeSender.send(m, n);
                            }
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
    public void sendOther(String module, NoticeModel noticeModel, boolean excludeSelf) {
        //如果在线需要排除自己，也需要选定当前环境选择的语言
        if (excludeSelf) {
            setLanguage(noticeModel);
        }
        // 定时任务调用不排除自己
        noticeModel.setExcludeSelf(excludeSelf);
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetailDTO> messageDetailDTOS = messageDetailDTOService.searchMessageByTypeAndOrgId(module, organizationId)
                    .stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .toList();
            if (CollectionUtils.isEmpty(messageDetailDTOS)) {
                MessageDetailDTO m = messageDetailDTOService.buildMessageTailDTO(module, noticeModel, organizationId);
                inSiteNoticeSender.send(m, noticeModel);
            } else {
                // 异步发送通知
                messageDetailDTOS.stream()
                        .forEach(messageDetail -> {
                            MessageDetailDTO m = SerializationUtils.clone(messageDetail);
                            NoticeModel n = SerializationUtils.clone(noticeModel);
                            try {
                                if (m.isSysEnable()) {
                                    inSiteNoticeSender.send(m, n);
                                }
                                if (m.isEmailEnable()) {
                                    mailNoticeSender.send(m, n);
                                }
                            } catch (Exception e) {
                                LogUtils.error(e);
                            }
                        });
            }

        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

}
