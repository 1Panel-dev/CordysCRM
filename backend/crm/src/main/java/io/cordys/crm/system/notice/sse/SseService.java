package io.cordys.crm.system.notice.sse;

import io.cordys.common.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

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
        userEmitters.forEach((userId, emitters) -> {
            var message = "User " + userId + " time: " + System.currentTimeMillis();
            sendToUser(userId, "message", message);
            LogUtils.info("Broadcast to user {}: {}", userId, message);
        });
    }
}
