package cn.cordys.crm.system.notice.sender.mail;


import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.notice.sender.AbstractNoticeSender;
import cn.cordys.crm.system.utils.MailSender;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MailNoticeSender extends AbstractNoticeSender {

    @Resource
    private MailSender mailSender;

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

        mailSender.send(subjectText, context, users, new String[0], organizationId);
    }

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        String context = super.getContext(messageDetailDTO, noticeModel);
        String subjectText = super.getSubjectText(messageDetailDTO, noticeModel);
        try {
            sendMail(context, noticeModel, messageDetailDTO.getOrganizationId(), subjectText);
            LogUtils.debug("发送邮件结束");
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

}
