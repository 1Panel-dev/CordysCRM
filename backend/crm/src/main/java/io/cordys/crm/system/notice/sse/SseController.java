package io.cordys.crm.system.notice.sse;

import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "客户端订阅 SSE 事件流")
    public SseEmitter subscribe(@RequestParam String clientId) {
        return sseService.addEmitter(SessionUtils.getUserId(), clientId);
    }

    /**
     * 客户端订阅 SSE 事件流
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "客户端订阅 SSE 事件流")
    public SseEmitter subscribe(@RequestParam String userId, @RequestParam String clientId) {
        return sseService.addEmitter(userId, clientId);
    }

    /**
     * 主动断开客户端连接
     */
    @GetMapping("/close")
    @Operation(summary = "主动断开客户端连接")
    public void close(@RequestParam String clientId) {
        sseService.removeEmitter(SessionUtils.getUserId(), clientId);
    }
}