package cn.cordys.crm.system.notice.sse;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.crm.system.notice.dto.SseMessageDTO;
import cn.cordys.crm.system.service.SendModuleService;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";
    private static final String ANNOUNCE_PREFIX = "announce_content:";
    private static final String USER_PREFIX = "msg_user:";
    private static final String MSG_PREFIX = "msg_content:";
    private static final String USER_READ_PREFIX = "user_read:";
    private final Map<String, Map<ClientKey, ClientSinkWrapper>> userClients = new ConcurrentHashMap<>();
    @Resource
    private ExtNotificationMapper extNotificationMapper;
    @Resource
    private SendModuleService sendModuleService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private record ClientKey(String organizationId, String sessionId, String clientId) {
    }

    private String currentUserId(String requestedUserId) {
        String userId = SessionUtils.getUserId();
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(SessionUtils.getSessionId())) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED);
        }
        if (requestedUserId != null && !userId.equals(requestedUserId)) {
            log.warn("Rejected SSE operation for a different user");
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        return userId;
    }

    private ClientKey currentClient(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED, "Client ID is required");
        }
        String organizationId = OrganizationContext.getOrganizationId();
        if (StringUtils.isBlank(organizationId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        return new ClientKey(organizationId, SessionUtils.getSessionId(), clientId);
    }

    /**
     * 请求中的用户 ID 仅用于兼容旧客户端，注册身份始终来自登录会话。
     */
    public Flux<String> addClient(String requestedUserId, String clientId) {
        String userId = currentUserId(requestedUserId);
        ClientKey key = currentClient(clientId);
        ClientSinkWrapper[] result = new ClientSinkWrapper[1];
        userClients.compute(userId, (id, existing) -> {
            Map<ClientKey, ClientSinkWrapper> clients = existing == null ? new ConcurrentHashMap<>() : existing;
            // 每个注册只允许一个 HTTP 订阅，复用 Flux 会绕过连接数量限制。
            if (clients.containsKey(key)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN, "SSE client already connected");
            }
            // 拒绝超限连接，不能通过新增连接淘汰其他登录会话。
            if (clients.size() >= 2) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN, "SSE connection limit reached");
            }
            ClientSinkWrapper wrapper = new ClientSinkWrapper();
            wrapper.flux = wrapper.flux.doFinally(signal -> removeClient(userId, key, wrapper));
            clients.put(key, wrapper);
            wrapper.emit("HEARTBEAT: " + System.currentTimeMillis());
            log.debug("SSE client registered");
            result[0] = wrapper;
            return clients;
        });
        return result[0].flux;
    }

    /**
     * 相同 clientId 在其他登录会话中不具备关闭权限。
     */
    public void removeClient(String requestedUserId, String clientId) {
        String userId = currentUserId(requestedUserId);
        removeClient(userId, currentClient(clientId), null);
    }

    private void removeClient(String userId, ClientKey key, ClientSinkWrapper expected) {
        ClientSinkWrapper[] removed = new ClientSinkWrapper[1];
        userClients.computeIfPresent(userId, (id, clients) -> {
            ClientSinkWrapper wrapper = clients.get(key);
            // 旧流的清理回调不能删除重连后新注册的流。
            if (wrapper != null && (expected == null || expected == wrapper)) {
                removed[0] = clients.remove(key);
            }
            return clients.isEmpty() ? null : clients;
        });
        if (removed[0] != null) {
            removed[0].complete();
            log.debug("SSE client closed");
        }
    }

    /**
     * 向指定用户所有客户端发送事件（供内部通知分发调用）。
     */
    public void sendToUser(String userId, String organizationId, Object data) {
        Map<ClientKey, ClientSinkWrapper> clients = userClients.get(userId);
        if (clients != null) {
            clients.forEach((key, wrapper) -> {
                if (key.organizationId().equals(organizationId)) {
                    wrapper.emit(JSON.toJSONString(data));
                }
            });
        }
    }

    /**
     * 向当前登录会话的单个客户端发送事件。
     */
    public void sendToClient(String requestedUserId, String clientId, Object data) {
        String userId = currentUserId(requestedUserId);
        ClientKey key = currentClient(clientId);
        Optional.ofNullable(userClients.get(userId))
                .map(clients -> clients.get(key)).ifPresent(wrapper -> wrapper.emit(JSON.toJSONString(data)));
    }

    /**
     * 定时广播逻辑调用
     */
    public void broadcastPeriodically(String userId, String sendType) {
        Map<ClientKey, ClientSinkWrapper> clients = userClients.get(userId);
        if (clients == null) return;
        clients.keySet().stream().map(ClientKey::organizationId).distinct().toList().forEach(organizationId -> {
            SseMessageDTO msg = buildMessage(userId, organizationId, sendType);
            sendToUser(userId, organizationId, msg);
        });
    }

    /**
     * 构建通知消息体
     */
    private SseMessageDTO buildMessage(String userId, String organizationId, String sendType) {
        SseMessageDTO dto = new SseMessageDTO();
        if (Strings.CI.equals(sendType, NotificationConstants.Type.SYSTEM_NOTICE.toString())) {
            List<String> modules = sendModuleService.getNoticeModules(organizationId);
            Set<String> sysValues = stringRedisTemplate.opsForZSet().range(USER_PREFIX + userId, 0, -1);
            List<NotificationDTO> list = buildDTOList(sysValues, MSG_PREFIX, userId, organizationId);
            // 用户缓存可能只包含其他组织的数据，过滤后为空也需要回查当前组织。
            if (CollectionUtils.isEmpty(list) && CollectionUtils.isNotEmpty(modules)) {
                list = extNotificationMapper.selectLastList(userId, organizationId, modules);
                list.forEach(n -> n.setContentText(new String(n.getContent())));
            }
            dto.setNotificationDTOList(list);
        }
        if (Strings.CI.equals(sendType, NotificationConstants.Type.ANNOUNCEMENT_NOTICE.toString())) {
            Set<String> values = stringRedisTemplate.opsForZSet().range(USER_ANNOUNCE_PREFIX + userId, 0, -1);
            if (CollectionUtils.isNotEmpty(values)) {
                dto.setAnnouncementDTOList(buildDTOList(values, ANNOUNCE_PREFIX, userId, organizationId));
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
    private List<NotificationDTO> buildDTOList(Set<String> values, String prefix, String userId, String organizationId) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(val -> stringRedisTemplate.opsForValue().get(prefix + val))
                .filter(StringUtils::isNotBlank)
                .map(json -> {
                    try {
                        Notification notification = JSON.parseObject(json, Notification.class);
                        if (!userId.equals(notification.getReceiver())
                                || !organizationId.equals(notification.getOrganizationId())) {
                            return null;
                        }
                        NotificationDTO dto = new NotificationDTO();
                        BeanUtils.copyBean(dto, notification);
                        dto.setContentText(new String(notification.getContent()));
                        return dto;
                    } catch (Exception e) {
                        log.warn("Failed to parse notification from Redis: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(NotificationDTO::getCreateTime).reversed())
                .toList();
    }

    /**
     * 客户端包装，包含 Sink 与 Flux
     */
    private static class ClientSinkWrapper {
        private final Sinks.Many<String> sink;
        private final Sinks.One<Void> closed = Sinks.one();
        private Flux<String> flux;

        ClientSinkWrapper() {
            this.sink = Sinks.many().multicast().onBackpressureBuffer();
            this.flux = sink.asFlux()
                    // 心跳自动推送每 15 秒一次
                    .mergeWith(Flux.interval(Duration.ofSeconds(15))
                            .map(tick -> "HEARTBEAT: " + System.currentTimeMillis()))
                    // 关闭时终止合并后的整个流，同时取消心跳定时任务。
                    .takeUntilOther(closed.asMono());
        }

        void emit(String message) {
            sink.tryEmitNext(message);
        }

        void complete() {
            closed.tryEmitEmpty();
            sink.tryEmitComplete();
        }
    }
}
