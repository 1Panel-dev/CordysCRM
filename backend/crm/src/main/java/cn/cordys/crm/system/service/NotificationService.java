package cn.cordys.crm.system.service;

import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.dto.OptionCountDTO;
import cn.cordys.common.redis.MessagePublisher;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.request.NotificationRequest;
import cn.cordys.crm.system.dto.response.NotificationCategoryDTO;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.crm.system.notice.NoticeRedisSync;
import cn.cordys.crm.system.notice.dto.NoticeRedisMessage;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class NotificationService {

    private static final String USER_ANNOUNCE_PREFIX = "announce_user:"; // Redis 存储用户前缀
    private static final String ANNOUNCE_PREFIX = "announce_content:"; // Redis 存储信息前缀
    private static final String USER_PREFIX = "msg_user:"; // Redis 存储系统通知用户前缀
    private static final String MSG_PREFIX = "msg_content:"; // Redis 存储系统通知内容信息前缀
    private static final String USER_READ_PREFIX = "user_read:"; // Redis 存储用户读取前缀
    @Resource
    private ExtNotificationMapper extNotificationMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SendModuleService sendModuleService;
    @Resource
    private MessagePublisher messagePublisher;

    private static void buildSourceCount(List<NotificationDTO> notifications, List<OptionCountDTO> options) {
        // 直接按 resource_type 分类统计：系统模块为模块标识(如 CUSTOMER/CLUE/FOLLOW_UP)，
        // 自定义表单为表单ID，公告为 ANNOUNCEMENT_NOTICE
        Map<String, Long> countMap = notifications.stream()
                .filter(notification -> StringUtils.isNotBlank(notification.getResourceType()))
                .collect(Collectors.groupingBy(Notification::getResourceType, Collectors.counting()));
        countMap.forEach((resourceType, count) -> {
            OptionCountDTO optionDTO = new OptionCountDTO();
            optionDTO.setKey(resourceType);
            optionDTO.setCount(count.intValue());
            options.add(optionDTO);
        });
    }

    private static void buildParam(NotificationRequest notificationRequest, String userId) {
        if (StringUtils.isNotBlank(notificationRequest.getSubject())) {
            notificationRequest.setSubject("%" + notificationRequest.getSubject() + "%");
        }
        if (StringUtils.isBlank(notificationRequest.getReceiver())) {
            notificationRequest.setReceiver(userId);
        }
    }

    public List<NotificationDTO> listNotification(NotificationRequest notificationRequest, String userId,
            String organizationId) {
        buildParam(notificationRequest, userId);
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest,
                organizationId);
        notifications.forEach(notification -> notification.setContentText(new String(notification.getContent())));
        return notifications;
    }

    public int read(String id, String userId, String orgId) {
        Notification record = new Notification();
        record.setId(id);
        record.setStatus(NotificationConstants.Status.READ.name());
        record.setReceiver(userId);
        record.setOrganizationId(orgId);
        //仅允许接收人本人更新自己所属组织的通知，非本人或非本组织更新行数为 0
        Integer update = extNotificationMapper.updateByIdAndReceiver(record);
        if (update == null || update == 0) {
            return 0;
        }
        // 在事务内查询更新后的未读数量，提交后再同步缓存和广播。
        Integer unRead = getUnRead(orgId, userId);
        NoticeRedisSync.afterCommit(() -> {
            stringRedisTemplate.opsForZSet().remove(USER_ANNOUNCE_PREFIX + userId, id);
            stringRedisTemplate.delete(ANNOUNCE_PREFIX + id);
            stringRedisTemplate.opsForZSet().remove(USER_PREFIX + userId, id);
            stringRedisTemplate.delete(MSG_PREFIX + id);
            if (unRead == 0) {
                stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + userId, "True");
                NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
                noticeRedisMessage.setMessage(userId);
                noticeRedisMessage.setNoticeType(NotificationConstants.Status.READ.toString());
                messagePublisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
            }
        });
        return update;
    }

    public int readAll(String organizationId, String userId) {
        Notification record = new Notification();
        record.setStatus(NotificationConstants.Status.READ.name());
        record.setOrganizationId(organizationId);
        record.setReceiver(userId);
        int update = extNotificationMapper.updateByReceiver(record);
        NoticeRedisSync.afterCommit(() -> {
            //所有这个用户的公告都不再显示
            Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(values)) {
                for (String value : values) {
                    stringRedisTemplate.delete(ANNOUNCE_PREFIX + value);
                }
            }
            Set<String> range = stringRedisTemplate.opsForZSet().range(USER_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(range)) {
                for (String value : range) {
                    stringRedisTemplate.delete(MSG_PREFIX + value);
                }
            }
            stringRedisTemplate.delete(USER_ANNOUNCE_PREFIX + userId);
            stringRedisTemplate.delete(USER_PREFIX + userId);
            stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + userId, "True");
            NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
            noticeRedisMessage.setMessage(userId);
            noticeRedisMessage.setNoticeType(NotificationConstants.Status.READ.toString());
            messagePublisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
        });
        return update;
    }

    public List<NotificationCategoryDTO> listCategories(NotificationRequest request, String userId,
            String organizationId) {
        request.setReceiver(userId);
        return extNotificationMapper.listCategories(request, organizationId);
    }

    public List<OptionCountDTO> countNotification(NotificationRequest notificationRequest, String organizationId,
            String userId) {
        List<OptionCountDTO> options = new ArrayList<>();
        buildParam(notificationRequest, userId);
        notificationRequest.setResourceType(StringUtils.EMPTY);
        notificationRequest.setStatus(NotificationConstants.Status.UNREAD.name());
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest,
                organizationId);
        OptionCountDTO totalOptionDTO = new OptionCountDTO();
        totalOptionDTO.setKey("total");
        totalOptionDTO.setCount(notifications.size());
        options.add(totalOptionDTO);
        buildSourceCount(notifications, options);
        return options;
    }

    public Integer getUnRead(String organizationId, String userId) {
        if (StringUtils.isBlank(organizationId)) {
            return 0;
        }
        Notification record = new Notification();
        record.setOrganizationId(organizationId);
        record.setStatus(NotificationConstants.Status.UNREAD.name());
        record.setReceiver(userId);
        return extNotificationMapper.countByReceiver(record);
    }

    public List<NotificationDTO> listLastNotification(String userId, String organizationId) {
        List<NotificationDTO> notifications = extNotificationMapper.selectLastAnnouncementList(userId, organizationId,
                false);
        notifications.forEach(notification -> notification.setContentText(new String(notification.getContent())));
        return notifications;
    }

    public List<NotificationDTO> listLastAnnouncement(String userId, String organizationId) {
        List<NotificationDTO> notifications = extNotificationMapper.selectLastAnnouncementList(userId, organizationId,
                true);
        notifications.forEach(notification -> notification.setContentText(new String(notification.getContent())));
        return notifications;
    }
}
