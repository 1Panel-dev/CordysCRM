package io.cordys.crm.system.notice.sender.insite;


import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.notice.common.Receiver;
import io.cordys.crm.system.notice.sender.AbstractNoticeSender;
import io.cordys.crm.system.notice.sse.SseService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InSiteNoticeSender extends AbstractNoticeSender {

    @Resource
    private BaseMapper<Notification>notificationBaseMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SseService sseService;

    private static final String USER_READ_PREFIX = "user_read:";  // Redis 存储用户读取前缀


    public void sendAnnouncement(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel, String context, String subjectText) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        LogUtils.info("发送站内通知: {}", receivers);
        receivers.forEach(receiver -> {
            String id = IDGenerator.nextStr();
            Map<String, Object> paramMap = noticeModel.getParamMap();
            Notification notification = new Notification();
            notification.setId(id);
            notification.setSubject(StringUtils.isBlank(subjectText) ? Translator.get("notice.default.subject") : subjectText);
            notification.setOrganizationId(messageDetailDTO.getOrganizationId());
            notification.setOperator(noticeModel.getOperator());
            notification.setOperation(noticeModel.getEvent());
            notification.setResourceId(messageDetailDTO.getId());
            notification.setResourceType(messageDetailDTO.getTaskType());
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
            notification.setCreateUser(noticeModel.getOperator());
            notification.setUpdateUser(noticeModel.getOperator());
            notification.setCreateTime(System.currentTimeMillis());
            notification.setUpdateTime(System.currentTimeMillis());
            notificationBaseMapper.insert(notification);
            //更新用户的已读全部消息状态 0 为未读，1为已读
            stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + receiver.getUserId(), "0");

            // 发送消息
            sseService.broadcastPeriodically(receiver.getUserId());
        });

    }

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        String context = super.getContext(messageDetailDTO, noticeModel);
        String subjectText = super.getSubjectText(messageDetailDTO, noticeModel);
        sendAnnouncement(messageDetailDTO, noticeModel, context, subjectText);
    }

}
