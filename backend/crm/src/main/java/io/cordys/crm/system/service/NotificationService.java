package io.cordys.crm.system.service;


import io.cordys.common.constants.ModuleKey;
import io.cordys.common.dto.OptionCountDTO;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.request.NotificationRequest;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.mapper.ExtNotificationMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Resource
    private BaseMapper<Module> moduleMapper;


    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";  // Redis 存储用户前缀
    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀
    private static final String USER_PREFIX = "msg_user:";  // Redis 存储系统通知用户前缀
    private static final String MSG_PREFIX = "msg_content:";  // Redis 存储系统通知内容信息前缀
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
        stringRedisTemplate.opsForZSet().remove(USER_PREFIX + userId, id);
        stringRedisTemplate.delete(MSG_PREFIX + id);
        Integer update = notificationMapper.update(record);
        //检查当前用户是否还有有已读信息，没有更新用户状态
        Integer unRead = getUnRead(orgId, userId);
        if (unRead == 0) {
            stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + userId, "True");
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
        Set<String> range = stringRedisTemplate.opsForZSet().range(USER_PREFIX + userId, 0, -1);
        if (CollectionUtils.isNotEmpty(range)){
            for (String value : range) {
                stringRedisTemplate.delete(MSG_PREFIX+value);
            }
        }
        stringRedisTemplate.delete(USER_ANNOUNCE_PREFIX + userId);
        stringRedisTemplate.delete(USER_PREFIX + userId);
        stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + userId, "True");
        return extNotificationMapper.updateByReceiver(record);
    }

    public List<OptionCountDTO> countNotification(NotificationRequest notificationRequest, String organizationId, String userId) {
        List<OptionCountDTO> optionDTOS = new ArrayList<>();
        buildParam(notificationRequest, userId);
        notificationRequest.setResourceType(StringUtils.EMPTY);
        notificationRequest.setStatus(NotificationConstants.Status.UNREAD.name());
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest, organizationId);
        OptionCountDTO totalOptionDTO = new OptionCountDTO();
        totalOptionDTO.setKey("total");
        totalOptionDTO.setCount(notifications.size());
        optionDTOS.add(totalOptionDTO);
        buildSourceCount(notifications, optionDTOS);
        return optionDTOS;
    }

    private static void buildSourceCount(List<NotificationDTO> notifications, List<OptionCountDTO> optionDTOS) {
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<Notification>> resourceMap = notifications.stream().collect(Collectors.groupingBy(Notification::getResourceType));
        resourceMap.forEach((k, v) -> {
            if (k.contains(NotificationConstants.Module.CUSTOMER)) {
                countMap.merge(NotificationConstants.Module.CUSTOMER, v.size(), Integer::sum);
            } else if (k.contains(NotificationConstants.Module.CLUE)) {
                countMap.merge(NotificationConstants.Module.CLUE, v.size(), Integer::sum);
            } else if (k.contains(NotificationConstants.Module.OPPORTUNITY)) {
                countMap.merge(NotificationConstants.Module.OPPORTUNITY, v.size(), Integer::sum);
            } else {
                countMap.merge(NotificationConstants.Type.ANNOUNCEMENT_NOTICE.name(), v.size(), Integer::sum);
            }
        });
        countMap.putIfAbsent(NotificationConstants.Module.CUSTOMER, 0);
        countMap.putIfAbsent(NotificationConstants.Module.CLUE, 0);
        countMap.putIfAbsent(NotificationConstants.Module.OPPORTUNITY, 0);
        countMap.putIfAbsent(NotificationConstants.Type.ANNOUNCEMENT_NOTICE.name(), 0);

        countMap.forEach((k, v) -> {
            OptionCountDTO optionDTO = new OptionCountDTO();
            optionDTO.setKey(k);
            optionDTO.setCount(v);
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

    public List<NotificationDTO> listLastNotification(String userId, String organizationId) {
        //获取已开启的模块
        List<String> enabledModules = moduleMapper.selectListByLambda(
                        new LambdaQueryWrapper<Module>()
                                .eq(Module::getOrganizationId, OrganizationContext.getOrganizationId())
                                .eq(Module::getEnable, true)
                ).stream()
                .map(Module::getModuleKey).distinct()
                .toList();

        List<String>modules = new ArrayList<>();
        for (String enabledModule : enabledModules) {
            if (StringUtils.equalsIgnoreCase(enabledModule, ModuleKey.BUSINESS.getKey())) {
                modules.add(NotificationConstants.Module.OPPORTUNITY);
            }
            if (StringUtils.equalsIgnoreCase(enabledModule,ModuleKey.CUSTOMER.getKey())) {
                modules.add(NotificationConstants.Module.CUSTOMER);
            }
            if (StringUtils.equalsIgnoreCase(enabledModule,ModuleKey.CLUE.getKey())) {
                modules.add(NotificationConstants.Module.CLUE);
            }

        }

        List<NotificationDTO> notifications = extNotificationMapper.selectLastList(userId, organizationId,modules);
        notifications.forEach(notification -> notification.setContentText(new String(notification.getContent())));
        return notifications;
    }
}
