package io.cordys.crm.system.service;


import io.cordys.common.dto.OptionDTO;

import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.request.NotificationRequest;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.mapper.ExtNotificationMapper;
import io.cordys.mybatis.BaseMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class NotificationService {

    @Resource
    private BaseMapper<Notification> notificationMapper;
    @Resource
    private ExtNotificationMapper extNotificationMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";  // Redis 存储用户前缀
    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀
    private static final String USER_READ_PREFIX = "user_read:";  // Redis 存储用户读取前缀


    public List<NotificationDTO> listNotification(NotificationRequest notificationRequest, String userId, String organizationId) {
        buildParam(notificationRequest, userId);
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest, organizationId);
        notifications.forEach(notification -> notification.setContentText(new String(notification.getContent())));
        return notifications;
    }

    public int read(String id, String userId, String orgId) {
        Notification record = new Notification();
        record.setId(id);
        record.setStatus(NotificationConstants.Status.READ.name());
        record.setReceiver(userId);
        //删除缓存中的公告提示
        stringRedisTemplate.opsForZSet().remove(USER_ANNOUNCE_PREFIX + userId, id);
        stringRedisTemplate.delete(ANNOUNCE_PREFIX + id);
        Integer update = notificationMapper.update(record);
        //检查当前用户是否还有有已读信息，没有更新用户状态
        Integer unRead = getUnRead(orgId, userId);
        if (unRead == 0) {
            stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + userId, "1");
        }
        return update;
    }

    public int readAll(String organizationId, String userId) {
        Notification record = new Notification();
        record.setStatus(NotificationConstants.Status.READ.name());
        record.setOrganizationId(organizationId);
        record.setReceiver(userId);
        //所有这个用户的公告都不再显示
        Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX + userId, 0, -1);
        if (CollectionUtils.isNotEmpty(values)) {
            for (String value : values) {
                stringRedisTemplate.delete(ANNOUNCE_PREFIX + value);
            }
        }
        stringRedisTemplate.delete(USER_ANNOUNCE_PREFIX + userId);
        stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + userId, "1");
        return extNotificationMapper.updateByReceiver(record);
    }

    public List<OptionDTO> countNotification(NotificationRequest notificationRequest, String organizationId, String userId) {
        List<OptionDTO> optionDTOS = new ArrayList<>();
        buildParam(notificationRequest, userId);
        notificationRequest.setResourceType(StringUtils.EMPTY);
        notificationRequest.setStatus(NotificationConstants.Status.UNREAD.name());
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest, organizationId);
        OptionDTO totalOptionDTO = new OptionDTO();
        totalOptionDTO.setId("total");
        totalOptionDTO.setName(String.valueOf(notifications.size()));
        optionDTOS.add(totalOptionDTO);
        buildSourceCount(notifications, optionDTOS);
        return optionDTOS;
    }

    private static void buildSourceCount(List<NotificationDTO> notifications, List<OptionDTO> optionDTOS) {
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<Notification>> resourceMap = notifications.stream().collect(Collectors.groupingBy(Notification::getResourceType));
        resourceMap.forEach((k, v) -> {
            if (k.contains(NotificationConstants.Module.CUSTOMER)) {
                countMap.merge(NotificationConstants.Module.CUSTOMER, v.size(), Integer::sum);
            } else if (k.contains(NotificationConstants.Module.CLUE)) {
                countMap.merge(NotificationConstants.Module.CLUE, v.size(), Integer::sum);
            } else if (k.contains(NotificationConstants.Module.BUSINESS)) {
                countMap.merge(NotificationConstants.Module.BUSINESS, v.size(), Integer::sum);
            } else {
                countMap.merge(NotificationConstants.Module.ANNOUNCEMENT, v.size(), Integer::sum);
            }
        });
        countMap.forEach((k, v) -> {
            OptionDTO optionDTO = new OptionDTO();
            optionDTO.setId(k);
            optionDTO.setName(String.valueOf(v));
            optionDTOS.add(optionDTO);
        });
    }


    private static void buildParam(NotificationRequest notificationRequest, String userId) {
        if (StringUtils.isNotBlank(notificationRequest.getSubject())) {
            notificationRequest.setSubject("%" + notificationRequest.getSubject() + "%");
        }
        if (StringUtils.isNotBlank(notificationRequest.getResourceType())) {
            notificationRequest.setResourceType("%" + notificationRequest.getResourceType() + "%");
        }
        if (StringUtils.isBlank(notificationRequest.getReceiver())) {
            notificationRequest.setReceiver(userId);
        }
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
}
