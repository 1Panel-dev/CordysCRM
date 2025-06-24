package io.cordys.common.constants;

import java.util.List;

public class TopicConstants {
    /**
     * 下载任务的 Redis 主题名称
     */
    public static final String DOWNLOAD_TOPIC = "download-topic";

    /**
     * sse 消息通知的Redis 主题名称
     */
    public static final String SSE_TOPIC = "sse-topic";

    /**
     * 所有 Redis 主题的集合
     * 用于统一管理订阅和发布的主题
     */
    public static final List<String> ALL_TOPICS = List.of(DOWNLOAD_TOPIC, SSE_TOPIC);

    private TopicConstants() {
        // 私有构造函数，防止实例化
    }
}
