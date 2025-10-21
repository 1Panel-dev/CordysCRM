package cn.cordys.crm.integration.lark.dto;

import lombok.Data;

@Data
public class LarkSendMessageDTO {

    private String receive_id;
    private String msg_type;
    private LarkTextDTO content;

}
