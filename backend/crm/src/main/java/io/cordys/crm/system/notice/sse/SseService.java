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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    @Resource
    private ExtNotificationMapper extNotificationMapper;

    @Resource
    private SendModuleService sendModuleService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";
    private static final String ANNOUNCE_PREFIX = "announce_content:";
    private static final String USER_PREFIX = "msg_user:";
    private static final String MSG_PREFIX = "msg_content:";
    private static final String USER_READ_PREFIX = "user_read:";

    private final Map<String, Map<String, ClientSinkWrapper>> userClients = new ConcurrentHashMap<>();

    /**
     * 客户端包装，包含 Sink 与 Flux
     */
    private static class ClientSinkWrapper {
        private final Sinks.Many<String> sink;
        private final Flux<String> flux;

        ClientSinkWrapper() {
            this.sink = Sinks.many().multicast().onBackpressureBuffer();
            this.flux = sink.asFlux()
                    // 心跳自动推送每 15 秒一次
                    .mergeWith(Flux.interval(Duration.ofSeconds(15))
                            .map(tick -> "HEARTBEAT: " + System.currentTimeMillis()))
                    .doOnCancel(this::complete);
        }

        void emit(String message) {
            sink.tryEmitNext(message);
        }

        void complete() {
            sink.tryEmitComplete();
        }
    }

    /**
     * 添加或获取现有客户端流
     */
    public Flux<String> addClient(String userId, String clientId) {
        if (StringUtils.isAnyBlank(userId, clientId)) {
            LogUtils.info("User ID or Client ID is blank, cannot add client.");
            return null;
        }
        Map<String, ClientSinkWrapper> inner = userClients.computeIfAbsent(userId,
                k -> Collections.synchronizedMap(new LinkedHashMap<>()));

        synchronized (inner) {
            if (inner.containsKey(clientId)) {
                return inner.get(clientId).flux;
            }
            ClientSinkWrapper wrapper = new ClientSinkWrapper();
            // 控制最多 3 个客户端
            inner.put(clientId, wrapper);
            if (inner.size() > 3) {
                Iterator<String> it = inner.keySet().iterator();
                String oldest = it.next();
                ClientSinkWrapper old = inner.remove(oldest);
                old.complete();
            }
            // 首次心跳
            wrapper.emit("HEARTBEAT: " + System.currentTimeMillis());
            return wrapper.flux;
        }
    }

    /**
     * 移除客户端
     */
    public void removeClient(String userId, String clientId) {
        if (StringUtils.isAnyBlank(userId, clientId)) return;
        Map<String, ClientSinkWrapper> map = userClients.get(userId);
        if (map == null) return;
        synchronized (map) {
            ClientSinkWrapper w = map.remove(clientId);
            if (w != null) w.complete();
            if (map.isEmpty()) userClients.remove(userId);
        }
    }

    /**
     * 向指定用户所有客户端发送事件
     */
    public void sendToUser(String userId, Object data) {
        Map<String, ClientSinkWrapper> map = userClients.get(userId);
        if (map != null) {
            map.forEach((clientId, wrapper) -> wrapper.emit(JSON.toJSONString(data)));
        }
    }

    /**
     * 向单个客户端发送事件
     */
    public void sendToClient(String userId, String clientId, Object data) {
        Optional.ofNullable(userClients.get(userId))
                .map(m -> m.get(clientId)).ifPresent(wrapper -> wrapper.emit(JSON.toJSONString(data)));
    }

    /**
     * 定时广播逻辑调用
     */
    public void broadcastPeriodically(String userId, String sendType) {
        SseMessageDTO msg = buildMessage(userId, sendType);
        sendToUser(userId, msg);
        LogUtils.info("Broadcast to user {} at {}", userId, System.currentTimeMillis());
    }

    /**
     * 构建通知消息体
     */
    private SseMessageDTO buildMessage(String userId, String sendType) {
        SseMessageDTO dto = new SseMessageDTO();
        if (StringUtils.equalsIgnoreCase(sendType, NotificationConstants.Type.SYSTEM_NOTICE.toString())) {
            List<String> modules = sendModuleService.getNoticeModules();
            Set<String> sysValues = stringRedisTemplate.opsForZSet().range(USER_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(sysValues)) {
                dto.setNotificationDTOList(buildDTOList(sysValues, MSG_PREFIX));
            } else {
                if (CollectionUtils.isNotEmpty(modules)) {
                    List<NotificationDTO> list = extNotificationMapper
                            .selectLastList(userId, OrganizationContext.getOrganizationId(), modules);
                    list.forEach(n -> n.setContentText(new String(n.getContent())));
                    dto.setNotificationDTOList(list);
                }
            }
        }
        if (StringUtils.equalsIgnoreCase(sendType, NotificationConstants.Type.ANNOUNCEMENT_NOTICE.toString())) {
            Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(values)) {
                dto.setAnnouncementDTOList(buildDTOList(values, ANNOUNCE_PREFIX));
            }
        }
        dto.setRead(Boolean.parseBoolean(
                stringRedisTemplate.opsForValue().get(USER_READ_PREFIX + userId)
        ));
        return dto;
    }

    /**
     * 根据 Redis 构建 DTO 列表
     */
    private List<NotificationDTO> buildDTOList(Set<String> values, String prefix) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(val -> stringRedisTemplate.opsForValue().get(prefix + val))
                .filter(StringUtils::isNotBlank)
                .map(json -> {
                    Notification notification = JSON.parseObject(json, Notification.class);
                    NotificationDTO dto = new NotificationDTO();
                    BeanUtils.copyBean(dto, notification);
                    dto.setContentText(new String(notification.getContent()));
                    return dto;
                })
                .sorted(Comparator.comparing(NotificationDTO::getCreateTime).reversed())
                .toList();
    }
}