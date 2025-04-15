package io.cordys.crm.system.notice.sse;

import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.mapper.ExtNotificationMapper;
import io.cordys.crm.system.notice.dto.SseMessageDTO;
import io.cordys.crm.system.service.SendModuleService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ExtNotificationMapper extNotificationMapper;

    @Resource
    private SendModuleService sendModuleService;

    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";  // Redis 存储用户前缀
    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀
    private static final String USER_PREFIX = "msg_user:";  // Redis 存储系统通知用户前缀
    private static final String MSG_PREFIX = "msg_content:";  // Redis 存储系统通知内容信息前缀
    private static final String USER_READ_PREFIX = "user_read:";  // Redis 存储用户读取前缀


    // 线程安全的 Map，按 userId 存储该用户的所有客户端
    private final Map<String, Map<String, SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * 添加一个新的客户端 SseEmitter
     * 每个用户最多允许三个客户端，超出时移除最早添加的客户端
     *
     * @param userId   用户 ID
     * @param clientId 客户端 ID
     * @return 新创建的 SseEmitter
     * @throws IllegalArgumentException 如果 userId 或 clientId 为空
     */
    public SseEmitter addEmitter(String userId, String clientId) {
        if (userId == null || userId.isBlank() || clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("userId 和 clientId 不能为空");
        }

        // 获取或初始化用户的内部映射，使用同步的 LinkedHashMap 保持插入顺序
        Map<String, SseEmitter> innerMap = userEmitters.computeIfAbsent(userId,
                k -> Collections.synchronizedMap(new ConcurrentHashMap<>()));

        synchronized (innerMap) {
            // 如果对于指定的 clientId 已经存在 emitter，则直接返回，不需要再新创建
            if (innerMap.containsKey(clientId)) {
                return innerMap.get(clientId);
            }

            // 否则，创建新的 SseEmitter，设置超时为最大值
            SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
            innerMap.put(clientId, emitter);

            // 限制同一用户的客户端数量，当超过 3 个时，移除最早的 emitter
            if (innerMap.size() > 3) {
                Iterator<Map.Entry<String, SseEmitter>> iterator = innerMap.entrySet().iterator();
                if (iterator.hasNext()) {
                    Map.Entry<String, SseEmitter> oldestEntry = iterator.next();
                    try {
                        oldestEntry.getValue().complete();
                    } catch (Exception e) {
                        oldestEntry.getValue().completeWithError(e);
                    }
                    iterator.remove();
                }
            }

            // 为 emitter 设置生命周期监听器，当完成、超时或错误时会自动移除对应的 emitter
            Runnable removeAction = () -> removeEmitter(userId, clientId);
            emitter.onCompletion(removeAction);
            emitter.onTimeout(removeAction);
            emitter.onError(e -> removeAction.run());
            // 立即推送一条消息
            sendHeartbeat(emitter, clientId, userId);
            return emitter;
        }
    }

    /**
     * 给所有用户发送心跳信息
     */
    public void sendHeartbeat() {
        if (userEmitters.isEmpty()) return;

        userEmitters.forEach((userId, emitters) ->
                emitters.forEach((clientId, emitter) -> sendHeartbeat(emitter, clientId, userId)));
    }

    private void sendHeartbeat(SseEmitter emitter, String clientId, String userId) {
        try {
            emitter.send(SseEmitter.event()
                    .id(clientId)
                    .reconnectTime(10000)
                    .name("SYSTEM_HEARTBEAT")
                    .data("SYSTEM_HEARTBEAT: " + System.currentTimeMillis()));
        } catch (IOException e) {
            removeEmitter(userId, clientId);
        }
    }

    /**
     * 按用户 ID 发送事件（所有客户端接收）
     */
    public void sendToUser(String userId, String eventName, Object data) {
        Optional.ofNullable(userEmitters.get(userId))
                .ifPresent(emitters -> emitters.forEach((clientId, emitter) -> sendToEmitter(userId, clientId, emitter, eventName, data)));
    }

    /**
     * 按具体客户端发送事件
     */
    public void sendToClient(String userId, String clientId, String eventName, Object data) {
        Optional.ofNullable(userEmitters.get(userId))
                .map(emitters -> emitters.get(clientId))
                .ifPresent(emitter -> sendToEmitter(userId, clientId, emitter, eventName, data));
    }

    /**
     * 发送数据并处理异常
     */
    private void sendToEmitter(String userId, String clientId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .id(clientId)
                    .reconnectTime(10000)
                    .data(data));
        } catch (IOException e) {
            removeEmitter(userId, clientId);
        }
    }

    /**
     * 移除指定客户端的 SseEmitter
     */
    public void removeEmitter(String userId, String clientId) {
        if (StringUtils.isAnyBlank(userId, clientId)) return;

        userEmitters.computeIfPresent(userId, (k, emitters) -> {
            Optional.ofNullable(emitters.remove(clientId))
                    .ifPresent(emitter -> {
                        try {
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });
            return emitters.isEmpty() ? null : emitters;
        });
    }


    /**
     * 广播消息给指定的用户
     * @param userId 用户 ID
     * @param sendType 发送类型
     */
    public void broadcastPeriodically(String userId, String sendType) {
        if (userEmitters.isEmpty()) return;

        SseMessageDTO sseMessageDTO = new SseMessageDTO();
        //获取系统通知
        if (StringUtils.equalsIgnoreCase(sendType, NotificationConstants.Type.SYSTEM_NOTICE.toString())) {
            //获取已开启的模块
            List<String> modules = sendModuleService.getNoticeModules();
            Set<String> sysValues = stringRedisTemplate.opsForZSet().range(USER_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(sysValues)) {
                int size = sysValues.size();
                sseMessageDTO.setNotificationDTOList(buildDTOList(sysValues, MSG_PREFIX));
                if (size != 5) {
                    //如果不够5条，从数据库提取最新未读加上
                    // 获取用户未读的系统通知
                    List<NotificationDTO> notifications = extNotificationMapper
                            .selectLastList(userId, OrganizationContext.getOrganizationId(), modules);
                    notifications.stream().sorted(Comparator.comparing(NotificationDTO::getCreateTime).reversed()).forEach(notification ->
                            notification.setContentText(new String(notification.getContent())));
                    List<NotificationDTO> notificationDTOS = notifications.subList(0, Math.min(5 - size, notifications.size()));
                    sseMessageDTO.setNotificationDTOList(notificationDTOS);
                }
            } else {
                List<NotificationDTO> notifications = extNotificationMapper
                        .selectLastList(userId, OrganizationContext.getOrganizationId(), modules);
                notifications.stream().sorted(Comparator.comparing(NotificationDTO::getCreateTime).reversed()).forEach(notification ->
                        notification.setContentText(new String(notification.getContent())));
                sseMessageDTO.setNotificationDTOList(notifications);
            }

        }


        // 获取公告（如果存在）
        if (StringUtils.equalsIgnoreCase(sendType, NotificationConstants.Type.ANNOUNCEMENT_NOTICE.toString())) {
            Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(values)) {
                sseMessageDTO.setAnnouncementDTOList(buildDTOList(values, ANNOUNCE_PREFIX));
            }
        }

        // 获取用户读取状态
        // Boolean.parseBoolean 直接将 null 或空字符串解析为 false
        sseMessageDTO.setRead(Boolean.parseBoolean(stringRedisTemplate
                .opsForValue().get(USER_READ_PREFIX + userId)));

        // 发送消息并记录日志
        String message = "User " + userId + " time: " + System.currentTimeMillis();
        sendToUser(userId, "message", JSON.toJSONString(sseMessageDTO));
        LogUtils.info("Broadcast to user {}: {}", userId, message);

    }



    private List<NotificationDTO> buildDTOList(Set<String> sysValues, String prefix) {
        List<NotificationDTO> notificationDTOList = new ArrayList<>();
        for (String value : sysValues) {
            String announceNotification = stringRedisTemplate.opsForValue().get(prefix + value);
            if (StringUtils.isBlank(announceNotification)) {
                continue;
            }
            Notification notification = JSON.parseObject(announceNotification, Notification.class);
            NotificationDTO notificationDTO = new NotificationDTO();
            BeanUtils.copyBean(notificationDTO, notification);
            notificationDTO.setContentText(new String(notification.getContent()));
            notificationDTOList.add(notificationDTO);
        }

        return notificationDTOList.stream().sorted(Comparator.comparing(NotificationDTO::getCreateTime).reversed()).toList();
    }
}
