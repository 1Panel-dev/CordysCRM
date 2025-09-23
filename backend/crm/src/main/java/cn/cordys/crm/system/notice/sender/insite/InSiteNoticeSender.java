package cn.cordys.crm.system.notice.sender.insite;


import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.redis.MessagePublisher;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.notice.dto.NoticeRedisMessage;
import cn.cordys.crm.system.notice.sender.AbstractNoticeSender;
import cn.cordys.crm.system.notice.sse.SseService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InSiteNoticeSender extends AbstractNoticeSender {

    private static final String USER_PREFIX = "msg_user:";  // Redis 存储系统通知用户前缀
    private static final String MSG_PREFIX = "msg_content:";  // Redis 存储系统通知内容信息前缀
    private static final String USER_READ_PREFIX = "user_read:";  // Redis 存储用户读取前缀
    @Resource
    private BaseMapper<Notification> notificationBaseMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SseService sseService;
    @Resource
    private MessagePublisher messagePublisher;

    public void sendAnnouncement(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel, String context, String subjectText) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
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
            String messageText = JSON.toJSONString(notification);
            //储存信息
            stringRedisTemplate.opsForZSet().add(USER_PREFIX + receiver.getUserId(), id, System.currentTimeMillis());
            stringRedisTemplate.opsForValue().set(MSG_PREFIX + id, messageText);
            // 限制 Redis 只存 5 条消息
            Set<String> oldNotificationIds = stringRedisTemplate.opsForZSet()
                    .reverseRange(USER_PREFIX + receiver.getUserId(), 4, -1);
            if (CollectionUtils.isNotEmpty(oldNotificationIds)) {
                for (String oldNotificationId : oldNotificationIds) {
                    stringRedisTemplate.delete(MSG_PREFIX + oldNotificationId);
                }
            }
            stringRedisTemplate.opsForZSet().removeRange(USER_PREFIX + receiver.getUserId(), 0, -6);
            //更新用户的已读全部消息状态 0 为未读，1为已读
            stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + receiver.getUserId(), "False");

            // 发送消息
            NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
            noticeRedisMessage.setMessage(receiver.getUserId());
            noticeRedisMessage.setNoticeType(NotificationConstants.Type.SYSTEM_NOTICE.toString());
            messagePublisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
        });

    }

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        String context = super.getContext(messageDetailDTO, noticeModel);
        String subjectText = super.getSubjectText(messageDetailDTO, noticeModel);
        sendAnnouncement(messageDetailDTO, noticeModel, context, subjectText);
    }

}
