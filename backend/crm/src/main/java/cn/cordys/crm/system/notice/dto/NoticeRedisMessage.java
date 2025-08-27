package cn.cordys.crm.system.notice.dto;

import cn.cordys.common.dto.RedisMessage;
import lombok.Data;

@Data
public class NoticeRedisMessage extends RedisMessage {
    /**
     * redis 发布订阅消息补充
     */
    private String noticeType;

}
