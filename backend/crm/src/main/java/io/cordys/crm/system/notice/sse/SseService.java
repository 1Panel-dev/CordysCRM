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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务端 SSE（Server-Sent Events）推送服务
 * <p>本服务基于 Spring MVC 的 SseEmitter 实现，提供用户级别的多客户端管理、心跳检测、消息广播等功能。</p>
 */
@Service
public class SseService {

    /**
     * 持久化通知数据访问接口
     */
    @Resource
    private ExtNotificationMapper extNotificationMapper;

    /**
     * 发送模块配置服务
     */
    @Resource
    private SendModuleService sendModuleService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis 存储用户公告前缀
     */
    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";

    /**
     * Redis 存储公告内容前缀
     */
    private static final String ANNOUNCE_PREFIX = "announce_content:";

    /**
     * Redis 存储系统通知用户前缀
     */
    private static final String USER_PREFIX = "msg_user:";

    /**
     * Redis 存储系统通知内容信息前缀
     */
    private static final String MSG_PREFIX = "msg_content:";

    /**
     * Redis 存储用户读取状态前缀
     */
    private static final String USER_READ_PREFIX = "user_read:";

    /**
     * 用户 -> 客户端ID -> SseEmitterWrapper 映射
     */
    private final Map<String, Map<String, SseEmitterWrapper>> userEmitters = new ConcurrentHashMap<>();

    /**
     * 包装 SseEmitter ，并追踪完成状态，避免重复发送异常
     */
    private static class SseEmitterWrapper {
        private final SseEmitter emitter;
        private final AtomicBoolean completed = new AtomicBoolean(false);

        SseEmitterWrapper(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 判断连接是否仍然打开
         */
        boolean isOpen() {
            return !completed.get();
        }

        /**
         * 完成并关闭连接，仅执行一次
         */
        void complete() {
            if (completed.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    completeWithError(e);
                }
            }
        }

        /**
         * 出错时完成并关闭连接，仅执行一次
         */
        void completeWithError(Throwable t) {
            if (completed.compareAndSet(false, true)) {
                try {
                    emitter.completeWithError(t);
                } catch (Exception ignore) {
                }
            }
        }

        /**
         * 发送事件，如果连接已关闭则跳过；发生异常时自动 complete()
         *
         * @param event SseEventBuilder 构建的事件
         */
        void sendEvent(SseEmitter.SseEventBuilder event) {
            if (!isOpen()) {
                return;
            }
            try {
                emitter.send(event);
            } catch (IllegalStateException | IOException ex) {
                complete();
            }
        }
    }

    /**
     * 添加一个新的客户端订阅
     *
     * @param userId   唯一用户 ID
     * @param clientId 唯一客户端 ID
     * @return 新创建的 SseEmitter 对象
     * @throws IllegalArgumentException 当 userId 或 clientId 为空时抛出
     */
    public SseEmitter addEmitter(String userId, String clientId) {
        if (StringUtils.isAnyBlank(userId, clientId)) {
            throw new IllegalArgumentException("userId 和 clientId 不能为空");
        }
        Map<String, SseEmitterWrapper> inner = userEmitters.computeIfAbsent(userId,
                k -> Collections.synchronizedMap(new LinkedHashMap<>()));

        synchronized (inner) {
            // 已存在则直接返回
            if (inner.containsKey(clientId)) {
                return inner.get(clientId).emitter;
            }
            // 创建新的 emitter
            SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
            // 注册生命周期回调
            Runnable remove = () -> removeEmitter(userId, clientId);
            emitter.onCompletion(remove);
            emitter.onTimeout(remove);
            emitter.onError(e -> remove.run());

            SseEmitterWrapper wrapper = new SseEmitterWrapper(emitter);
            inner.put(clientId, wrapper);

            // 限制同一用户最大 3 个客户端
            if (inner.size() > 3) {
                Iterator<String> it = inner.keySet().iterator();
                String oldest = it.next();
                SseEmitterWrapper old = inner.remove(oldest);
                old.complete();
            }

            // 首次心跳，客户端建立连接后立即推送
            sendHeartbeat(wrapper, clientId);
            return emitter;
        }
    }

    /**
     * 向所有活跃的客户端发送心跳
     */
    public void sendHeartbeat() {
        userEmitters.forEach((uid, map) -> map.forEach((cid, w) -> sendHeartbeat(w, cid)));
    }

    /**
     * 向单个客户端发送心跳
     *
     * @param wrapper  包装后的 emitter
     * @param clientId 客户端 ID
     */
    private void sendHeartbeat(SseEmitterWrapper wrapper, String clientId) {
        wrapper.sendEvent(SseEmitter.event()
                .id(clientId)
                .name("SYSTEM_HEARTBEAT")
                .reconnectTime(10000)
                .data("HEARTBEAT: " + System.currentTimeMillis()));
    }

    /**
     * 向指定用户的所有客户端发送事件
     *
     * @param userId    用户 ID
     * @param eventName 事件名称
     * @param data      事件数据
     */
    public void sendToUser(String userId, String eventName, Object data) {
        Map<String, SseEmitterWrapper> map = userEmitters.get(userId);
        if (map != null) {
            map.forEach((cid, w) -> sendToEmitter(cid, w, eventName, data));
        }
    }

    /**
     * 向指定用户的单个客户端发送事件
     *
     * @param clientId  客户端 ID
     * @param eventName 事件名称
     * @param data      事件数据
     */
    public void sendToClient(String userId, String clientId, String eventName, Object data) {
        Optional.ofNullable(userEmitters.get(userId))
                .map(m -> m.get(clientId)).ifPresent(w -> sendToEmitter(clientId, w, eventName, data));
    }

    /**
     * 发送事件到指定 emitter 并处理异常
     */
    private void sendToEmitter(String clientId, SseEmitterWrapper wrapper,
                               String eventName, Object data) {
        wrapper.sendEvent(SseEmitter.event()
                .id(clientId)
                .name(eventName)
                .reconnectTime(10000)
                .data(data));
    }

    /**
     * 移除并关闭指定客户端的连接
     *
     * @param userId   用户 ID
     * @param clientId 客户端 ID
     */
    public void removeEmitter(String userId, String clientId) {
        if (StringUtils.isAnyBlank(userId, clientId)) return;
        Map<String, SseEmitterWrapper> map = userEmitters.get(userId);
        if (map == null) return;
        synchronized (map) {
            SseEmitterWrapper w = map.remove(clientId);
            if (w != null) {
                w.complete();
            }
            if (map.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * 定时广播消息给指定用户
     * <p>本方法构建通知消息体后，调用 sendToUser 统一发送。</p>
     *
     * @param userId   用户 ID
     * @param sendType 发送类型：系统通知或公告
     */
    public void broadcastPeriodically(String userId, String sendType) {
        SseMessageDTO msg = buildMessage(userId, sendType);
        sendToUser(userId, "message", JSON.toJSONString(msg));

        LogUtils.info("Broadcast to user {} at {}", userId, System.currentTimeMillis());
    }

    /**
     * 构建通知消息体
     */
    private SseMessageDTO buildMessage(String userId, String sendType) {
        SseMessageDTO sseMessageDTO = new SseMessageDTO();
        // 构建系统通知列表
        if (StringUtils.equalsIgnoreCase(sendType, NotificationConstants.Type.SYSTEM_NOTICE.toString())) {
            List<String> modules = sendModuleService.getNoticeModules();
            Set<String> sysValues = stringRedisTemplate.opsForZSet().range(USER_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(sysValues)) {
                sseMessageDTO.setNotificationDTOList(
                        buildDTOList(sysValues, MSG_PREFIX)
                );
            } else {
                List<NotificationDTO> notifications = extNotificationMapper
                        .selectLastList(userId, OrganizationContext.getOrganizationId(), modules);
                notifications.forEach(n -> n.setContentText(new String(n.getContent())));
                sseMessageDTO.setNotificationDTOList(notifications);
            }
        }
        // 构建公告列表
        if (StringUtils.equalsIgnoreCase(sendType, NotificationConstants.Type.ANNOUNCEMENT_NOTICE.toString())) {
            Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(values)) {
                sseMessageDTO.setAnnouncementDTOList(
                        buildDTOList(values, ANNOUNCE_PREFIX)
                );
            }
        }
        // 用户已读状态
        sseMessageDTO.setRead(Boolean.parseBoolean(
                stringRedisTemplate.opsForValue().get(USER_READ_PREFIX + userId)
        ));
        return sseMessageDTO;
    }

    /**
     * 根据 Redis 中的值构建 DTO 列表并按创建时间倒序排序
     */
    private List<NotificationDTO> buildDTOList(Set<String> values, String prefix) {
        List<NotificationDTO> list = new ArrayList<>();
        for (String val : values) {
            String json = stringRedisTemplate.opsForValue().get(prefix + val);
            if (StringUtils.isBlank(json)) continue;
            Notification notification = JSON.parseObject(json, Notification.class);
            NotificationDTO dto = new NotificationDTO();
            BeanUtils.copyBean(dto, notification);
            dto.setContentText(new String(notification.getContent()));
            list.add(dto);
        }
        list.sort(Comparator.comparing(NotificationDTO::getCreateTime).reversed());
        return list;
    }
}
