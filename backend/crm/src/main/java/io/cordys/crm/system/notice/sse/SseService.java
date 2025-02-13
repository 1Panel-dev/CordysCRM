package io.cordys.crm.system.notice.sse;

import io.cordys.common.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    // 使用线程安全的 Map 存储所有客户端的 SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 添加一个新的客户端 SseEmitter
     */
    public SseEmitter addEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 设置超时时间为无限
        emitters.put(clientId, emitter);

        // 监听客户端断开连接
        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> emitters.remove(clientId));
        emitter.onError((e) -> emitters.remove(clientId));

        return emitter;
    }

    /**
     * 向所有客户端广播事件
     */
    public void broadcastEvent(String eventName, Object data) {
        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName) // 事件名称
                        .data(data)); // 事件数据
            } catch (IOException e) {
                // 发送失败时移除该客户端
                emitters.remove(clientId);
            }
        });
    }


    /**
     * 移除指定客户端的 SseEmitter
     */
    public void removeEmitter(String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
            emitters.remove(clientId);
        }
    }


    /**
     * 每隔 5 秒向所有客户端广播一次事件
     */
    @Scheduled(fixedRate = 5000) // 单位：毫秒
    public void broadcastPeriodically() {
        String message = "Server time: " + System.currentTimeMillis();

        this.broadcastEvent("message", message);

        LogUtils.info("Broadcast: " + message);
    }

}