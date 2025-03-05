package io.cordys.crm.system.notice.sse;

import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.notice.dto.SseMessageDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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

    private static final String USER_PREFIX = "msg_user:";  // Redis 存储用户前缀
    private static final String MSG_PREFIX = "msg_content:";  // Redis 存储信息前缀
    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";  // Redis 存储用户前缀
    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀
    private static final String USER_READ_PREFIX = "user_read:";  // Redis 存储用户读取前缀


    // 线程安全的 Map，按 userId 存储该用户的所有客户端
    private final Map<String, Map<String, SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * 添加一个新的客户端 SseEmitter
     */
    public SseEmitter addEmitter(String userId, String clientId) {
        if (StringUtils.isAnyBlank(userId, clientId)) {
            throw new IllegalArgumentException("userId 和 clientId 不能为空");
        }

        var emitter = new SseEmitter(Long.MAX_VALUE);
        userEmitters
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(clientId, emitter);

        // 监听客户端断开连接
        emitter.onCompletion(() -> removeEmitter(userId, clientId));
        emitter.onTimeout(() -> removeEmitter(userId, clientId));
        emitter.onError(e -> removeEmitter(userId, clientId));

        return emitter;
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
            emitters.remove(clientId);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    /**
     * 每隔 5 秒向所有用户推送一次数据，数据内容根据用户决定
     */
    public void broadcastPeriodically() {
        if (userEmitters.isEmpty()) return;
        //返回最新的5条信息，加是否有未读信息, 公告
        userEmitters.forEach((userId, emitters) -> {
            //获取系统通知
            SseMessageDTO sseMessageDTO = new SseMessageDTO();
            Set<String> sysValues = stringRedisTemplate.opsForZSet().range(USER_PREFIX+userId, 0, -1);
            if (CollectionUtils.isNotEmpty(sysValues)) {
                List<NotificationDTO> notificationDTOList = buildDTOList(sysValues, MSG_PREFIX);
                sseMessageDTO.setNotificationDTOList(notificationDTOList);
            }
            //获取公告
            Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX+userId, 0, -1);
            if (CollectionUtils.isNotEmpty(values)) {
                List<NotificationDTO> announcementDTOList = buildDTOList(values, ANNOUNCE_PREFIX);
                sseMessageDTO.setAnnouncementDTOList(announcementDTOList);
            }
            //获取用户读取状态
            String read = stringRedisTemplate.opsForValue().get(USER_READ_PREFIX + userId);
            if (StringUtils.isBlank(read)) {
                sseMessageDTO.setRead(false);
            } else {
                sseMessageDTO.setRead(Boolean.getBoolean(read));
            }
            var message = "User " + userId + " time: " + System.currentTimeMillis();
            sendToUser(userId, "message", JSON.toJSONString(sseMessageDTO));
            LogUtils.info("Broadcast to user {}: {}", userId, message);
        });
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
            BeanUtils.copyBean(notificationDTO,notification);
            notificationDTO.setContentText(new String(notification.getContent()));
            notificationDTOList.add(notificationDTO);
        }
        return notificationDTOList;
    }
}
