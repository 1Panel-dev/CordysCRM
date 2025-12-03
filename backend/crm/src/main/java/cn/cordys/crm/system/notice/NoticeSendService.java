package cn.cordys.crm.system.notice;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.dingtalk.service.DingTalkNoticeSender;
import cn.cordys.crm.integration.lark.service.LarkNoticeSender;
import cn.cordys.crm.integration.wecom.service.WeComNoticeSender;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.message.MessageDetailService;
import cn.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import cn.cordys.crm.system.notice.sender.mail.MailNoticeSender;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NoticeSendService {

    private final MailNoticeSender mailNoticeSender;
    private final InSiteNoticeSender inSiteNoticeSender;
    private final MessageDetailService messageDetailService;

    @Async("threadPoolTaskExecutor")
    public void send(String module, NoticeModel noticeModel) {
        setLanguage(noticeModel.getParamMap().get("Language"));
        boolean useTemplate = Boolean.getBoolean((String) noticeModel.getParamMap().get("useTemplate"));
        String template = (String) noticeModel.getParamMap().get("template");
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetailDTO> messageDetails = messageDetailService.searchMessageByTypeAndOrgId(module, useTemplate, template, organizationId);

            messageDetails.stream()
                    .filter(messageDetail -> Strings.CS.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> sendNotification(messageDetail, noticeModel));

        } catch (Exception e) {
            LogUtils.error("Error sending notification", e);
        }
    }

    private void setLanguage(Object languageObj) {
        String language = languageObj instanceof String ? (String) languageObj : "";
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (Strings.CI.contains(language, "US")) {
            locale = Locale.US;
        }
        LocaleContextHolder.setLocale(locale);
    }

    public void sendNotification(MessageDetailDTO messageDetail, NoticeModel noticeModel) {
        MessageDetailDTO clonedMessageDetail = SerializationUtils.clone(messageDetail);
        NoticeModel clonedNoticeModel = SerializationUtils.clone(noticeModel);
        try {
            if (clonedMessageDetail.isSysEnable()) {
                inSiteNoticeSender.send(clonedMessageDetail, clonedNoticeModel);
            }
            if (clonedMessageDetail.isEmailEnable()) {
                mailNoticeSender.send(clonedMessageDetail, clonedNoticeModel);
            }
            if (clonedMessageDetail.isWeComEnable()) {
                WeComNoticeSender weComNoticeSender = CommonBeanFactory.getBean(WeComNoticeSender.class);
                if (weComNoticeSender != null) {
                    weComNoticeSender.sendWeCom(clonedMessageDetail, clonedNoticeModel);
                } else {
                    LogUtils.warn("WeComNoticeSender bean not found, skipping WeCom notification.");
                }
            }
            if (clonedMessageDetail.isDingTalkEnable()) {
                DingTalkNoticeSender dingTalkNoticeSender = CommonBeanFactory.getBean(DingTalkNoticeSender.class);
                if (dingTalkNoticeSender != null) {
                    dingTalkNoticeSender.sendDingTalk(clonedMessageDetail, clonedNoticeModel);
                } else {
                    LogUtils.warn("DingTalkNoticeSender bean not found, skipping DingTalk notification.");
                }
            }
            if (clonedMessageDetail.isLarkEnable()) {
                LarkNoticeSender larkNoticeSender = CommonBeanFactory.getBean(LarkNoticeSender.class);
                if (larkNoticeSender != null) {
                    larkNoticeSender.sendLark(clonedMessageDetail, clonedNoticeModel);
                } else {
                    LogUtils.warn("LarkNoticeSender bean not found, skipping Lark notification.");
                }
            }
        } catch (Exception e) {
            LogUtils.error("Error sending individual notification", e);
        }
    }

    @Async("threadPoolTaskExecutor")
    public void send(String organizationId, String module, NoticeModel noticeModel) {
        setLanguage(noticeModel.getParamMap().get("Language"));
        boolean useTemplate = Boolean.getBoolean((String) noticeModel.getParamMap().get("useTemplate"));
        String template = (String) noticeModel.getParamMap().get("template");
        try {
            List<MessageDetailDTO> messageDetails = messageDetailService.searchMessageByTypeAndOrgId(module, useTemplate, template, organizationId);

            messageDetails.stream()
                    .filter(messageDetail -> Strings.CS.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> sendNotification(messageDetail, noticeModel));

        } catch (Exception e) {
            LogUtils.error("Error sending notification", e);
        }
    }

    @Async("threadPoolTaskExecutor")
    public void sendOther(String module, NoticeModel noticeModel, boolean excludeSelf) {
        setLanguage(noticeModel.getParamMap().get("Language"));
        boolean useTemplate = Boolean.parseBoolean((String) noticeModel.getParamMap().get("useTemplate"));
        String template = (String) noticeModel.getParamMap().get("template");
        noticeModel.setExcludeSelf(excludeSelf);
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetailDTO> messageDetails = messageDetailService.searchMessageByTypeAndOrgId(module, useTemplate, template, organizationId)
                    .stream()
                    .filter(messageDetail -> Strings.CS.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .toList();

            messageDetails.forEach(messageDetail -> sendNotification(messageDetail, noticeModel));

        } catch (Exception e) {
            LogUtils.error("Error sending other notifications", e);
        }
    }
}
