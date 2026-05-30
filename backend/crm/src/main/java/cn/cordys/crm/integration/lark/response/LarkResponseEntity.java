package cn.cordys.crm.integration.lark.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LarkResponseEntity {

    /**
     * 错误码
     */
    @JsonProperty("code")
    private int code;

    /**
     * 错误消息
     */
    @JsonProperty("msg")
    private String msg;

}
