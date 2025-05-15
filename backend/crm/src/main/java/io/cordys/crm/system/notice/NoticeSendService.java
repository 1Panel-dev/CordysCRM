package io.cordys.crm.system.notice;

import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.notice.message.MessageDetailDTOService;
import io.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import io.cordys.crm.system.notice.sender.mail.MailNoticeSender;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
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
    private final MessageDetailDTOService messageDetailDTOService;

    @Async("threadPoolTaskExecutor")
    public void send(String module, NoticeModel noticeModel) {
        setLanguage(noticeModel.getParamMap().get("Language"));
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetailDTO> messageDetailDTOS = messageDetailDTOService.searchMessageByTypeAndOrgId(module, organizationId);

            messageDetailDTOS.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> sendNotification(messageDetail, noticeModel));

        } catch (Exception e) {
            LogUtils.error("Error sending notification", e);
        }
    }

    private void setLanguage(Object languageObj) {
        String language = languageObj instanceof String ? (String) languageObj : "";
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (StringUtils.containsIgnoreCase(language, "US")) {
            locale = Locale.US;
        }
        LocaleContextHolder.setLocale(locale);
    }

    private void sendNotification(MessageDetailDTO messageDetail, NoticeModel noticeModel) {
        MessageDetailDTO clonedMessageDetail = SerializationUtils.clone(messageDetail);
        NoticeModel clonedNoticeModel = SerializationUtils.clone(noticeModel);
        try {
            if (clonedMessageDetail.isSysEnable()) {
                inSiteNoticeSender.send(clonedMessageDetail, clonedNoticeModel);
            }
            if (clonedMessageDetail.isEmailEnable()) {
                mailNoticeSender.send(clonedMessageDetail, clonedNoticeModel);
            }
        } catch (Exception e) {
            LogUtils.error("Error sending individual notification", e);
        }
    }

    @Async("threadPoolTaskExecutor")
    public void send(String organizationId, String module, NoticeModel noticeModel) {
        setLanguage(noticeModel.getParamMap().get("Language"));
        try {
            List<MessageDetailDTO> messageDetailDTOS = messageDetailDTOService.searchMessageByTypeAndOrgId(module, organizationId);

            messageDetailDTOS.stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .forEach(messageDetail -> sendNotification(messageDetail, noticeModel));

        } catch (Exception e) {
            LogUtils.error("Error sending notification", e);
        }
    }

    @Async("threadPoolTaskExecutor")
    public void sendOther(String module, NoticeModel noticeModel, boolean excludeSelf) {
        setLanguage(noticeModel.getParamMap().get("Language"));
        noticeModel.setExcludeSelf(excludeSelf);
        try {
            String organizationId = (String) noticeModel.getParamMap().get("organizationId");
            List<MessageDetailDTO> messageDetailDTOS = messageDetailDTOService.searchMessageByTypeAndOrgId(module, organizationId)
                    .stream()
                    .filter(messageDetail -> StringUtils.equals(messageDetail.getEvent(), noticeModel.getEvent()))
                    .toList();

            messageDetailDTOS.forEach(messageDetail -> sendNotification(messageDetail, noticeModel));

        } catch (Exception e) {
            LogUtils.error("Error sending other notifications", e);
        }
    }
}
