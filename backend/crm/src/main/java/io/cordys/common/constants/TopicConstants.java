package io.cordys.common.constants;

public class TopicConstants {
    /**
     * 下载任务的 Redis 主题名称
     */
    public static final String DOWNLOAD_TOPIC = "download-topic";

    /**
     * sse 消息通知的Redis 主题名称
     */
    public static final String SSE_TOPIC = "sse-topic";

    private TopicConstants() {
        // 私有构造函数，防止实例化
    }
}
