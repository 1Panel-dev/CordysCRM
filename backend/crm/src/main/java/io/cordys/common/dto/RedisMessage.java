package io.cordys.common.dto;

import lombok.Data;

@Data
public class RedisMessage {
    /**
     * redis 发布订阅消息主体
     */
    private String message;
}
