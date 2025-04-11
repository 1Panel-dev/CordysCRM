package io.cordys.crm.system.notice.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
@Tag(name = "消息通知-SSE")
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    /**
     * 客户端订阅 SSE 事件流
     */
    /*
        @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @Operation(summary = "客户端订阅 SSE 事件流")
        public SseEmitter subscribe(@RequestParam String clientId) {
            return sseService.addEmitter(SessionUtils.getUserId(), clientId);
        }
    */

    /**
     * 客户端订阅 SSE 事件流
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "客户端订阅 SSE 事件流")
    @CrossOrigin
    public SseEmitter subscribe(@RequestParam String userId, @RequestParam String clientId) {
        return sseService.addEmitter(userId, clientId);
    }


    /**
     * 模拟向所有客户端广播事件-(测试使用)
     */
    @GetMapping("/broadcast")
    @Operation(summary = "模拟向所有客户端广播事件-(测试使用)")
    public String broadcast(@RequestParam String userId, @RequestParam String clientId, @RequestParam String message) {
        sseService.sendToClient(userId, clientId, "SYSTEM_HEARTBEAT", "SYSTEM_HEARTBEAT: " + message);
        return "Broadcast: " + message;
    }

    /**
     * 主动断开客户端连接
     */
    @GetMapping("/close")
    @Operation(summary = "主动断开客户端连接")
    public void close(@RequestParam String userId, @RequestParam String clientId) {
        sseService.removeEmitter(userId, clientId);
    }
}