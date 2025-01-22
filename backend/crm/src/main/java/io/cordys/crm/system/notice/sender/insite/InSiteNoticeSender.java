package io.cordys.crm.system.notice.sender.insite;


import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.notice.message.MessageDetail;
import io.cordys.crm.system.notice.NoticeModel;
import io.cordys.crm.system.notice.Receiver;
import io.cordys.crm.system.notice.sender.AbstractNoticeSender;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InSiteNoticeSender extends AbstractNoticeSender {

    @Resource
    private BaseMapper<Notification>notificationBaseMapper;

    public void sendAnnouncement(MessageDetail messageDetail, NoticeModel noticeModel, String context, String subjectText) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        LogUtils.info("发送站内通知: {}", receivers);
        receivers.forEach(receiver -> {
            Map<String, Object> paramMap = noticeModel.getParamMap();
            Notification notification = new Notification();
            notification.setSubject(subjectText);
            notification.setOrganizationId(messageDetail.getOrganizationId());
            notification.setOperator(noticeModel.getOperator());
            notification.setOperation(noticeModel.getEvent());
            notification.setResourceType(messageDetail.getTaskType());
            if (paramMap.get("name") != null) {
                notification.setResourceName((String) paramMap.get("name"));
            }
            if (paramMap.get("title") != null) {
                notification.setResourceName((String) paramMap.get("title"));
            }
            notification.setType(receiver.getType());
            notification.setStatus(NotificationConstants.Status.UNREAD.name());
            notification.setCreateTime(System.currentTimeMillis());
            notification.setReceiver(receiver.getUserId());
            notification.setContent(context.getBytes());
            notificationBaseMapper.insert(notification);
            //TODO: sse 发送 保存信息
        });
    }

    @Override
    public void send(MessageDetail messageDetail, NoticeModel noticeModel) {
        String context = super.getContext(messageDetail, noticeModel);
        String subjectText = super.getSubjectText(messageDetail, noticeModel);
        sendAnnouncement(messageDetail, noticeModel, context, subjectText);
    }

}
