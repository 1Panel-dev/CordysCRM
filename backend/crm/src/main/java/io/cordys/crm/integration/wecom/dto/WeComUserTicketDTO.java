package io.cordys.crm.integration.wecom.dto;

import lombok.Data;

@Data
public class WeComUserTicketDTO {

    /**
     * 成员票据，最大为512字节，有效期为1800s。scope为snsapi_privateinfo，且用户在应用可见范围之内时返回此参数。后续利用该参数可以获取用户信息或敏感信息
     */
    private String user_ticket;
}
