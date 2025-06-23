package io.cordys.crm.system.dto;

import io.cordys.common.dto.RedisMessage;
import lombok.Data;

@Data
public class ExportRedisMessage extends RedisMessage {
    /**
     * redis 发布订阅消息补充
     */
    private String exportType;
}
